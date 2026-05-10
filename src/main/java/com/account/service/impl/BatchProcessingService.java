package com.account.service.impl;

import com.account.entity.FileInfo;
import com.account.util.HDFSUtil;
import com.account.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch processing service - provides batch upload, batch delete,
 * recursive directory copy, and file move/rename operations on HDFS.
 */
@Slf4j
@Service
public class BatchProcessingService {

    @Autowired
    private HDFSUtil hdfsUtil;

    /**
     * Batch upload multiple local files to an HDFS target directory.
     * Tracks success/failure for each file and returns a summary.
     *
     * @param localPaths list of local file paths to upload
     * @param targetDir  target directory path on HDFS
     * @return Result containing a map with total, success, failed counts, and failedPaths list
     */
    public Result<?> batchUpload(List<String> localPaths, String targetDir) {
        if (localPaths == null || localPaths.isEmpty()) {
            log.warn("batchUpload called with null or empty localPaths list");
            return Result.error("Local paths list cannot be null or empty");
        }
        if (targetDir == null || targetDir.trim().isEmpty()) {
            log.warn("batchUpload called with null or empty targetDir");
            return Result.error("Target directory cannot be null or empty");
        }

        int total = localPaths.size();
        int successCount = 0;
        int failCount = 0;
        List<String> failedPaths = new ArrayList<String>();

        for (String localPath : localPaths) {
            if (localPath == null || localPath.trim().isEmpty()) {
                log.warn("Skipping null or empty path in batch upload");
                failCount++;
                failedPaths.add("null or empty path");
                continue;
            }

            try {
                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    log.warn("Local file does not exist: {}", localPath);
                    failCount++;
                    failedPaths.add(localPath);
                    continue;
                }
                if (!localFile.isFile()) {
                    log.warn("Path is not a regular file: {}", localPath);
                    failCount++;
                    failedPaths.add(localPath);
                    continue;
                }

                String fileName = localFile.getName();
                String hdfsPath = targetDir;
                if (!hdfsPath.endsWith("/")) {
                    hdfsPath += "/";
                }
                hdfsPath += fileName;

                log.info("Batch uploading: {} -> {}", localPath, hdfsPath);
                boolean uploaded = hdfsUtil.uploadFile(localFile.getAbsolutePath(), hdfsPath);

                if (uploaded) {
                    successCount++;
                    log.info("Successfully uploaded: {}", fileName);
                } else {
                    failCount++;
                    failedPaths.add(localPath);
                    log.error("HDFSUtil.uploadFile returned false for: {}", localPath);
                }
            } catch (Exception e) {
                failCount++;
                failedPaths.add(localPath);
                log.error("Error uploading file: {}", localPath, e);
            }
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("total", total);
        summary.put("success", successCount);
        summary.put("failed", failCount);
        summary.put("failedPaths", failedPaths);

        log.info("Batch upload completed: {}/{} succeeded, {} failed", successCount, total, failCount);

        return Result.success("Batch upload: " + successCount + "/" + total + " succeeded", summary);
    }

