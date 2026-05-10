/**
 * app.js — 主应用控制器
 * 串联 api.js / charts.js，管理页面交互与数据渲染
 * 依赖：HdfsApi（api.js）、Charts（charts.js）、ECharts CDN
 * 适配 Member 4 的 index.html / layout.html 实际 DOM 结构
 */
(function () {
  'use strict';

  // ==================== 应用状态 ====================

  const state = {
    currentPath: '/',
    fileList: [],
    searchDebounceTimer: null,
    performanceHistory: [],       // 存储多次性能测试结果，用于趋势图
    sortField: 'name',           // 当前排序字段
    sortAsc: true,               // 是否升序
  };

  // ==================== 增强 Toast 通知 ====================

  const Toast = {
    _counter: 0,

    show(message, type, duration) {
      // 优先使用 layout.html 提供的 showToast
      if (typeof window.showToast === 'function') {
        window.showToast(message, type || 'info');
        return '';
      }
      // 降级实现（与 CSS @keyframes toastIn 兼容）
      const container = document.getElementById('toast-container');
      if (!container) return '';
      const icons = { success: '✔', error: '✘', warning: '⚠', info: 'ℹ' };
      const el = document.createElement('div');
      el.className = `toast toast-${type || 'info'}`;
      el.innerHTML = `<span class="toast-icon">${icons[type] || icons.info}</span>
                      <span class="toast-msg">${message}</span>`;
      el.addEventListener('click', () => { if (el.parentNode) el.remove(); });
      container.appendChild(el); // CSS animation 自动播放
      if ((duration || 3500) > 0) {
        setTimeout(() => { if (el.parentNode) el.remove(); }, duration || 3500);
      }
      return '';
    },

    success(msg) { return this.show(msg, 'success'); },
    error(msg)   { return this.show(msg, 'error', 5000); },
    warning(msg) { return this.show(msg, 'warning'); },
    info(msg)    { return this.show(msg, 'info'); },
  };

  // ==================== 工具函数 ====================

  function formatSize(bytes) {
    if (bytes == null || bytes < 0) return '--';
    if (bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 2)} ${units[i]}`;
  }

  function formatTime(timestamp) {
    if (!timestamp) return '--';
    const d = new Date(timestamp);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function esc(str) {
    const div = document.createElement('div');
    div.textContent = String(str ?? '');
    return div.innerHTML;
  }

  function getEl(id) {
    return document.getElementById(id);
  }

  /** 元素存在且可设置文本 */
  function setText(id, text) {
    const el = getEl(id);
    if (el) el.textContent = text;
  }

  // ==================== 加载状态管理 ====================

  /** 文件列表区域：切换 spinner / table / empty */
  function setTableLoading(loading) {
    const spinner = getEl('table-spinner');
    const table = getEl('file-table');
    const empty = getEl('table-empty');
    if (loading) {
      if (spinner) spinner.style.display = 'flex';
      if (table) table.style.display = 'none';
      if (empty) empty.style.display = 'none';
    } else {
      if (spinner) spinner.style.display = 'none';
    }
  }

  function setTableVisible(hasData) {
    const table = getEl('file-table');
    const empty = getEl('table-empty');
    if (table) table.style.display = hasData ? '' : 'none';
    if (empty) empty.style.display = hasData ? 'none' : '';
  }

  /** 分析区域 spinner */
  function setAnalyzeLoading(loading) {
    const spinner = getEl('analyze-spinner');
    if (spinner) spinner.style.display = loading ? 'flex' : 'none';
  }

  /** 图表区域 spinner */
  function setChartSpinner(chartBodyId, loading) {
    // 尝试查找对应 chart 区域的 spinner
    const map = {
      'chart-perf-body': 'chart-perf-spinner',
      'chart-dist-body': 'chart-dist-spinner',
      'chart-storage-body': 'chart-storage-spinner',
    };
    const spinnerId = map[chartBodyId];
    if (spinnerId) {
      const spinner = getEl(spinnerId);
      if (spinner) spinner.style.display = loading ? 'flex' : 'none';
    }
  }

  // ==================== 面包屑导航 ====================

  function renderBreadcrumb(path) {
    const container = getEl('breadcrumb-nav');
    if (!container) return;

    const parts = path === '/' ? [] : path.split('/').filter(Boolean);
    container.innerHTML = '';

    // 根目录（始终可点击）
    const root = document.createElement('span');
    root.className = 'breadcrumb-item breadcrumb-root';
    root.setAttribute('data-path', '/');
    root.textContent = '/';
    root.addEventListener('click', () => loadFileList('/'));
    container.appendChild(root);

    let cumulative = '';
    parts.forEach((part, idx) => {
      cumulative += '/' + part;
      // 分隔符
      const sep = document.createElement('span');
      sep.className = 'breadcrumb-sep';
      sep.textContent = ' › ';
      container.appendChild(sep);

      if (idx === parts.length - 1) {
        // 当前目录（加粗，不可点击）
        const cur = document.createElement('span');
        cur.className = 'breadcrumb-item current';
        cur.textContent = part;
        container.appendChild(cur);
      } else {
        const link = document.createElement('span');
        link.className = 'breadcrumb-item';
        link.setAttribute('data-path', cumulative);
        link.textContent = part;
        link.addEventListener('click', () => loadFileList(cumulative));
        container.appendChild(link);
      }
    });
  }

  // ==================== 文件表格渲染 ====================

  /** 表格排序 */
  function sortFiles(files) {
    const sorted = [...(files || [])];
    const dirs = sorted.filter(f => f.directory === true);
    const docs = sorted.filter(f => f.directory !== true);

    const cmp = (a, b) => {
      let va, vb;
      switch (state.sortField) {
        case 'name':
          va = (a.name || '').toLowerCase();
          vb = (b.name || '').toLowerCase();
          break;
        case 'size':
          va = a.size || 0;
          vb = b.size || 0;
          break;
        case 'modificationTime':
          va = a.modificationTime || 0;
          vb = b.modificationTime || 0;
          break;
        default:
          return 0;
      }
      if (va < vb) return state.sortAsc ? -1 : 1;
      if (va > vb) return state.sortAsc ? 1 : -1;
      return 0;
    };

    return [...dirs.sort(cmp), ...docs.sort(cmp)];
  }

  function renderFileTable(files) {
    const tbody = getEl('file-table-body');
    if (!tbody) return;

    state.fileList = Array.isArray(files) ? files : [];
    const sorted = sortFiles(state.fileList);

    if (sorted.length === 0) {
      tbody.innerHTML = '';
      setTableVisible(false);
      return;
    }

    tbody.innerHTML = sorted.map(f => {
      const isDir = f.directory === true;
      const icon = isDir ? '\u{1F4C1}' : '\u{1F4C4}';
      const safePath = esc(f.path || '');
      const safeName = esc(f.name || '');

      return `<tr>
        <td>
          <span class="file-name${isDir ? ' dir' : ''}"
            ${isDir ? `onclick="window.App&&window.App.navigateTo('${safePath}')" title="打开目录"` : ''}>
            ${icon} ${safeName}
          </span>
        </td>
        <td style="width:120px;">${isDir ? '--' : formatSize(f.size)}</td>
        <td style="width:170px;">${formatTime(f.modificationTime)}</td>
        <td style="width:180px;">
          ${!isDir ? `<button class="btn btn-outline btn-sm" title="下载"
            onclick="window.App&&window.App.triggerDownload('${safePath}')">
            \u{2B07} 下载</button>` : ''}
          <button class="btn btn-outline btn-sm" title="删除" style="color:var(--danger);margin-left:${isDir ? 0 : 6}px;"
            onclick="window.App&&window.App.triggerDelete('${safePath}')">
            \u{1F5D1} 删除</button>
        </td>
      </tr>`;
    }).join('');

    setTableVisible(true);
  }

  // ==================== 统计卡片 ====================

  function renderStats(data) {
    if (!data) return;
    setText('stat-total-files', data.fileCount != null ? data.fileCount : '--');
    setText('stat-total-size',  data.totalSize != null ? formatSize(data.totalSize) : '--');
    setText('stat-dir-count',   data.dirCount != null ? data.dirCount : '--');

    const statusEl = getEl('stat-hdfs-status');
    if (statusEl) {
      statusEl.textContent = data.fileCount != null ? '\u{2705} 在线' : '\u{26A0} 异常';
    }
    // 同步 header 状态徽标
    const dotEl = getEl('status-dot');
    const textEl = getEl('status-text');
    if (data.fileCount != null) {
      if (dotEl) { dotEl.className = 'status-dot online'; }
      if (textEl) textEl.textContent = 'HDFS 在线';
    } else {
      if (dotEl) { dotEl.className = 'status-dot offline'; }
      if (textEl) textEl.textContent = 'HDFS 离线';
    }
    // 子标题
    setText('stat-files-sub', data.fileCount != null ? '已加载' : '加载失败');
    setText('stat-size-sub',  data.totalSize != null ? formatSize(data.totalSize) : '加载失败');
    setText('stat-dirs-sub',  data.dirCount != null ? '已加载' : '加载失败');
    setText('stat-status-sub', data.fileCount != null ? '连接正常' : '无法连接');
  }

  // ==================== 数据加载 ====================

  async function loadStats() {
    try {
      const data = await window.HdfsApi.getStats(state.currentPath);
      renderStats(data);
    } catch (err) {
      // 错误已被 api.js toast
      const statusEl = getEl('stat-hdfs-status');
      if (statusEl) statusEl.textContent = '\u{274C} 获取失败';
      const dotEl = getEl('status-dot');
      if (dotEl) dotEl.className = 'status-dot offline';
      const textEl = getEl('status-text');
      if (textEl) textEl.textContent = 'HDFS 离线';
    }
  }

  async function loadFileList(path) {
    if (path !== undefined) state.currentPath = path;

    setTableLoading(true);
    try {
      const files = await window.HdfsApi.listFiles(state.currentPath);
      renderFileTable(files);
      renderBreadcrumb(state.currentPath);
      loadStats();
    } catch (err) {
      renderFileTable([]);
      renderBreadcrumb(state.currentPath);
    } finally {
      setTableLoading(false);
    }
  }

  // ==================== 操作处理 ====================

  /** 上传文件 */
  async function handleUpload(event) {
    if (event && typeof event.preventDefault === 'function') {
      event.preventDefault();
    }

    const fileInput = getEl('upload-file-input');
    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
      Toast.warning('请先选择要上传的文件');
      return;
    }

    const pathInput = getEl('upload-target-path');
    const path = (pathInput && pathInput.value) ? pathInput.value : state.currentPath;
    const files = fileInput.files;
    const total = files.length;

    // 更新进度显示
    const progressBar = getEl('progress-bar-fill');
    const progressStatus = getEl('progress-status');
    const progressPercent = getEl('progress-percent');

    if (progressBar) progressBar.style.width = '0%';
    if (progressPercent) progressPercent.textContent = '0%';

    try {
      if (total === 1) {
        if (progressStatus) progressStatus.textContent = `正在上传 ${files[0].name}...`;
        await window.HdfsApi.uploadFile(files[0], path);
        if (progressBar) progressBar.style.width = '100%';
        if (progressPercent) progressPercent.textContent = '100%';
        Toast.success(`"${files[0].name}" 上传成功`);
      } else {
        if (progressStatus) progressStatus.textContent = `正在上传 ${total} 个文件...`;
        const result = await window.HdfsApi.batchUpload(files, path);
        if (progressBar) progressBar.style.width = '100%';
        if (progressPercent) progressPercent.textContent = '100%';
        Toast.success(`批量上传完成：成功 ${result.success || total} 个，失败 ${result.failed || 0} 个`);
      }

      fileInput.value = '';
      // 清空已选文件列表
      const fileListEl = getEl('upload-file-list');
      if (fileListEl) { fileListEl.style.display = 'none'; fileListEl.innerHTML = ''; }
      // 隐藏清空按钮
      const clearBtn = getEl('btn-upload-clear');
      if (clearBtn) clearBtn.style.display = 'none';
      // 恢复进度文字
      if (progressStatus) progressStatus.textContent = '准备上传...';
      if (progressPercent) progressPercent.textContent = '0%';
      if (progressBar) progressBar.style.width = '0%';

      loadFileList(state.currentPath);
    } catch (err) {
      if (progressStatus) progressStatus.textContent = '上传失败';
      // 错误已由 api.js 处理
    }
  }

  /** 上传按钮点击（通过 id 绑定） */
  function onUploadClick() {
    handleUpload({ preventDefault: () => {} });
  }

  /** 清空已选文件 */
  function onClearFiles() {
    const fileInput = getEl('upload-file-input');
    if (fileInput) fileInput.value = '';
    const fileListEl = getEl('upload-file-list');
    if (fileListEl) { fileListEl.style.display = 'none'; fileListEl.innerHTML = ''; }
    const clearBtn = getEl('btn-upload-clear');
    if (clearBtn) clearBtn.style.display = 'none';
    const progressBar = getEl('progress-bar-fill');
    if (progressBar) progressBar.style.width = '0%';
    const progressStatus = getEl('progress-status');
    if (progressStatus) progressStatus.textContent = '准备上传...';
    const progressPercent = getEl('progress-percent');
    if (progressPercent) progressPercent.textContent = '0%';
  }

  /** 删除文件/目录 — 使用 layout.html 的 shared modal */
  async function triggerDelete(path) {
    const name = path.split('/').pop() || path;
    if (typeof window.showModal === 'function') {
      window.showModal(
        '确认删除',
        `<p>确定要删除 <strong>"${esc(name)}"</strong> 吗？</p><p style="color:var(--danger);font-size:13px;">此操作不可撤销。</p>`,
        () => doDelete(path)
      );
    } else if (confirm(`确定要删除 "${name}" 吗？此操作不可撤销。`)) {
      doDelete(path);
    }
  }

  async function doDelete(path) {
    try {
      await window.HdfsApi.deleteFile(path);
      Toast.success('删除成功');
      loadFileList(state.currentPath);
    } catch (err) {
      // 错误已由 api.js 处理
    }
  }

  /** 下载文件 */
  async function triggerDownload(path) {
    try {
      await window.HdfsApi.downloadFile(path);
    } catch (err) {
      // 错误已由 api.js 处理
    }
  }

  // ==================== 新建目录（三步交互） ====================

  function enterMkdirMode() {
    const mkdirBtn = getEl('btn-mkdir');
    const analyzeBtn = getEl('btn-analyze');
    const input = getEl('input-mkdir-name');
    const confirm = getEl('btn-mkdir-confirm');
    const cancel = getEl('btn-mkdir-cancel');
    if (mkdirBtn) mkdirBtn.style.display = 'none';
    if (analyzeBtn) analyzeBtn.style.display = 'none';
    if (input) { input.style.display = ''; input.value = ''; input.focus(); }
    if (confirm) confirm.style.display = '';
    if (cancel) cancel.style.display = '';
  }

  function exitMkdirMode() {
    const mkdirBtn = getEl('btn-mkdir');
    const analyzeBtn = getEl('btn-analyze');
    const input = getEl('input-mkdir-name');
    const confirm = getEl('btn-mkdir-confirm');
    const cancel = getEl('btn-mkdir-cancel');
    if (mkdirBtn) mkdirBtn.style.display = '';
    if (analyzeBtn) analyzeBtn.style.display = '';
    if (input) { input.style.display = 'none'; input.value = ''; }
    if (confirm) confirm.style.display = 'none';
    if (cancel) cancel.style.display = 'none';
  }

  async function confirmMkdir() {
    const input = getEl('input-mkdir-name');
    const dirName = input ? input.value.trim() : '';
    if (!dirName) {
      Toast.warning('请输入目录名称');
      return;
    }
    const newPath = state.currentPath === '/'
      ? `/${dirName}`
      : `${state.currentPath}/${dirName}`;

    try {
      await window.HdfsApi.createDir(newPath);
      Toast.success(`目录 "${dirName}" 创建成功`);
      exitMkdirMode();
      loadFileList(state.currentPath);
    } catch (err) {
      // 错误已由 api.js 处理
    }
  }

  // ==================== 搜索（带 300ms 防抖） ====================

  function handleSearch(keyword) {
    // 如果页面没有搜索输入框，该功能降级为空操作
    clearTimeout(state.searchDebounceTimer);
    state.searchDebounceTimer = setTimeout(async () => {
      const kw = (keyword || '').trim();
      if (!kw) {
        loadFileList();
        return;
      }
      try {
        setTableLoading(true);
        const results = await window.HdfsApi.searchFiles(state.currentPath, kw);
        renderFileTable(results);
        setTableLoading(false);
        Toast.info(`找到 ${results ? results.length : 0} 个结果`);
      } catch (err) {
        setTableLoading(false);
      }
    }, 300);
  }

  // ==================== 目录分析 ====================

  async function handleAnalyze() {
    setAnalyzeLoading(true);
    try {
      const data = await window.HdfsApi.analyzeDir(state.currentPath);

      // 设置分析路径标题
      const pathEl = getEl('analyze-path');
      if (pathEl) pathEl.textContent = state.currentPath;

      // 渲染分析结果网格
      const gridEl = getEl('analyze-grid');
      if (gridEl) {
        const typeTags = data.typeDistribution
          ? Object.entries(data.typeDistribution)
              .map(([k, v]) => `<span class="type-tag">${esc(k)}: ${v}</span>`)
              .join('')
          : '<span class="type-tag">无类型数据</span>';

        gridEl.innerHTML = `
          <div class="analyze-item"><label>总大小</label><span>${formatSize(data.totalSize)}</span></div>
          <div class="analyze-item"><label>文件数</label><span>${data.fileCount != null ? data.fileCount : '--'}</span></div>
          <div class="analyze-item"><label>最大文件</label><span>${esc(data.largestFile || '--')}</span></div>
          <div class="analyze-item"><label>最小文件</label><span>${esc(data.smallestFile || '--')}</span></div>
          <div class="analyze-item"><label>平均大小</label><span>${formatSize(data.avgSize)}</span></div>
          <div class="analyze-item" style="grid-column:1/-1;">
            <label>类型分布</label>
            <div class="type-dist">${typeTags}</div>
          </div>`;
      }

      // 渲染文件类型饼图（使用图表区 div，优先 file-type-pie，次选 chart-dist-body）
      if (window.Charts) {
        const pieContainer = document.getElementById('chart-file-type-pie')
          || getEl('chart-dist-body');
        if (pieContainer) {
          setChartSpinner(pieContainer.id, false);
          window.Charts.initFileTypePie(pieContainer.id, data);
        }
      }

      Toast.success('目录分析完成');
    } catch (err) {
      // 错误已由 api.js 处理
    } finally {
      setAnalyzeLoading(false);
    }
  }

  // ==================== 性能测试 ====================

  async function runPerformanceTest() {
    Toast.info('正在运行性能测试，请稍候...');
    try {
      const data = await window.HdfsApi.runPerfTest();

      // 记录历史
      state.performanceHistory.push({
        time: new Date().toISOString(),
        uploadTime: data.uploadTime,
        listTime: data.listTime,
        downloadTime: data.downloadTime,
        deleteTime: data.deleteTime,
        totalTime: data.totalTime,
      });

      if (window.Charts) {
        // 性能柱状图 → chart-perf-body
        const perfDiv = getEl('chart-perf-body');
        if (perfDiv) {
          setChartSpinner(perfDiv.id, false);
          window.Charts.initPerformanceChart(perfDiv.id, data);
        }

        // 吞吐量仪表盘 → chart-dist-body
        const gaugeDiv = getEl('chart-throughput-gauge') || getEl('chart-dist-body');
        if (gaugeDiv) {
          setChartSpinner(gaugeDiv.id, false);
          window.Charts.initThroughputChart(gaugeDiv.id, {
            tps: data.totalTime > 0 ? Math.round((1000 / data.totalTime) * 100) / 100 : 0,
          });
        }

        // 趋势图 → chart-storage-body（如果多轮）
        if (state.performanceHistory.length >= 2) {
          const trendDiv = getEl('chart-trend-line') || getEl('chart-storage-body');
          if (trendDiv) {
            setChartSpinner(trendDiv.id, false);
            window.Charts.initTrendLine(trendDiv.id, state.performanceHistory);
          }
        }
      }

      Toast.success(`性能测试完成（总计: ${data.totalTime} ms）`);
    } catch (err) {
      // 错误已由 api.js 处理
    }
  }

  /** 加载性能报告 */
  async function loadPerformanceReport() {
    try {
      const data = await window.HdfsApi.getPerfReport();

      // 报告表格（如果存在）
      const perfTbody = getEl('perf-report-table');
      if (perfTbody && data && data.endpoints) {
        perfTbody.innerHTML = data.endpoints.map(ep => `
          <tr>
            <td>${esc(ep.name)}</td>
            <td>${ep.avgTime != null ? ep.avgTime.toFixed(2) + ' ms' : '--'}</td>
            <td>${ep.tps != null ? ep.tps.toFixed(2) : '--'}</td>
            <td>${ep.totalRequests != null ? ep.totalRequests : '--'}</td>
            <td>
              <span class="error-tag ${ep.errorRate > 0.05 ? 'high' : ep.errorRate > 0 ? 'warn' : 'ok'}">
                ${ep.errorRate != null ? (ep.errorRate * 100).toFixed(2) + '%' : '0%'}
              </span>
            </td>
          </tr>`).join('');
      }

      // 更新图表
      if (window.Charts && data && data.endpoints) {
        const perfData = {};
        data.endpoints.forEach(ep => {
          switch (ep.name) {
            case 'upload':   perfData.uploadTime   = ep.avgTime; break;
            case 'list':     perfData.listTime     = ep.avgTime; break;
            case 'download': perfData.downloadTime = ep.avgTime; break;
            case 'delete':   perfData.deleteTime   = ep.avgTime; break;
          }
        });
        perfData.totalTime = data.endpoints.reduce((s, e) => s + (e.avgTime || 0), 0);

        const perfDiv = getEl('chart-perf-body');
        if (perfDiv) window.Charts.initPerformanceChart(perfDiv.id, perfData);

        const gaugeDiv = getEl('chart-throughput-gauge') || getEl('chart-dist-body');
        if (gaugeDiv) {
          const totalTps = data.endpoints.reduce((s, e) => s + (e.tps || 0), 0);
          window.Charts.initThroughputChart(gaugeDiv.id, { tps: totalTps });
        }

        // 存储概览图
        const storageDiv = getEl('chart-storage-body');
        if (storageDiv && data.storageDistribution) {
          window.Charts.initStorageBar(storageDiv.id, data.storageDistribution);
        }
      }

      Toast.success('性能报告加载完成');
    } catch (err) {
      // 错误已由 api.js 处理
    }
  }

  // ==================== 拖拽上传 ====================

  function setupDragDrop() {
    const dropZone = getEl('upload-zone');
    const fileInput = getEl('upload-file-input');
    if (!dropZone) return;

    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(evt => {
      dropZone.addEventListener(evt, e => {
        e.preventDefault();
        e.stopPropagation();
      });
    });

    dropZone.addEventListener('dragenter', () => dropZone.classList.add('drag-over'));
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));

    dropZone.addEventListener('drop', e => {
      dropZone.classList.remove('drag-over');
      const dt = e.dataTransfer;
      if (!dt || !dt.files || dt.files.length === 0) return;

      // 尝试同步到 file input
      if (fileInput) {
        try {
          const dtt = new DataTransfer();
          Array.from(dt.files).forEach(f => dtt.items.add(f));
          fileInput.files = dtt.files;
        } catch (ex) {
          /* 降级：无法同步，input 保留用户手动选择 */
        }
      }

      // 显示已选文件列表
      showSelectedFiles(dt.files);

      // 视觉反馈
      dropZone.style.borderColor = '#52C41A';
      dropZone.style.backgroundColor = 'rgba(82,196,26,0.06)';
      setTimeout(() => {
        dropZone.style.borderColor = '';
        dropZone.style.backgroundColor = '';
      }, 1500);

      Toast.info(`已选择 ${dt.files.length} 个文件，请点击"开始上传"`);
    });

    // 点击 dropZone 触发文件选择
    dropZone.addEventListener('click', e => {
      if (e.target === fileInput) return; // input 自身会处理
      if (fileInput) fileInput.click();
    });
  }

  /** 在 upload-file-list 中展示已选文件 */
  function showSelectedFiles(files) {
    const listEl = getEl('upload-file-list');
    const clearBtn = getEl('btn-upload-clear');
    if (!listEl) return;

    const arr = Array.from(files);
    if (arr.length === 0) {
      listEl.style.display = 'none';
      listEl.innerHTML = '';
      if (clearBtn) clearBtn.style.display = 'none';
      return;
    }

    listEl.innerHTML = arr.map(f => `
      <div class="upload-file-item">
        <span class="upload-file-icon">\u{1F4C4}</span>
        <span class="upload-file-name">${esc(f.name)}</span>
        <span class="upload-file-size">${formatSize(f.size)}</span>
      </div>`).join('');
    listEl.style.display = '';
    if (clearBtn) clearBtn.style.display = '';
  }

  // ==================== 事件绑定 ====================

  function bindEvents() {
    // ---- 刷新按钮 ----
    const refreshBtn = getEl('btn-refresh');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => loadFileList());
    }

    // ---- 上传表单 ----
    const uploadForm = getEl('upload-form');
    if (uploadForm) {
      uploadForm.addEventListener('submit', handleUpload);
    }

    // 上传按钮（通过 id）
    const uploadBtn = getEl('btn-upload-start');
    if (uploadBtn) {
      uploadBtn.addEventListener('click', onUploadClick);
    }

    // 清空文件列表
    const clearBtn = getEl('btn-upload-clear');
    if (clearBtn) {
      clearBtn.addEventListener('click', onClearFiles);
    }

    // 文件 input 变化时显示已选文件
    const fileInput = getEl('upload-file-input');
    if (fileInput) {
      fileInput.addEventListener('change', () => {
        if (fileInput.files && fileInput.files.length > 0) {
          showSelectedFiles(fileInput.files);
        }
      });
    }

    // ---- 新建目录（三步交互） ----
    const mkdirBtn = getEl('btn-mkdir');
    if (mkdirBtn) {
      mkdirBtn.addEventListener('click', enterMkdirMode);
    }
    const mkdirConfirm = getEl('btn-mkdir-confirm');
    if (mkdirConfirm) {
      mkdirConfirm.addEventListener('click', confirmMkdir);
    }
    const mkdirCancel = getEl('btn-mkdir-cancel');
    if (mkdirCancel) {
      mkdirCancel.addEventListener('click', exitMkdirMode);
    }
    // Enter 键确认
    const mkdirInput = getEl('input-mkdir-name');
    if (mkdirInput) {
      mkdirInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') confirmMkdir();
        if (e.key === 'Escape') exitMkdirMode();
      });
    }

    // ---- 分析按钮 ----
    const analyzeBtn = getEl('btn-analyze');
    if (analyzeBtn) {
      analyzeBtn.addEventListener('click', handleAnalyze);
    }

    // ---- 搜索（如果存在） ----
    const searchInput = getEl('search-input');
    if (searchInput) {
      searchInput.addEventListener('input', e => handleSearch(e.target.value));
    }
    const searchBtn = getEl('search-btn');
    if (searchBtn && searchInput) {
      searchBtn.addEventListener('click', () => handleSearch(searchInput.value));
    }

    // ---- 性能测试按钮（如果存在） ----
    const perfTestBtn = document.querySelector('[data-action="perf-test"]');
    if (perfTestBtn) {
      perfTestBtn.addEventListener('click', runPerformanceTest);
    }
    const perfReportBtn = document.querySelector('[data-action="perf-report"]');
    if (perfReportBtn) {
      perfReportBtn.addEventListener('click', loadPerformanceReport);
    }

    // ---- 表格列头排序 ----
    const sortHeaders = document.querySelectorAll('th.sortable');
    sortHeaders.forEach(th => {
      th.addEventListener('click', () => {
        const field = th.getAttribute('data-sort');
        if (!field) return;
        if (state.sortField === field) {
          state.sortAsc = !state.sortAsc;
        } else {
          state.sortField = field;
          state.sortAsc = true;
        }
        // 更新排序箭头样式
        sortHeaders.forEach(h => {
          const arrow = h.querySelector('.sort-arrow');
          if (arrow) {
            arrow.style.visibility = (h === th) ? '' : 'hidden';
            arrow.textContent = (h === th)
              ? (state.sortAsc ? '▲' : '▼')
              : '▲';
          }
        });
        renderFileTable(state.fileList);
      });
    });

    // ---- 拖拽上传 ----
    setupDragDrop();
  }

  // ==================== App 命名空间 ====================

  window.App = {
    navigateTo(path)     { loadFileList(path); },
    triggerDelete(path)  { triggerDelete(path).catch(() => {}); },
    triggerDownload(path){ triggerDownload(path).catch(() => {}); },
    refresh()            { loadFileList(); },
    getState()           { return state; },
  };

  // ==================== 启动入口 ====================

  document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    loadStats();
    loadFileList('/');
  });

})();
