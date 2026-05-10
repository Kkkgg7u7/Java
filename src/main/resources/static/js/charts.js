/**
 * charts.js — ECharts 可视化模块
 * 依赖：ECharts CDN（由 index.html 引入）
 * 提供柱状图、仪表盘、饼图、横向条形图、折线图等
 */
(function () {
  'use strict';

  // ==================== 内部工具 ====================

  /** 存储所有图表实例，用于 resize 和 update */
  const chartInstances = {};

  /** 专业配色方案（蓝/青） */
  const COLORS = {
    primary: ['#1677FF', '#13C2C2', '#36CFC9', '#40A9FF', '#69C0FF', '#91D5FF', '#B5F5EC', '#87E8DE'],
    success: '#52C41A',
    warning: '#FAAD14',
    danger: '#FF4D4F',
    gauge: [
      [0.3, '#52C41A'],
      [0.6, '#1677FF'],
      [0.8, '#FAAD14'],
      [1.0, '#FF4D4F'],
    ],
    background: 'rgba(22, 119, 255, 0.06)',
    text: '#595959',
    axisLine: '#E8E8E8',
  };

  /** 通用 tooltip 配置——显示单位 */
  function tooltipWithUnit(unit) {
    return {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: '#fff',
      borderColor: '#E8E8E8',
      textStyle: { color: '#262626', fontSize: 13 },
      extraCssText: 'box-shadow: 0 3px 12px rgba(0,0,0,0.12); border-radius: 6px;',
      formatter: function (params) {
        if (!params || !params.length) return '';
        const p = Array.isArray(params) ? params[0] : params;
        return `<strong>${p.name}</strong><br/>
          <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:6px;"></span>
          ${p.seriesName}: <strong>${p.value}${unit || ''}</strong>`;
      },
    };
  }

  /** 通用 tooltip（饼图） */
  function pieTooltip() {
    return {
      trigger: 'item',
      backgroundColor: '#fff',
      borderColor: '#E8E8E8',
      textStyle: { color: '#262626', fontSize: 13 },
      extraCssText: 'box-shadow: 0 3px 12px rgba(0,0,0,0.12); border-radius: 6px;',
      formatter: '{b}: {c} 个 ({d}%)',
    };
  }

  /** 获取空数据占位 option */
  function emptyPlaceholder(title) {
    return {
      title: {
        text: title || '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#BFBFBF', fontSize: 16, fontWeight: 'normal' },
      },
      graphic: {
        type: 'text',
        left: 'center',
        top: '52%',
        style: { text: '暂无数据', textAlign: 'center', fill: '#BFBFBF', fontSize: 13 },
      },
    };
  }

  /** 初始化 ECharts 实例 */
  function getChartInstance(domId) {
    if (chartInstances[domId]) return chartInstances[domId];
    const dom = document.getElementById(domId);
    if (!dom) {
      console.warn(`[charts] DOM 元素 #${domId} 不存在`);
      return null;
    }
    if (typeof echarts === 'undefined') {
      console.error('[charts] ECharts 未加载，请检查 CDN 引入');
      return null;
    }
    const instance = echarts.init(dom);
    chartInstances[domId] = instance;
    return instance;
  }

  /** 窗口 resize 时同步重绘所有图表 */
  window.addEventListener('resize', () => {
    Object.values(chartInstances).forEach(inst => {
      try { inst.resize(); } catch (e) { /* ignore */ }
    });
  });

  // ==================== Charts 命名空间 ====================

  window.Charts = {

    /**
     * 性能柱状图 — 各操作耗时对比
     * @param {string} domId - 容器 DOM id
     * @param {object} data  - { uploadTime, listTime, downloadTime, deleteTime, totalTime }
     */
    initPerformanceChart(domId, data) {
      const chart = getChartInstance(domId);
      if (!chart) return;

      const categories = ['上传', '列表', '下载', '删除', '总计'];
      const values = data
        ? [data.uploadTime, data.listTime, data.downloadTime, data.deleteTime, data.totalTime]
        : [];

      const hasData = values.length > 0 && values.some(v => v != null && v > 0);

      const option = hasData ? {
        title: {
          text: '操作耗时对比',
          left: 20,
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        tooltip: tooltipWithUnit(' ms'),
        grid: { left: 60, right: 40, top: 60, bottom: 40 },
        xAxis: {
          type: 'category',
          data: categories,
          axisLine: { lineStyle: { color: COLORS.axisLine } },
          axisTick: { show: false },
          axisLabel: { color: COLORS.text, fontSize: 12 },
        },
        yAxis: {
          type: 'value',
          name: '耗时 (ms)',
          nameTextStyle: { color: COLORS.text, fontSize: 12 },
          axisLabel: { color: COLORS.text },
          splitLine: { lineStyle: { color: COLORS.axisLine, type: 'dashed' } },
        },
        series: [{
          type: 'bar',
          name: '耗时',
          data: values,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: function (params) {
              const colorList = ['#1677FF', '#13C2C2', '#40A9FF', '#36CFC9', '#0958D9'];
              return colorList[params.dataIndex] || '#1677FF';
            },
          },
          emphasis: {
            itemStyle: { color: '#0958D9' },
          },
          animationDuration: 800,
          animationEasing: 'cubicOut',
          barMaxWidth: 50,
        }],
      } : emptyPlaceholder('暂无性能测试数据');

      chart.setOption(option, true);
    },

    /**
     * 仪表盘图 — 显示吞吐量 TPS
     * @param {string} domId - 容器 DOM id
     * @param {number|object} data - TPS 数值 或 { tps, ... }
     */
    initThroughputChart(domId, data) {
      const chart = getChartInstance(domId);
      if (!chart) return;

      const tps = typeof data === 'number' ? data : (data && data.tps) ? data.tps : 0;
      const hasData = tps > 0;

      const option = hasData ? {
        title: {
          text: '系统吞吐量',
          left: 'center',
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        series: [{
          type: 'gauge',
          startAngle: 210,
          endAngle: -30,
          min: 0,
          max: Math.max(tps * 1.5, 100),
          center: ['50%', '58%'],
          radius: '88%',
          axisLine: {
            show: true,
            lineStyle: {
              width: 18,
              color: COLORS.gauge,
            },
          },
          pointer: {
            icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
            length: '65%',
            width: 6,
            offsetCenter: [0, '-10%'],
            itemStyle: { color: '#1677FF' },
          },
          axisTick: {
            distance: -18,
            length: 6,
            lineStyle: { color: '#BFBFBF', width: 1 },
          },
          splitLine: {
            distance: -20,
            length: 16,
            lineStyle: { color: '#BFBFBF', width: 2 },
          },
          axisLabel: {
            color: COLORS.text,
            distance: 25,
            fontSize: 11,
          },
          anchor: {
            show: true,
            showAbove: true,
            size: 18,
            itemStyle: { borderWidth: 1, borderColor: '#E8E8E8' },
          },
          title: {
            show: true,
            offsetCenter: [0, '82%'],
            fontSize: 13,
            color: COLORS.text,
          },
          detail: {
            valueAnimation: true,
            fontSize: 32,
            fontWeight: 'bold',
            offsetCenter: [0, '50%'],
            formatter: '{value} TPS',
            color: '#1677FF',
          },
          data: [{ value: tps, name: 'TPS' }],
          animationDuration: 1200,
          animationEasing: 'cubicInOut',
        }],
      } : {
        title: {
          text: '系统吞吐量',
          left: 'center',
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        graphic: {
          type: 'text',
          left: 'center',
          top: '55%',
          style: { text: '暂无数据', textAlign: 'center', fill: '#BFBFBF', fontSize: 13 },
        },
      };

      chart.setOption(option, true);
    },

    /**
     * 饼图 — 文件类型分布
     * @param {string} domId - 容器 DOM id
     * @param {object} data  - { typeDistribution: { "txt": 10, "pdf": 5, ... } }
     */
    initFileTypePie(domId, data) {
      const chart = getChartInstance(domId);
      if (!chart) return;

      let pieData = [];
      if (data && data.typeDistribution) {
        pieData = Object.entries(data.typeDistribution)
          .map(([name, value]) => ({ name, value }))
          .sort((a, b) => b.value - a.value);
      }

      const hasData = pieData.length > 0;

      const option = hasData ? {
        title: {
          text: '文件类型分布',
          left: 20,
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        tooltip: pieTooltip(),
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          textStyle: { color: COLORS.text, fontSize: 12 },
        },
        series: [{
          type: 'pie',
          radius: ['45%', '72%'],
          center: ['45%', '55%'],
          avoidLabelOverlap: false,
          padAngle: 2,
          itemStyle: {
            borderRadius: 4,
            borderColor: '#fff',
            borderWidth: 2,
          },
          label: {
            show: false,
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold',
            },
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.3)',
            },
          },
          labelLine: { show: false },
          data: pieData,
          color: COLORS.primary,
          animationType: 'scale',
          animationEasing: 'elasticOut',
          animationDuration: 1000,
        }],
      } : {
        title: {
          text: '文件类型分布',
          left: 20,
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        graphic: {
          type: 'text',
          left: 'center',
          top: '55%',
          style: { text: '暂无数据\n请先对目录执行分析', textAlign: 'center', fill: '#BFBFBF', fontSize: 13, lineHeight: 22 },
        },
      };

      chart.setOption(option, true);
    },

    /**
     * 横向条形图 — 各目录占用空间
     * @param {string} domId  - 容器 DOM id
     * @param {Array}  data   - [{ name, size }] 或包含此类结构的对象
     */
    initStorageBar(domId, data) {
      const chart = getChartInstance(domId);
      if (!chart) return;

      let barData = [];
      if (Array.isArray(data)) {
        barData = data.sort((a, b) => (b.size || 0) - (a.size || 0));
      } else if (data && Array.isArray(data.data)) {
        barData = data.data.sort((a, b) => (b.size || 0) - (a.size || 0));
      }

      const hasData = barData.length > 0;

      const option = hasData ? {
        title: {
          text: '目录空间占用',
          left: 20,
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        tooltip: tooltipWithUnit(' MB'),
        grid: { left: 140, right: 60, top: 50, bottom: 30 },
        xAxis: {
          type: 'value',
          name: '大小 (MB)',
          nameTextStyle: { color: COLORS.text, fontSize: 12 },
          axisLabel: { color: COLORS.text },
          splitLine: { lineStyle: { color: COLORS.axisLine, type: 'dashed' } },
        },
        yAxis: {
          type: 'category',
          data: barData.map(d => d.name || 'Unknown'),
          axisLine: { lineStyle: { color: COLORS.axisLine } },
          axisTick: { show: false },
          axisLabel: { color: COLORS.text, fontSize: 12 },
          inverse: true,
        },
        series: [{
          type: 'bar',
          name: '大小',
          data: barData.map(d => +(d.size || 0).toFixed(2)),
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: function (params) {
              const colorList = ['#1677FF', '#13C2C2', '#40A9FF', '#36CFC9', '#69C0FF', '#91D5FF'];
              return colorList[params.dataIndex % colorList.length];
            },
          },
          barMaxWidth: 28,
          animationDuration: 600,
          animationEasing: 'cubicOut',
          label: {
            show: true,
            position: 'right',
            fontSize: 11,
            color: COLORS.text,
            formatter: '{c} MB',
          },
        }],
      } : emptyPlaceholder('暂无存储数据');

      chart.setOption(option, true);
    },

    /**
     * 折线图 — 性能趋势（多次测试结果）
     * @param {string} domId - 容器 DOM id
     * @param {Array}  data  - [{ time, uploadTime, listTime, downloadTime, deleteTime }]
     */
    initTrendLine(domId, data) {
      const chart = getChartInstance(domId);
      if (!chart) return;

      const series = Array.isArray(data) && data.length > 0;

      const option = series ? {
        title: {
          text: '性能趋势',
          left: 20,
          top: 10,
          textStyle: { fontSize: 15, fontWeight: 600, color: '#262626' },
        },
        tooltip: tooltipWithUnit(' ms'),
        legend: {
          data: ['上传', '列表', '下载', '删除'],
          top: 8,
          right: 20,
          textStyle: { color: COLORS.text, fontSize: 12 },
        },
        grid: { left: 60, right: 40, top: 60, bottom: 40 },
        xAxis: {
          type: 'category',
          data: data.map((_, i) => `第${i + 1}次`),
          axisLine: { lineStyle: { color: COLORS.axisLine } },
          axisTick: { show: false },
          axisLabel: { color: COLORS.text },
          boundaryGap: false,
        },
        yAxis: {
          type: 'value',
          name: '耗时 (ms)',
          nameTextStyle: { color: COLORS.text, fontSize: 12 },
          axisLabel: { color: COLORS.text },
          splitLine: { lineStyle: { color: COLORS.axisLine, type: 'dashed' } },
        },
        series: [
          { name: '上传', type: 'line', data: data.map(d => d.uploadTime), smooth: true, lineStyle: { width: 2.5 }, itemStyle: { color: '#1677FF' }, symbolSize: 6 },
          { name: '列表', type: 'line', data: data.map(d => d.listTime), smooth: true, lineStyle: { width: 2.5 }, itemStyle: { color: '#13C2C2' }, symbolSize: 6 },
          { name: '下载', type: 'line', data: data.map(d => d.downloadTime), smooth: true, lineStyle: { width: 2.5 }, itemStyle: { color: '#40A9FF' }, symbolSize: 6 },
          { name: '删除', type: 'line', data: data.map(d => d.deleteTime), smooth: true, lineStyle: { width: 2.5 }, itemStyle: { color: '#36CFC9' }, symbolSize: 6 },
        ],
        animationDuration: 800,
        animationEasing: 'cubicOut',
      } : emptyPlaceholder('暂无趋势数据\n请多次运行性能测试');

      chart.setOption(option, true);
    },

    /**
     * 更新已有图表实例
     * @param {string} domId - 容器 DOM id
     * @param {object} option - ECharts option 对象 (或返回 option 的回调函数)
     */
    updateChart(domId, option) {
      const chart = chartInstances[domId] || getChartInstance(domId);
      if (!chart) return;
      const opt = typeof option === 'function' ? option() : option;
      chart.setOption(opt, true);
    },

    /** 销毁指定图表 */
    dispose(domId) {
      if (chartInstances[domId]) {
        chartInstances[domId].dispose();
        delete chartInstances[domId];
      }
    },

    /** 销毁所有图表 */
    disposeAll() {
      Object.keys(chartInstances).forEach(id => {
        chartInstances[id].dispose();
      });
      for (const key in chartInstances) delete chartInstances[key];
    },
  };

})();