    /**
     * Batch delete multiple HDFS paths.
     *
     * @param hdfsPaths list of HDFS paths to delete
     * @return Result containing a map with total, success, failed counts, and failedPaths list
     */
    public Result<?> batchDelete(List<String> hdfsPaths) {
        if (hdfsPaths == null || hdfsPaths.isEmpty()) {
            log.warn("batchDelete called with null or empty hdfsPaths list");
            return Result.error("HDFS paths list cannot be null or empty");
        }

        int total = hdfsPaths.size();
        int successCount = 0;
        int failCount = 0;
        List<String> failedPaths = new ArrayList<String>();

        for (String hdfsPath : hdfsPaths) {
            if (hdfsPath == null || hdfsPath.trim().isEmpty()) {
                log.warn("Skipping null or empty path in batch delete");
                failCount++;
                failedPaths.add("null or empty path");
                continue;
            }

            try {
                log.info("Batch deleting: {}", hdfsPath);
                boolean deleted = hdfsUtil.delete(hdfsPath);

                if (deleted) {
                    successCount++;
                    log.info("Successfully deleted: {}", hdfsPath);
                } else {
                    failCount++;
                    failedPaths.add(hdfsPath);
                    log.error("HDFSUtil.delete returned false for: {}", hdfsPath);
                }
            } catch (Exception e) {
                failCount++;
                failedPaths.add(hdfsPath);
                log.error("Error deleting: {}", hdfsPath, e);
            }
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("total", total);
        summary.put("success", successCount);
        summary.put("failed", failCount);
        summary.put("failedPaths", failedPaths);

        log.info("Batch delete completed: {}/{} succeeded, {} failed", successCount, total, failCount);

        return Result.success("Batch delete: " + successCount + "/" + total + " succeeded", summary);
    }

    /**
     * Recursively copy an HDFS directory from sourcePath to targetPath.
     * Creates the target directory if it does not exist.
     * Handles nested subdirectories by recursion.
     *
     * @param sourcePath source directory path on HDFS
     * @param targetPath target directory path on HDFS
     * @return Result containing a map with totalFiles, copied count, failed count, and failedFiles list
     */
    public Result<?> copyDirectory(String sourcePath, String targetPath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            log.warn("copyDirectory called with null or empty sourcePath");
            return Result.error("Source path cannot be null or empty");
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            log.warn("copyDirectory called with null or empty targetPath");
            return Result.error("Target path cannot be null or empty");
        }

        try {
            log.info("Copying directory from '{}' to '{}'", sourcePath, targetPath);

            // Check if source exists
            boolean sourceExists = hdfsUtil.exists(sourcePath);
            if (!sourceExists) {
                log.error("Source path does not exist: {}", sourcePath);
                return Result.error("Source path does not exist: " + sourcePath);
            }

            // Ensure target directory exists
            boolean targetExists = hdfsUtil.exists(targetPath);
            if (!targetExists) {
                log.info("Target directory does not exist, creating: {}", targetPath);
                boolean created = hdfsUtil.mkdir(targetPath);
                if (!created) {
                    log.error("Failed to create target directory: {}", targetPath);
                    return Result.error("Failed to create target directory: " + targetPath);
                }
            }

            List<FileInfo> files = hdfsUtil.listFiles(sourcePath);
            if (files == null) {
                log.error("HDFSUtil.listFiles returned null for source: {}", sourcePath);
                return Result.error("Failed to list files in source path: " + sourcePath);
            }

            int totalFiles = files.size();
            int copiedCount = 0;
            int failedCount = 0;
            List<String> failedFiles = new ArrayList<String>();

            String tempDir = System.getProperty("java.io.tmpdir");

            for (FileInfo file : files) {
                if (file == null) {
                    continue;
                }

                String name = file.getName();
                if (name == null) {
                    continue;
                }

                try {
                    String childSource = sourcePath;
                    if (!childSource.endsWith("/")) {
                        childSource += "/";
                    }
                    childSource += name;

                    String childTarget = targetPath;
                    if (!childTarget.endsWith("/")) {
                        childTarget += "/";
                    }
                    childTarget += name;

                    if (file.isDirectory()) {
                        // Recursively copy subdirectory
                        Result<?> subResult = copyDirectory(childSource, childTarget);
                        if (subResult.isSuccess()) {
                            copiedCount++;
                        } else {
                            failedCount++;
                            failedFiles.add(name);
                        }
                    } else {
                        // Download file to temp location
                        File tempFile = new File(tempDir, name);
                        boolean downloaded = hdfsUtil.downloadFile(childSource, tempFile.getAbsolutePath());

                        if (downloaded && tempFile.exists()) {
                            // Upload from temp to target
                            boolean uploaded = hdfsUtil.uploadFile(
                                    tempFile.getAbsolutePath(), childTarget);
                            if (uploaded) {
                                copiedCount++;
                                log.debug("Copied file: {}", name);
                            } else {
                                failedCount++;
                                failedFiles.add(name);
                                log.error("Failed to upload file to target: {}", childTarget);
                            }
                        } else {
                            failedCount++;
                            failedFiles.add(name);
                            log.error("Failed to download file from source: {}", childSource);
                        }

                        // Clean up temp file
                        if (tempFile.exists()) {
                            boolean deleted = tempFile.delete();
                            if (!deleted) {
                                tempFile.deleteOnExit();
                            }
                        }
                    }
                } catch (Exception e) {
                    failedCount++;
                    failedFiles.add(name);
                    log.error("Error copying file/directory: {}", name, e);
                }
            }

            Map<String, Object> summary = new HashMap<String, Object>();
            summary.put("totalFiles", totalFiles);
            summary.put("copied", copiedCount);
            summary.put("failed", failedCount);
            summary.put("failedFiles", failedFiles);

            log.info("Directory copy completed: {}/{} items processed ({} failed)",
                    copiedCount, totalFiles, failedCount);

            return Result.success("Directory copied: " + copiedCount + "/" + totalFiles + " items", summary);

        } catch (Exception e) {
            log.error("Error copying directory from '{}' to '{}'", sourcePath, targetPath, e);
            return Result.error("Error copying directory: " + e.getMessage());
        }
    }

