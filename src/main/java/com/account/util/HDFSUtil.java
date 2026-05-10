package com.account.util;

import com.account.entity.FileInfo;
import com.account.entity.FileStats;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HDFSUtil {

    @Autowired
    private FileSystem fileSystem;

    /**
     * Check if the FileSystem bean is available (HDFS connected).
     */
    private boolean isAvailable() {
        if (fileSystem == null) {
            log.error("HDFS FileSystem is not initialized - HDFS may be unreachable");
            return false;
        }
        return true;
    }

    /**
     * List files and directories at the given HDFS path.
     *
     * @param hdfsPath the HDFS path to list
     * @return list of FileInfo objects, empty list if path does not exist or is empty
     */
    public List<FileInfo> listFiles(String hdfsPath) {
        List<FileInfo> result = new ArrayList<FileInfo>();
        if (!isAvailable()) return result;
        try {
            Path path = new Path(hdfsPath);
            if (!fileSystem.exists(path)) {
                log.warn("Path does not exist: {}", hdfsPath);
                return result;
            }
            FileStatus[] statuses = fileSystem.listStatus(path);
            if (statuses == null || statuses.length == 0) {
                return result;
            }
            for (FileStatus status : statuses) {
                FileInfo info = new FileInfo();
                info.setName(status.getPath().getName());
                info.setPath(status.getPath().toString());
                info.setDirectory(status.isDirectory());
                info.setSize(status.getLen());
                info.setModificationTime(status.getModificationTime());
                info.setOwner(status.getOwner());
                info.setReplication(status.getReplication());
                info.setBlockSize(status.getBlockSize());
                info.setPermission(status.getPermission().toString());
                result.add(info);
            }
        } catch (Exception e) {
            log.error("Failed to list files at path: {}", hdfsPath, e);
        }
        return result;
    }

    /**
     * Upload a local file to HDFS.
     *
     * @param localPath the local file path
     * @param hdfsPath  the target HDFS path
     * @return true if upload succeeded, false otherwise
     */
    public boolean uploadFile(String localPath, String hdfsPath) {
        if (!isAvailable()) return false;
        InputStream in = null;
        OutputStream out = null;
        try {
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                log.error("Local file does not exist: {}", localPath);
                return false;
            }
            if (localFile.isDirectory()) {
                log.error("Local path is a directory, not a file: {}", localPath);
                return false;
            }

            Path dstPath = new Path(hdfsPath);
            // Ensure parent directory exists on HDFS
            Path parent = dstPath.getParent();
            if (parent != null && !fileSystem.exists(parent)) {
                fileSystem.mkdirs(parent);
            }

            in = new FileInputStream(localFile);
            out = fileSystem.create(dstPath, true);
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            out.flush();
            log.info("Uploaded file: {} -> {} ({} bytes)", localPath, hdfsPath, totalBytes);
            return true;
        } catch (Exception e) {
            log.error("Failed to upload file: {} -> {}", localPath, hdfsPath, e);
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /**
     * Download a file from HDFS to local filesystem.
     *
     * @param hdfsPath  the source HDFS path
     * @param localPath the target local path
     * @return true if download succeeded, false otherwise
     */
    public boolean downloadFile(String hdfsPath, String localPath) {
        if (!isAvailable()) return false;
        InputStream in = null;
        OutputStream out = null;
        try {
            Path srcPath = new Path(hdfsPath);
            if (!fileSystem.exists(srcPath)) {
                log.error("HDFS path does not exist: {}", hdfsPath);
                return false;
            }
            if (fileSystem.isDirectory(srcPath)) {
                log.error("HDFS path is a directory, not a file: {}", hdfsPath);
                return false;
            }

            // Ensure parent directory exists locally
            File localFile = new File(localPath);
            File parentDir = localFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            in = fileSystem.open(srcPath);
            out = new FileOutputStream(localFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            out.flush();
            log.info("Downloaded file: {} -> {} ({} bytes)", hdfsPath, localPath, totalBytes);
            return true;
        } catch (Exception e) {
            log.error("Failed to download file: {} -> {}", hdfsPath, localPath, e);
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /**
     * Delete a file or directory from HDFS (recursive).
     *
     * @param hdfsPath the HDFS path to delete
     * @return true if deletion succeeded, false otherwise
     */
    public boolean delete(String hdfsPath) {
        if (!isAvailable()) return false;
        try {
            Path path = new Path(hdfsPath);
            if (!fileSystem.exists(path)) {
                log.warn("Path does not exist, nothing to delete: {}", hdfsPath);
                return true;
            }
            boolean result = fileSystem.delete(path, true);
            if (result) {
                log.info("Deleted: {}", hdfsPath);
            } else {
                log.error("Failed to delete: {}", hdfsPath);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to delete path: {}", hdfsPath, e);
            return false;
        }
    }

    /**
     * Create a directory on HDFS (including parent directories).
     *
     * @param hdfsPath the HDFS path to create
     * @return true if directory was created, false otherwise
     */
    public boolean mkdir(String hdfsPath) {
        if (!isAvailable()) return false;
        try {
            Path path = new Path(hdfsPath);
            if (fileSystem.exists(path)) {
                log.warn("Path already exists: {}", hdfsPath);
                return true;
            }
            boolean result = fileSystem.mkdirs(path);
            if (result) {
                log.info("Created directory: {}", hdfsPath);
            } else {
                log.error("Failed to create directory: {}", hdfsPath);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to create directory: {}", hdfsPath, e);
            return false;
        }
    }

    /**
     * Get statistics for a given HDFS path.
     *
     * @param hdfsPath the HDFS path
     * @return FileStats containing total size, file count, and dir count; returns empty stats on failure
     */
    public FileStats getStats(String hdfsPath) {
        FileStats stats = new FileStats();
        stats.setPath(hdfsPath);
        if (!isAvailable()) return stats;
        try {
            Path path = new Path(hdfsPath);
            if (!fileSystem.exists(path)) {
                log.warn("Path does not exist: {}", hdfsPath);
                return stats;
            }
            org.apache.hadoop.fs.ContentSummary summary = fileSystem.getContentSummary(path);
            stats.setTotalSize(summary.getLength());
            stats.setFileCount(summary.getFileCount());
            stats.setDirCount(summary.getDirectoryCount());
            log.info("Stats for {}: totalSize={}, fileCount={}, dirCount={}",
                    hdfsPath, stats.getTotalSize(), stats.getFileCount(), stats.getDirCount());
        } catch (Exception e) {
            log.error("Failed to get stats for path: {}", hdfsPath, e);
        }
        return stats;
    }

    /**
     * Check if a path exists on HDFS.
     *
     * @param hdfsPath the HDFS path to check
     * @return true if the path exists, false otherwise
     */
    public boolean exists(String hdfsPath) {
        if (!isAvailable()) return false;
        try {
            Path path = new Path(hdfsPath);
            return fileSystem.exists(path);
        } catch (Exception e) {
            log.error("Failed to check existence for path: {}", hdfsPath, e);
            return false;
        }
    }

    /**
     * Close an InputStream quietly (no exception thrown).
     */
    private void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                log.warn("Failed to close input stream", e);
            }
        }
    }

    /**
     * Close an OutputStream quietly (no exception thrown).
     */
    private void closeQuietly(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                log.warn("Failed to close output stream", e);
            }
        }
    }
}
