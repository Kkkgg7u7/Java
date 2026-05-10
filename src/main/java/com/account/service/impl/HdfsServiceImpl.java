package com.account.service.impl;

import com.account.entity.FileInfo;
import com.account.entity.FileStats;
import com.account.service.HdfsService;
import com.account.util.HDFSUtil;
import com.account.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HDFS service implementation - wraps HDFSUtil operations with error handling,
 * logging, and HTTP integration for file upload/download.
 */
@Slf4j
@Service
public class HdfsServiceImpl implements HdfsService {

    @Autowired
    private HDFSUtil hdfsUtil;

    private static final int BUFFER_SIZE = 4096;

    @Override
    public Result<?> listFiles(String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("listFiles called with null or empty path");
            return Result.error("HDFS path cannot be null or empty");
        }
        try {
            List<FileInfo> files = hdfsUtil.listFiles(path);
            if (files == null) {
                log.error("HDFSUtil.listFiles returned null for path: {}", path);
                return Result.error("Failed to list files in path: " + path);
            }
            log.info("Listed {} files from path: {}", files.size(), path);
            return Result.success("File list retrieved successfully", files);
        } catch (Exception e) {
            log.error("Error listing files from path: {}", path, e);
            return Result.error("Error listing files: " + e.getMessage());
        }
    }

    @Override
    public Result<?> upload(MultipartFile file, String targetPath) {
        if (file == null || file.isEmpty()) {
            log.warn("upload called with null or empty file");
            return Result.error("Upload file cannot be null or empty");
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            log.warn("upload called with null or empty targetPath");
            return Result.error("Target path cannot be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            log.warn("upload called with file that has no original filename");
            return Result.error("File name cannot be null or empty");
        }

        File tempFile = null;
        try {
            // Save multipart file to a temporary local file
            String tempDir = System.getProperty("java.io.tmpdir");
            tempFile = new File(tempDir, originalFilename);
            file.transferTo(tempFile);
            log.debug("Saved upload to temp file: {}", tempFile.getAbsolutePath());

            // Build HDFS target path
            String hdfsPath = targetPath;
            if (!hdfsPath.endsWith("/")) {
                hdfsPath += "/";
            }
            hdfsPath += originalFilename;

            log.info("Uploading file '{}' to HDFS path: {}", originalFilename, hdfsPath);
            boolean uploaded = hdfsUtil.uploadFile(tempFile.getAbsolutePath(), hdfsPath);

            if (uploaded) {
                log.info("Successfully uploaded file: {}", originalFilename);
                return Result.success("File uploaded successfully: " + originalFilename);
            } else {
                log.error("HDFSUtil.uploadFile returned false for file: {}", originalFilename);
                return Result.error("Failed to upload file: " + originalFilename);
            }
        } catch (Exception e) {
            log.error("Error uploading file: {}", originalFilename, e);
            return Result.error("Error uploading file: " + e.getMessage());
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    @Override
    public Result<?> download(String hdfsPath, HttpServletResponse response) {
        if (hdfsPath == null || hdfsPath.trim().isEmpty()) {
            log.warn("download called with null or empty hdfsPath");
            return Result.error("HDFS path cannot be null or empty");
        }
        if (response == null) {
            log.warn("download called with null HttpServletResponse");
            return Result.error("HttpServletResponse cannot be null");
        }

        String fileName = hdfsPath.substring(hdfsPath.lastIndexOf("/") + 1);
        if (fileName.isEmpty()) {
            log.warn("Could not extract file name from path: {}", hdfsPath);
            return Result.error("Invalid HDFS path, cannot extract file name: " + hdfsPath);
        }

        File tempFile = null;
        FileInputStream fis = null;
        OutputStream os = null;

        try {
            // Download from HDFS to a temporary local file
            String tempDir = System.getProperty("java.io.tmpdir");
            tempFile = new File(tempDir, fileName);

            log.info("Downloading file from HDFS: {} to temp: {}", hdfsPath, tempFile.getAbsolutePath());
            boolean downloaded = hdfsUtil.downloadFile(hdfsPath, tempFile.getAbsolutePath());

            if (!downloaded || !tempFile.exists()) {
                log.error("HDFSUtil.downloadFile failed for path: {}", hdfsPath);
                return Result.error("Failed to download file: " + fileName);
            }

            // Set HTTP response headers for file download
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
            response.setContentLength((int) tempFile.length());

            // Write file contents to the response output stream
            fis = new FileInputStream(tempFile);
            os = response.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            log.info("Successfully downloaded and streamed file: {}", fileName);
            return Result.success("File downloaded successfully: " + fileName);

        } catch (Exception e) {
            log.error("Error downloading file from HDFS: {}", hdfsPath, e);
            return Result.error("Error downloading file: " + e.getMessage());
        } finally {
            // Clean up streams
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.warn("Failed to close FileInputStream", e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.warn("Failed to close OutputStream", e);
                }
            }
            // Clean up temp file
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    @Override
    public Result<?> delete(String hdfsPath) {
        if (hdfsPath == null || hdfsPath.trim().isEmpty()) {
            log.warn("delete called with null or empty hdfsPath");
            return Result.error("HDFS path cannot be null or empty");
        }
        try {
            log.info("Deleting HDFS path: {}", hdfsPath);
            boolean deleted = hdfsUtil.delete(hdfsPath);
            if (deleted) {
                log.info("Successfully deleted HDFS path: {}", hdfsPath);
                return Result.success("Deleted successfully: " + hdfsPath);
            } else {
                log.error("HDFSUtil.delete returned false for path: {}", hdfsPath);
                return Result.error("Failed to delete: " + hdfsPath);
            }
        } catch (Exception e) {
            log.error("Error deleting HDFS path: {}", hdfsPath, e);
            return Result.error("Error deleting: " + e.getMessage());
        }
    }

    @Override
    public Result<?> mkdir(String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("mkdir called with null or empty path");
            return Result.error("Directory path cannot be null or empty");
        }
        try {
            log.info("Creating directory on HDFS: {}", path);
            boolean created = hdfsUtil.mkdir(path);
            if (created) {
                log.info("Successfully created directory: {}", path);
                return Result.success("Directory created successfully: " + path);
            } else {
                log.error("HDFSUtil.mkdir returned false for path: {}", path);
                return Result.error("Failed to create directory: " + path);
            }
        } catch (Exception e) {
            log.error("Error creating directory: {}", path, e);
            return Result.error("Error creating directory: " + e.getMessage());
        }
    }

    @Override
    public Result<?> getStats(String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("getStats called with null or empty path");
            return Result.error("Path cannot be null or empty");
        }
        try {
            log.info("Getting stats for path: {}", path);
            FileStats stats = hdfsUtil.getStats(path);
            if (stats == null) {
                log.error("HDFSUtil.getStats returned null for path: {}", path);
                return Result.error("Failed to get stats for path: " + path);
            }
            log.info("Retrieved stats for path: {} - {} files, {} dirs, total size: {}",
                    path, stats.getFileCount(), stats.getDirCount(), stats.getTotalSize());
            return Result.success("Stats retrieved successfully", stats);
        } catch (Exception e) {
            log.error("Error getting stats for path: {}", path, e);
            return Result.error("Error getting stats: " + e.getMessage());
        }
    }

    @Override
    public Result<?> batchUpload(MultipartFile[] files, String targetPath) {
        if (files == null || files.length == 0) {
            log.warn("batchUpload called with null or empty files array");
            return Result.error("Files array cannot be null or empty");
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            log.warn("batchUpload called with null or empty targetPath");
            return Result.error("Target path cannot be null or empty");
        }

        int total = files.length;
        int successCount = 0;
        int failCount = 0;
        List<String> failedFiles = new ArrayList<String>();

        for (MultipartFile file : files) {
            Result<?> result = upload(file, targetPath);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
                String fname = file.getOriginalFilename();
                failedFiles.add(fname != null ? fname : "unknown");
            }
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("total", total);
        summary.put("success", successCount);
        summary.put("failed", failCount);
        summary.put("failedFiles", failedFiles);

        log.info("Batch upload completed: {}/{} succeeded, {} failed", successCount, total, failCount);

        return Result.success("Batch upload completed: " + successCount + "/" + total + " succeeded", summary);
    }
}
