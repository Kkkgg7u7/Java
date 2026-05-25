        let defaultBaseDir = '';
        let performanceHistory = [];
        let fileTypeChartInstance = null;
        let trendChartInstance = null;
        let throughputChartInstance = null;
        let timeChartInstance = null;
        let latestRunPath = '';

        // ==================== 工具函数 ====================

        function formatBytes(bytes) {
            const value = Number(bytes || 0);
            if (value < 1024) return value + ' B';
            if (value < 1024 * 1024) return (value / 1024).toFixed(2) + ' KB';
            if (value < 1024 * 1024 * 1024) return (value / 1024 / 1024).toFixed(2) + ' MB';
            return (value / 1024 / 1024 / 1024).toFixed(2) + ' GB';
        }

        function showStatus(connected) {
            const pill = document.getElementById('statusPill');
            pill.textContent = connected ? 'HDFS 已连接' : '连接异常';
            pill.className = 'status-pill ' + (connected ? 'ok' : 'error');
            const connectionState = document.getElementById('connectionState');
            if (connectionState) {
                connectionState.textContent = connected ? '已连接' : '未连接';
            }
        }

        function ensureLogin(data) {
            if (data && data.code === 401) {
                window.location.href = '/user/login';
                return false;
            }
            return true;
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text == null ? '' : String(text);
            return div.innerHTML;
        }

        function escapeAttribute(text) {
            return escapeHtml(text).replace(/"/g, '&quot;');
        }

        function escapeJsString(text) {
            return String(text == null ? '' : text)
                .replace(/\\/g, '\\\\')
                .replace(/'/g, "\\'")
                .replace(/\r/g, '\\r')
                .replace(/\n/g, '\\n');
        }

        function applyPreset(button) {
            document.querySelectorAll('.preset-btn').forEach(item => item.classList.remove('active'));
            button.classList.add('active');
            document.getElementById('filesInput').value = button.dataset.files;
            document.getElementById('recordsInput').value = button.dataset.records;
            document.getElementById('payloadInput').value = button.dataset.payload;
            document.getElementById('performanceMessage').textContent = '已应用“' + button.textContent.trim() + '”参数，可直接开始归档验证。';
        }

        // ==================== Tab 切换 ====================

        document.querySelectorAll('.tab-item').forEach(tab => {
            tab.addEventListener('click', function() {
                document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
                document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
                this.classList.add('active');
                document.getElementById('tab-' + this.dataset.tab).classList.add('active');
                if (this.dataset.tab === 'charts' || this.dataset.tab === 'analysis') {
                    setTimeout(() => resizeCharts(), 100);
                }
            });
        });

        function resizeCharts() {
            if (typeof echarts === 'undefined') return;
            if (trendChartInstance) trendChartInstance.resize();
            if (throughputChartInstance) throughputChartInstance.resize();
            if (timeChartInstance) timeChartInstance.resize();
            if (fileTypeChartInstance) fileTypeChartInstance.resize();
        }
        window.addEventListener('resize', resizeCharts);

        // ==================== 健康检查 ====================

        function loadHealth() {
            fetch('/api/hdfs/health')
                .then(res => res.json())
                .then(data => {
                    if (!ensureLogin(data)) return;
                    const health = data.data || {};
                    showStatus(Boolean(health.connected));
                    defaultBaseDir = health.baseDir || '';
                    document.getElementById('fsUri').textContent = health.uri || health.defaultFs || '--';
                    document.getElementById('baseDir').textContent = health.baseDir || '--';
                    document.getElementById('probeMillis').textContent = (health.probeMillis || 0) + ' ms';
                    document.getElementById('healthMessage').textContent = health.message || '';
                    document.getElementById('listPathInput').placeholder = health.baseDir || '默认使用归档目录';
                    document.getElementById('statsPathInput').placeholder = health.baseDir || '默认使用归档目录';
                    document.getElementById('analysisPathInput').placeholder = health.baseDir || '默认使用归档目录';
                    renderHaConfig(health.haConfig || {});
                    loadFiles();
                    initCharts();
                });
        }

        function renderHaConfig(config) {
            const body = document.getElementById('haConfigBody');
            const entries = Object.entries(config);
            if (entries.length === 0) {
                body.innerHTML = '<tr><td colspan="2">未读取到 HA 配置；当前使用本地 file:/// 模式，可用于无 Hadoop 集群环境下的文件读写验证。</td></tr>';
                return;
            }
            body.innerHTML = entries.map(([key, value]) => `
                <tr>
                    <td>${escapeHtml(key)}</td>
                    <td class="path-cell">${escapeHtml(value)}</td>
                </tr>
            `).join('');
        }

        // ==================== 性能测试 ====================

        function runPerformance() {
            const btn = document.getElementById('runPerfBtn');
            const progress = document.getElementById('progressBar');
            const message = document.getElementById('performanceMessage');
            const files = Number(document.getElementById('filesInput').value);
            const records = Number(document.getElementById('recordsInput').value);
            const payloadSize = Number(document.getElementById('payloadInput').value);
            if (files < 1 || files > 32 || records < 1 || records > 2000000 || payloadSize < 16 || payloadSize > 4096) {
                message.textContent = '请确认参数范围：文件数 1-32，记录数 1-2000000，单条填充字节 16-4096。';
                return;
            }
            btn.disabled = true;
            progress.style.display = 'block';
            message.textContent = '归档验证运行中，请稍候...';
            const params = new URLSearchParams();
            params.append('files', files);
            params.append('records', records);
            params.append('payloadSize', payloadSize);
            params.append('clean', document.getElementById('cleanInput').checked);
            fetch('/api/hdfs/performance', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params
            })
            .then(res => res.json())
            .then(data => {
                if (!ensureLogin(data)) return;
                progress.style.display = 'none';
                btn.disabled = false;
                if (data.code !== 200) {
                    message.textContent = data.message || '归档性能验证失败';
                    return;
                }
                renderPerformance(data.data || {});
                performanceHistory.push(data.data || {});
                updateCharts();
                loadFiles();
            })
            .catch(err => {
                progress.style.display = 'none';
                btn.disabled = false;
                message.textContent = err.message;
            });
        }

        function renderPerformance(result) {
            document.getElementById('writeRate').textContent = (result.writeMbPerSecond || 0) + ' MB/s';
            document.getElementById('readRate').textContent = (result.readMbPerSecond || 0) + ' MB/s';
            document.getElementById('recordRate').textContent = (result.recordsPerSecond || 0).toLocaleString() + ' rec/s';
            document.getElementById('totalMillis').textContent = (result.totalMillis || 0) + ' ms';
            document.getElementById('bytesValue').textContent = formatBytes(result.bytes);
            document.getElementById('recordsValue').textContent = (result.records || 0).toLocaleString();
            document.getElementById('filesValue').textContent = result.files || 0;
            document.getElementById('pathValue').textContent = result.path || '--';
            latestRunPath = result.path || '';
            document.getElementById('openRunFilesBtn').disabled = !latestRunPath;
            document.getElementById('reportSummary').textContent = buildReportSummary(result);
            const localTip = result.usedLocalFileSystem ? '当前使用本地 file:/// 模式；切换 HADOOP_DEFAULT_FS 后可连接 HA HDFS 集群。' : '当前使用 HDFS 集群。';
            document.getElementById('performanceMessage').textContent = (result.message || '') + ' ' + localTip;
        }

        function buildReportSummary(result) {
            const storage = result.usedLocalFileSystem ? '本地 file:///' : 'HDFS';
            return storage + ' 写入 ' + (result.files || 0) + ' 个财务流水文件，'
                + (result.records || 0).toLocaleString() + ' 条财务流水，数据量 '
                + formatBytes(result.bytes) + '；写入 ' + (result.writeMbPerSecond || 0)
                + ' MB/s，读取 ' + (result.readMbPerSecond || 0) + ' MB/s。输出目录：'
                + (result.path || '--');
        }

        function openLatestRunFiles() {
            if (!latestRunPath) return;
            document.getElementById('listPathInput').value = latestRunPath;
            document.querySelector('[data-tab="files"]').click();
            loadFiles();
        }

        // ==================== 文件管理 ====================

        function loadFiles() {
            const path = document.getElementById('listPathInput').value || defaultBaseDir;
            const query = path ? '?recursive=true&path=' + encodeURIComponent(path) : '?recursive=true';
            fetch('/api/hdfs/list' + query)
                .then(res => res.json())
                .then(data => {
                    if (!ensureLogin(data)) return;
                    const files = data.code === 200 ? (data.data || []) : [];
                    renderFiles(files);
                    if (data.code !== 200) {
                        document.getElementById('healthMessage').textContent = data.message || '';
                    }
                });
        }

        function renderFiles(files) {
            const body = document.getElementById('fileBody');
            if (!files.length) {
                body.innerHTML = '<tr><td colspan="7">当前归档目录暂无文件</td></tr>';
                return;
            }
            body.innerHTML = files.map((file, idx) => `
                <tr>
                    <td><input type="checkbox" class="file-checkbox" data-path="${escapeAttribute(file.path || '')}"></td>
                    <td>${escapeHtml(file.name || '')}</td>
                    <td class="path-cell">${escapeHtml(file.path || '')}</td>
                    <td>${formatBytes(file.length)}</td>
                    <td>${file.directory ? '目录' : '文件'}</td>
                    <td>${file.replication || '-'}</td>
                    <td>
                        ${!file.directory ? `
                            <button class="file-action-btn download" onclick="downloadFile('${escapeJsString(file.path)}')">下载</button>
                            <button class="file-action-btn preview" onclick="previewFile('${escapeJsString(file.path)}')">预览</button>
                        ` : ''}
                        <button class="file-action-btn delete" onclick="deleteFile('${escapeJsString(file.path)}', ${file.directory})">删除</button>
                    </td>
                </tr>
            `).join('');
        }

        function toggleSelectAll() {
            const checked = document.getElementById('selectAll').checked;
            document.querySelectorAll('.file-checkbox').forEach(cb => cb.checked = checked);
        }

        function downloadFile(path) {
            window.open('/api/hdfs/download?path=' + encodeURIComponent(path));
        }

        function previewFile(path) {
            document.getElementById('previewPathInput').value = path;
            document.querySelector('[data-tab="analysis"]').click();
            loadDataPreview();
        }

        function deleteFile(path, isDir) {
            if (!confirm('确定删除 ' + path + '？')) return;
            const params = new URLSearchParams();
            params.append('path', path);
            params.append('recursive', 'true');
            fetch('/api/hdfs/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params
            })
            .then(res => res.json())
            .then(data => {
                if (!ensureLogin(data)) return;
                document.getElementById('healthMessage').textContent = data.message || '';
                loadFiles();
            });
        }

        function batchDeleteFiles() {
            const checked = document.querySelectorAll('.file-checkbox:checked');
            if (checked.length === 0) { alert('请先选择文件'); return; }
            if (!confirm('确定删除选中的 ' + checked.length + ' 个文件？')) return;
            const paths = Array.from(checked).map(cb => cb.dataset.path);
            fetch('/api/hdfs/batch-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ paths: paths, recursive: true })
            })
            .then(res => res.json())
            .then(data => {
                if (!ensureLogin(data)) return;
                document.getElementById('healthMessage').textContent = data.message || '';
                loadFiles();
            });
        }

        function createDir() {
            const path = document.getElementById('listPathInput').value || defaultBaseDir;
            if (!path) { alert('请输入路径'); return; }
            const params = new URLSearchParams();
            params.append('path', path);
            fetch('/api/hdfs/mkdir', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params
            })
            .then(res => res.json())
            .then(data => {
                if (!ensureLogin(data)) return;
                document.getElementById('healthMessage').textContent = data.message || '';
                loadFiles();
            });
        }

        // ==================== 文件上传 ====================

        const uploadArea = document.getElementById('uploadArea');
        const fileInput = document.getElementById('fileInput');

        uploadArea.addEventListener('click', () => fileInput.click());
        uploadArea.addEventListener('dragover', e => { e.preventDefault(); uploadArea.classList.add('dragover'); });
        uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
        uploadArea.addEventListener('drop', e => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            uploadFiles(e.dataTransfer.files);
        });
        fileInput.addEventListener('change', () => { uploadFiles(fileInput.files); fileInput.value = ''; });

        function uploadFiles(files) {
            if (!files || files.length === 0) return;
            const progressDiv = document.getElementById('uploadProgress');
            const progressBar = document.getElementById('uploadProgressBar');
            const messageDiv = document.getElementById('uploadMessage');
            progressDiv.style.display = 'block';
            progressBar.style.width = '0%';
            messageDiv.textContent = '上传中...';

            let completed = 0;
            const total = files.length;
            const path = document.getElementById('listPathInput').value || defaultBaseDir;

            Array.from(files).forEach(file => {
                const formData = new FormData();
                formData.append('file', file);
                if (path) formData.append('path', path);
                formData.append('overwrite', 'true');

                fetch('/api/hdfs/upload', { method: 'POST', body: formData })
                    .then(res => res.json())
                    .then(data => {
                        if (!ensureLogin(data)) return;
                        completed++;
                        progressBar.style.width = (completed / total * 100) + '%';
                        if (completed === total) {
                            messageDiv.textContent = '上传完成！';
                            setTimeout(() => { progressDiv.style.display = 'none'; }, 2000);
                            loadFiles();
                        }
                    })
                    .catch(err => {
                        completed++;
                        messageDiv.textContent = '上传失败：' + err.message;
                    });
            });
        }

        // ==================== 数据分析 ====================

        function loadSizeStatistics() {
            const path = document.getElementById('statsPathInput').value || defaultBaseDir;
            fetch('/api/hdfs/size-statistics?path=' + encodeURIComponent(path) + '&topN=10')
                .then(res => res.json())
                .then(data => {
                    if (!ensureLogin(data)) return;
                    if (data.code !== 200) { alert(data.message); return; }
                    const stats = data.data || {};
                    document.getElementById('statTotalSize').textContent = formatBytes(stats.totalSize);
                    document.getElementById('statFileCount').textContent = stats.fileCount || 0;
                    document.getElementById('statDirCount').textContent = stats.directoryCount || 0;
                    document.getElementById('statSizeMb').textContent = (stats.sizeMb || 0).toFixed(2) + ' MB';

                    const topFiles = stats.topFiles || [];
                    const body = document.getElementById('topFilesBody');
                    if (!topFiles.length) {
                        body.innerHTML = '<tr><td colspan="3">暂无数据</td></tr>';
                    } else {
                        body.innerHTML = topFiles.map(f => `
                            <tr>
                                <td>${escapeHtml(f.name || '')}</td>
                                <td>${formatBytes(f.length)}</td>
                                <td class="path-cell">${escapeHtml(f.path || '')}</td>
                            </tr>
                        `).join('');
                    }
                });
        }

        function loadDirectoryAnalysis() {
            const path = document.getElementById('analysisPathInput').value || defaultBaseDir;
            fetch('/api/hdfs/directory-analysis?path=' + encodeURIComponent(path))
                .then(res => res.json())
                .then(data => {
                    if (!ensureLogin(data)) return;
                    if (data.code !== 200) { alert(data.message); return; }
                    const analysis = data.data || {};
                    document.getElementById('analysisFiles').textContent = analysis.totalFiles || 0;
                    document.getElementById('analysisDirs').textContent = analysis.totalDirectories || 0;
                    document.getElementById('analysisMaxFile').textContent = formatBytes(analysis.maxFileSize);
                    document.getElementById('analysisAvgSize').textContent = formatBytes(analysis.avgFileSize);

                    const typeDist = analysis.fileTypeDistribution || {};
                    renderFileTypeChart(typeDist);
                });
        }

        function loadDataPreview() {
            const path = document.getElementById('previewPathInput').value;
            const lines = document.getElementById('previewLinesInput').value || 20;
            if (!path) { alert('请输入文件路径'); return; }
            fetch('/api/hdfs/data-preview?path=' + encodeURIComponent(path) + '&lines=' + lines)
                .then(res => res.json())
                .then(data => {
                    if (!ensureLogin(data)) return;
                    if (data.code !== 200) {
                        document.getElementById('previewResult').innerHTML = '<div class="message">' + escapeHtml(data.message || '预览失败') + '</div>';
                        return;
                    }
                    const preview = data.data || {};
                    const sampleLines = preview.sampleLines || [];
                    const headers = preview.headers || [];
                    let html = '<div class="message">业务数据行数：' + (preview.totalLines || 0) + '，预览行数：' + (preview.previewLineCount || 0) + '</div>';
                    if (headers.length > 0) {
                        html += '<div style="margin-top:10px;font-weight:bold;color:#b8643c;">表头字段：' + headers.map(escapeHtml).join(' | ') + '</div>';
                    }
                    html += '<div class="preview-box" style="margin-top:10px;">';
                    sampleLines.forEach((line, i) => {
                        html += (i + 1) + ': ' + escapeHtml(line) + '\n';
                    });
                    html += '</div>';
                    document.getElementById('previewResult').innerHTML = html;
                });
        }

        // ==================== ECharts 图表 ====================

        function initCharts() {
            if (typeof echarts === 'undefined') {
                document.getElementById('trendChart').innerHTML = '<div class="message">图表库未加载，归档指标仍会在结果卡片中展示。</div>';
                document.getElementById('throughputChart').innerHTML = '<div class="message">图表库未加载。</div>';
                document.getElementById('timeChart').innerHTML = '<div class="message">图表库未加载。</div>';
                document.getElementById('fileTypeChart').innerHTML = '<div class="message">图表库未加载，目录统计仍可查看。</div>';
                return;
            }
            trendChartInstance = echarts.init(document.getElementById('trendChart'));
            throughputChartInstance = echarts.init(document.getElementById('throughputChart'));
            timeChartInstance = echarts.init(document.getElementById('timeChart'));
            fileTypeChartInstance = echarts.init(document.getElementById('fileTypeChart'));
            updateCharts();
        }

        function updateCharts() {
            updateTrendChart();
            updateThroughputChart();
            updateTimeChart();
        }

        function updateTrendChart() {
            if (!trendChartInstance) return;
            const labels = performanceHistory.map((_, i) => '第' + (i + 1) + '次');
            trendChartInstance.setOption({
                tooltip: { trigger: 'axis' },
                legend: { data: ['写入吞吐(MB/s)', '读取吞吐(MB/s)', '记录处理(rec/s)'] },
                grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
                xAxis: { type: 'category', data: labels },
                yAxis: [
                    { type: 'value', name: 'MB/s' },
                    { type: 'value', name: 'rec/s' }
                ],
                series: [
                    {
                        name: '写入吞吐(MB/s)', type: 'line', smooth: true,
                        data: performanceHistory.map(r => r.writeMbPerSecond || 0),
                        itemStyle: { color: '#b8643c' }
                    },
                    {
                        name: '读取吞吐(MB/s)', type: 'line', smooth: true,
                        data: performanceHistory.map(r => r.readMbPerSecond || 0),
                        itemStyle: { color: '#486a7c' }
                    },
                    {
                        name: '记录处理(rec/s)', type: 'line', smooth: true, yAxisIndex: 1,
                        data: performanceHistory.map(r => r.recordsPerSecond || 0),
                        itemStyle: { color: '#6e7f73' }
                    }
                ]
            });
        }

        function updateThroughputChart() {
            if (!throughputChartInstance || performanceHistory.length === 0) return;
            const latest = performanceHistory[performanceHistory.length - 1];
            throughputChartInstance.setOption({
                tooltip: { trigger: 'axis' },
                legend: { data: ['写入', '读取'] },
                grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
                xAxis: { type: 'category', data: ['吞吐量 (MB/s)'] },
                yAxis: { type: 'value', name: 'MB/s' },
                series: [
                    {
                        name: '写入', type: 'bar',
                        data: [latest.writeMbPerSecond || 0],
                        itemStyle: { color: '#b8643c' }, barWidth: '30%'
                    },
                    {
                        name: '读取', type: 'bar',
                        data: [latest.readMbPerSecond || 0],
                        itemStyle: { color: '#486a7c' }, barWidth: '30%'
                    }
                ]
            });
        }

        function updateTimeChart() {
            if (!timeChartInstance || performanceHistory.length === 0) return;
            const latest = performanceHistory[performanceHistory.length - 1];
            timeChartInstance.setOption({
                tooltip: { trigger: 'item' },
                legend: { orient: 'vertical', left: 'left' },
                series: [{
                    name: '耗时分布', type: 'pie', radius: ['40%', '70%'],
                    avoidLabelOverlap: false,
                    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
                    label: { show: true, formatter: '{b}: {c}ms' },
                    data: [
                        { value: latest.writeMillis || 0, name: '写入耗时', itemStyle: { color: '#b8643c' } },
                        { value: latest.readMillis || 0, name: '读取耗时', itemStyle: { color: '#486a7c' } },
                        { value: Math.max(0, (latest.totalMillis || 0) - (latest.writeMillis || 0) - (latest.readMillis || 0)), name: '其他耗时', itemStyle: { color: '#c58a3a' } }
                    ]
                }]
            });
        }

        function renderFileTypeChart(typeDist) {
            if (!fileTypeChartInstance) return;
            const data = Object.entries(typeDist).map(([name, value]) => ({ name: name, value: value }));
            fileTypeChartInstance.setOption({
                tooltip: { trigger: 'item' },
                legend: { orient: 'vertical', left: 'left' },
                series: [{
                    name: '文件类型', type: 'pie', radius: '60%',
                    data: data,
                    emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
                }]
            });
        }

        // ==================== 初始化 ====================
        loadHealth();
