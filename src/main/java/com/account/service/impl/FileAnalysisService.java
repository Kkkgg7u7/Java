package com.account.service.impl;

import com.account.entity.FileInfo;
import com.account.util.HDFSUtil;
import com.account.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File analysis service - provides deep directory analysis, file search,
 * and large-file detection capabilities on top of HDFS.
 */
@Slf4j
@Service
public class FileAnalysisService {

    @Autowired
    private HDFSUtil hdfsUtil;

    /**
     * Perform a deep analysis of the specified HDFS directory.
     * Computes total size, file count, largest/smallest file, average size,
     * and file type distribution.
     *
     * @param path HDFS directory path to analyze
     * @return Result containing a map with analysis metrics
     */
    public Result<?> analyzeDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("analyzeDirectory called with null or empty path");
            return Result.error("Path cannot be null or empty");
        }
        try {
            List<FileInfo> files = hdfsUtil.listFiles(path);
            if (files == null) {
                log.error("HDFSUtil.listFiles returned null for path: {}", path);
                return Result.error("Failed to list files in path: " + path);
            }
            if (files.isEmpty()) {
                log.info("No files found in path: {}", path);
                Map<String, Object> emptyAnalysis = new HashMap<String, Object>();
                emptyAnalysis.put("path", path);
                emptyAnalysis.put("totalSize", 0L);
                emptyAnalysis.put("fileCount", 0);
                emptyAnalysis.put("dirCount", 0);
                emptyAnalysis.put("largestFile", null);
                emptyAnalysis.put("largestFileSize", 0L);
                emptyAnalysis.put("smallestFile", null);
                emptyAnalysis.put("smallestFileSize", 0L);
                emptyAnalysis.put("averageFileSize", 0.0);
                emptyAnalysis.put("fileTypeDistribution", new HashMap<String, Integer>());
                return Result.success("Directory is empty", emptyAnalysis);
            }

            long totalSize = 0L;
            int fileCount = 0;
            int dirCount = 0;
            String largestFileName = null;
            long largestFileSize = Long.MIN_VALUE;
            String smallestFileName = null;
            long smallestFileSize = Long.MAX_VALUE;
            Map<String, Integer> fileTypeDistribution = new HashMap<String, Integer>();

            for (FileInfo file : files) {
                if (file == null) {
                    continue;
                }

                if (file.isDirectory()) {
                    dirCount++;
                    continue;
                }

                fileCount++;
                long fileSize = file.getSize();
                totalSize += fileSize;

                // Track largest file
                if (fileSize > largestFileSize) {
                    largestFileSize = fileSize;
                    largestFileName = file.getName();
                }

                // Track smallest file
                if (fileSize < smallestFileSize) {
                    smallestFileSize = fileSize;
                    smallestFileName = file.getName();
                }

                // Build file type distribution
                String fileName = file.getName();
                String extension = "unknown";
                if (fileName != null && fileName.contains(".")) {
                    extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    if (extension.isEmpty()) {
                        extension = "unknown";
                    }
                }
                Integer count = fileTypeDistribution.get(extension);
                if (count == null) {
                    fileTypeDistribution.put(extension, 1);
                } else {
                    fileTypeDistribution.put(extension, count + 1);
                }
            }

            // Fix edge case where no regular files were found
            if (fileCount == 0) {
                largestFileName = null;
                largestFileSize = 0L;
                smallestFileName = null;
                smallestFileSize = 0L;
            }

            double averageFileSize = fileCount > 0 ? (double) totalSize / fileCount : 0.0;
            double roundedAvg = Math.round(averageFileSize * 100.0) / 100.0;

            Map<String, Object> analysis = new HashMap<String, Object>();
            analysis.put("path", path);
            analysis.put("totalSize", totalSize);
            analysis.put("fileCount", fileCount);
            analysis.put("dirCount", dirCount);
            analysis.put("largestFile", largestFileName);
            analysis.put("largestFileSize", largestFileSize);
            analysis.put("smallestFile", smallestFileName);
            analysis.put("smallestFileSize", smallestFileSize);
            analysis.put("averageFileSize", roundedAvg);
            analysis.put("fileTypeDistribution", fileTypeDistribution);

            log.info("Directory analysis completed for path: {} - {} files, {} dirs, total size: {}",
                    path, fileCount, dirCount, totalSize);

            return Result.success("Directory analysis completed", analysis);

        } catch (Exception e) {
            log.error("Error analyzing directory: {}", path, e);
            return Result.error("Error analyzing directory: " + e.getMessage());
        }
    }

    /**
     * Search for files in an HDFS directory whose names match a keyword.
     *
     * @param path    HDFS directory path to search
     * @param keyword keyword to match against file names (case-sensitive contains)
     * @return Result containing list of matching FileInfo objects
     */
    public Result<?> searchFiles(String path, String keyword) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("searchFiles called with null or empty path");
            return Result.error("Path cannot be null or empty");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("searchFiles called with null or empty keyword");
            return Result.error("Search keyword cannot be null or empty");
        }
        try {
            List<FileInfo> allFiles = hdfsUtil.listFiles(path);
            if (allFiles == null) {
                log.error("HDFSUtil.listFiles returned null for path: {}", path);
                return Result.error("Failed to list files in path: " + path);
            }
            if (allFiles.isEmpty()) {
                return Result.success("No files found in path", new ArrayList<FileInfo>());
            }

            List<FileInfo> matchedFiles = new ArrayList<FileInfo>();
            for (FileInfo file : allFiles) {
                if (file != null && file.getName() != null && file.getName().contains(keyword)) {
                    matchedFiles.add(file);
                }
            }

            log.info("Search completed for keyword '{}' in path '{}': {} matches",
                    keyword, path, matchedFiles.size());

            return Result.success("Found " + matchedFiles.size() + " matching files", matchedFiles);

        } catch (Exception e) {
            log.error("Error searching files in path: {}", path, e);
            return Result.error("Error searching files: " + e.getMessage());
        }
    }

    /**
     * Find files in an HDFS directory larger than a specified size threshold.
     * Directories are excluded from the results.
     *
     * @param path    HDFS directory path to search
     * @param minSize minimum file size in bytes (inclusive)
     * @return Result containing list of matching FileInfo objects
     */
    public Result<?> getLargeFiles(String path, long minSize) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("getLargeFiles called with null or empty path");
            return Result.error("Path cannot be null or empty");
        }
        if (minSize < 0) {
            log.warn("getLargeFiles called with negative minSize: {}", minSize);
            return Result.error("Minimum size cannot be negative");
        }
        try {
            List<FileInfo> allFiles = hdfsUtil.listFiles(path);
            if (allFiles == null) {
                log.error("HDFSUtil.listFiles returned null for path: {}", path);
                return Result.error("Failed to list files in path: " + path);
            }
            if (allFiles.isEmpty()) {
                return Result.success("No files found in path", new ArrayList<FileInfo>());
            }

            List<FileInfo> largeFiles = new ArrayList<FileInfo>();
            for (FileInfo file : allFiles) {
                if (file != null && !file.isDirectory() && file.getSize() >= minSize) {
                    largeFiles.add(file);
                }
            }

            log.info("Large files search completed for path '{}' (minSize={}): {} files",
                    path, minSize, largeFiles.size());

            return Result.success("Found " + largeFiles.size() + " large files", largeFiles);

        } catch (Exception e) {
            log.error("Error searching large files in path: {}", path, e);
            return Result.error("Error searching large files: " + e.getMessage());
        }
    }
}
