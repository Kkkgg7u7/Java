package com.account.service;

import com.account.util.Result;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * HDFS service interface - wraps HDFS file operations.
 */
public interface HdfsService {

    /**
     * List files in the given HDFS path.
     *
     * @param path HDFS directory path
     * @return Result containing list of FileInfo
     */
    Result<?> listFiles(String path);

    /**
     * Upload a single file to HDFS.
     *
     * @param file       the multipart file to upload
     * @param targetPath target directory path on HDFS
     * @return Result indicating success or failure
     */
    Result<?> upload(MultipartFile file, String targetPath);

    /**
     * Download a file from HDFS and write it to the HTTP response.
     *
     * @param hdfsPath HDFS file path to download
     * @param response HTTP servlet response to write the file to
     * @return Result indicating success or failure
     */
    Result<?> download(String hdfsPath, HttpServletResponse response);

    /**
     * Delete a file or directory on HDFS.
     *
     * @param hdfsPath HDFS path to delete
     * @return Result indicating success or failure
     */
    Result<?> delete(String hdfsPath);

    /**
     * Create a directory on HDFS.
     *
     * @param path HDFS directory path to create
     * @return Result indicating success or failure
     */
    Result<?> mkdir(String path);

    /**
     * Get file statistics for a given HDFS path.
     *
     * @param path HDFS path
     * @return Result containing FileStats
     */
    Result<?> getStats(String path);

    /**
     * Batch upload multiple files to HDFS.
     *
     * @param files      array of multipart files
     * @param targetPath target directory path on HDFS
     * @return Result containing upload summary (total, success, failed, failedFiles)
     */
    Result<?> batchUpload(MultipartFile[] files, String targetPath);
}
