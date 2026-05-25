package com.account.service;

import com.account.dto.HdfsDataPreview;
import com.account.dto.HdfsDirectoryAnalysis;
import com.account.dto.HdfsFileInfo;
import com.account.dto.HdfsPerformanceResult;
import com.account.dto.HdfsSizeStatistics;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface HadoopDemoService {

    List<HdfsFileInfo> list(String path, boolean recursive) throws Exception;

    boolean mkdir(String path) throws Exception;

    String upload(String path, MultipartFile file, boolean overwrite) throws Exception;

    void download(String path, HttpServletResponse response) throws Exception;

    boolean delete(String path, boolean recursive) throws Exception;

    HdfsPerformanceResult runPerformanceDemo(int files, long records, int payloadSize, boolean clean) throws Exception;

    /**
     * 文件大小统计：总大小、文件数、目录数、Top N 大文件
     */
    HdfsSizeStatistics getSizeStatistics(String path, int topN) throws Exception;

    /**
     * 目录分析：文件/目录数、大小分布、文件类型分布、最大文件
     */
    HdfsDirectoryAnalysis analyzeDirectory(String path) throws Exception;

    /**
     * 数据读取解析：预览文件前 N 行，解析 CSV 表头
     */
    HdfsDataPreview previewData(String path, int previewLines) throws Exception;

    /**
     * 批量删除：删除指定路径列表
     */
    int batchDelete(List<String> paths, boolean recursive) throws Exception;
}