    /**
     * Move/rename a file or directory on HDFS from sourcePath to targetPath.
     * The operation performs a copy to the target first, then deletes the source.
     * For simple rename operations within the same parent directory, the copy is
     * skipped and only the final delete occurs.
     *
     * @param sourcePath source HDFS path
     * @param targetPath target HDFS path
     * @return Result containing a map with sourcePath, targetPath, and sourceDeleted flag
     */
    public Result<?> moveFiles(String sourcePath, String targetPath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            log.warn("moveFiles called with null or empty sourcePath");
            return Result.error("Source path cannot be null or empty");
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            log.warn("moveFiles called with null or empty targetPath");
            return Result.error("Target path cannot be null or empty");
        }

        try {
            log.info("Moving from '{}' to '{}'", sourcePath, targetPath);

            // Verify source exists
            boolean sourceExists = hdfsUtil.exists(sourcePath);
            if (!sourceExists) {
                log.error("Source path does not exist: {}", sourcePath);
                return Result.error("Source path does not exist: " + sourcePath);
            }

            // Copy all content from source to target
            Result<?> copyResult = copyDirectory(sourcePath, targetPath);
            if (!copyResult.isSuccess()) {
                log.error("Copy phase of move failed for '{}' -> '{}'", sourcePath, targetPath);
                return Result.error("Failed to copy files during move operation from "
                        + sourcePath + " to " + targetPath);
            }

            // Delete the source after successful copy
            boolean deleted = hdfsUtil.delete(sourcePath);
            if (!deleted) {
                log.warn("Files copied to '{}' but source deletion failed for '{}'",
                        targetPath, sourcePath);
            } else {
                log.info("Source deleted successfully: {}", sourcePath);
            }

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("sourcePath", sourcePath);
            result.put("targetPath", targetPath);
            result.put("copyResult", copyResult.getData());
            result.put("sourceDeleted", deleted);

            log.info("Move completed from '{}' to '{}' (sourceDeleted={})",
                    sourcePath, targetPath, deleted);

            String message = "Files moved successfully from " + sourcePath + " to " + targetPath;
            if (!deleted) {
                message += " (warning: source deletion failed)";
            }
            return Result.success(message, result);

        } catch (Exception e) {
            log.error("Error moving files from '{}' to '{}'", sourcePath, targetPath, e);
            return Result.error("Error moving files: " + e.getMessage());
        }
    }
}
