package com.account.controller;

import com.account.service.HdfsService;
import com.account.service.impl.BatchProcessingService;
import com.account.service.impl.FileAnalysisService;
import com.account.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * HDFS Controller - RESTful API for HDFS file operations, file analysis, and batch processing.
 *
 * @author Big Data Demo Team - Member 3
 */
@Slf4j
@RestController
@RequestMapping("/api/hdfs")
public class HdfsController {

    @Autowired
    private HdfsService hdfsService;

    @Autowired
    private FileAnalysisService fileAnalysisService;

    @Autowired
    private BatchProcessingService batchProcessingService;

    /**
     * List files in a directory.
     */
    @GetMapping("/list")
    public Result listFiles(@RequestParam(defaultValue = "/") String path) {
        log.info("Listing files for path: {}", path);
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        return hdfsService.listFiles(path);
    }

    /**
     * Upload a single file to HDFS.
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(defaultValue = "/") String path) {
        log.info("Uploading file: {} to path: {}", file.getOriginalFilename(), path);
        if (file == null || file.isEmpty()) {
            return Result.error(400, "Upload file cannot be empty");
        }
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Target path cannot be empty");
        }
        return hdfsService.upload(file, path);
    }

    /**
     * Download a file from HDFS.
     */
    @GetMapping("/download")
    public void download(@RequestParam("path") String hdfsPath,
                         HttpServletResponse response) {
        log.info("Downloading file: {}", hdfsPath);
        if (hdfsPath == null || hdfsPath.trim().isEmpty()) {
            log.warn("Download path is empty");
            response.setStatus(400);
            return;
        }
        hdfsService.download(hdfsPath, response);
    }

    /**
     * Delete a file or directory from HDFS.
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam("path") String hdfsPath) {
        log.info("Deleting path: {}", hdfsPath);
        if (hdfsPath == null || hdfsPath.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        return hdfsService.delete(hdfsPath);
    }

    /**
     * Create a directory in HDFS.
     */
    @PostMapping("/mkdir")
    public Result mkdir(@RequestParam("path") String path) {
        log.info("Creating directory: {}", path);
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        return hdfsService.mkdir(path);
    }

    /**
     * Get directory statistics from HDFS.
     */
    @GetMapping("/stats")
    public Result getStats(@RequestParam(defaultValue = "/") String path) {
        log.info("Getting stats for path: {}", path);
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        return hdfsService.getStats(path);
    }

    /**
     * Batch upload multiple files to HDFS.
     */
    @PostMapping("/batch-upload")
    public Result batchUpload(@RequestParam("files") MultipartFile[] files,
                              @RequestParam(defaultValue = "/") String path) {
        log.info("Batch uploading {} files to path: {}", files != null ? files.length : 0, path);
        if (files == null || files.length == 0) {
            return Result.error(400, "Upload files cannot be empty");
        }
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Target path cannot be empty");
        }
        return hdfsService.batchUpload(files, path);
    }

    /**
     * Analyze a directory deeply (file types, size distribution, etc.).
     */
    @GetMapping("/analyze")
    public Result analyzeDirectory(@RequestParam(defaultValue = "/") String path) {
        log.info("Analyzing directory: {}", path);
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        return fileAnalysisService.analyzeDirectory(path);
    }

    /**
     * Search files by keyword in a directory.
     */
    @GetMapping("/search")
    public Result searchFiles(@RequestParam(defaultValue = "/") String path,
                              @RequestParam("keyword") String keyword) {
        log.info("Searching for '{}' in path: {}", keyword, path);
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error(400, "Keyword parameter cannot be empty");
        }
        return fileAnalysisService.searchFiles(path, keyword);
    }

    /**
     * Find large files exceeding a minimum size in a directory.
     */
    @GetMapping("/large-files")
    public Result getLargeFiles(@RequestParam(defaultValue = "/") String path,
                                @RequestParam(defaultValue = "1048576") long minSize) {
        log.info("Finding large files in path: {} with minSize: {}", path, minSize);
        if (path == null || path.trim().isEmpty()) {
            return Result.error(400, "Path parameter cannot be empty");
        }
        if (minSize <= 0) {
            return Result.error(400, "minSize must be a positive number");
        }
        return fileAnalysisService.getLargeFiles(path, minSize);
    }
}
