/**
 * api.js — HDFS & Performance API 服务层
 * 纯 JS 命名空间模式，封装所有后端 REST API 调用
 * 依赖：无（纯 fetch + async/await）
 */
(function () {
  'use strict';

  // ==================== 内部工具 ====================

  /** 显示 loading 遮罩 */
  function showLoading() {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) overlay.style.display = 'flex';
  }

  /** 隐藏 loading 遮罩 */
  function hideLoading() {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) overlay.style.display = 'none';
  }

  /**
   * 简易 toast 消息（基础实现，可被 app.js 增强版本覆盖）
   * @param {string} message - 消息文本
   * @param {'success'|'error'|'warning'|'info'} type - 消息类型
   */
  function toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.textContent = message;
    container.appendChild(el); // CSS @keyframes toastIn 自动播放入场动画

    // 自动移除
    setTimeout(() => {
      if (el.parentNode) el.remove();
    }, 3500);
  }

  // ==================== HdfsApi 命名空间 ====================

  window.HdfsApi = {

    /**
     * 通用请求封装
     * @param {string} url - 请求地址
     * @param {object} options - fetch 选项
     * @returns {Promise<any>} 解析后的响应 data 字段
     */
    async _request(url, options = {}) {
      showLoading();
      try {
        const response = await fetch(url, options);
        // HTTP 状态异常
        if (!response.ok) {
          const text = await response.text().catch(() => '');
          throw new Error(`HTTP ${response.status}${text ? ': ' + text : ''}`);
        }
        const result = await response.json();
        // 业务码异常
        if (result.code !== 200) {
          throw new Error(result.message || '请求失败，请稍后重试');
        }
        return result.data;
      } catch (err) {
        // 网络异常
        if (err.name === 'TypeError' && err.message === 'Failed to fetch') {
          toast('网络连接失败，请检查网络后重试', 'error');
        } else {
          toast(err.message || '未知错误', 'error');
        }
        throw err;
      } finally {
        hideLoading();
      }
    },

    // ---------- HDFS 文件操作 ----------

    /** 列出目录文件 */
    async listFiles(path = '/') {
      return this._request(
        `/api/hdfs/list?path=${encodeURIComponent(path)}`
      );
    },

    /** 上传单个文件 */
    async uploadFile(file, path = '/') {
      if (!file) throw new Error('请选择要上传的文件');
      const formData = new FormData();
      formData.append('file', file);
      formData.append('path', path);
      return this._request('/api/hdfs/upload', {
        method: 'POST',
        body: formData,
      });
    },

    /** 下载文件（触发浏览器下载） */
    async downloadFile(path) {
      if (!path) throw new Error('请指定要下载的文件路径');
      showLoading();
      try {
        const response = await fetch(
          `/api/hdfs/download?path=${encodeURIComponent(path)}`
        );
        if (!response.ok) {
          throw new Error(`下载失败 (HTTP ${response.status})`);
        }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = path.split('/').pop() || 'download';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        toast('文件下载已开始', 'success');
      } catch (err) {
        toast(err.message || '下载失败', 'error');
        throw err;
      } finally {
        hideLoading();
      }
    },

    /** 删除文件或目录 */
    async deleteFile(path) {
      if (!path) throw new Error('请指定要删除的路径');
      return this._request(
        `/api/hdfs/delete?path=${encodeURIComponent(path)}`,
        { method: 'DELETE' }
      );
    },

    /** 创建目录 */
    async createDir(path) {
      if (!path) throw new Error('请指定目录路径');
      return this._request(
        `/api/hdfs/mkdir?path=${encodeURIComponent(path)}`,
        { method: 'POST' }
      );
    },

    /** 获取目录统计信息 */
    async getStats(path = '/') {
      return this._request(
        `/api/hdfs/stats?path=${encodeURIComponent(path)}`
      );
    },

    /** 批量上传文件 */
    async batchUpload(files, path = '/') {
      if (!files || files.length === 0) throw new Error('请选择要上传的文件');
      const formData = new FormData();
      for (const file of files) {
        formData.append('files', file);
      }
      formData.append('path', path);
      return this._request('/api/hdfs/batch-upload', {
        method: 'POST',
        body: formData,
      });
    },

    /** 分析目录结构与文件类型分布 */
    async analyzeDir(path = '/') {
      return this._request(
        `/api/hdfs/analyze?path=${encodeURIComponent(path)}`
      );
    },

    /** 按关键字搜索文件 */
    async searchFiles(path = '/', keyword = '') {
      if (!keyword) throw new Error('请输入搜索关键字');
      return this._request(
        `/api/hdfs/search?path=${encodeURIComponent(path)}&keyword=${encodeURIComponent(keyword)}`
      );
    },

    /** 列出超过指定大小的文件 */
    async getLargeFiles(path = '/', minSize = 1048576) {
      return this._request(
        `/api/hdfs/large-files?path=${encodeURIComponent(path)}&minSize=${encodeURIComponent(minSize)}`
      );
    },

    // ---------- 性能测试 ----------

    /** 运行单次性能测试 */
    async runPerfTest() {
      return this._request('/api/performance/test');
    },

    /** 获取性能汇总报告 */
    async getPerfReport() {
      return this._request('/api/performance/report');
    },
  };

})();
