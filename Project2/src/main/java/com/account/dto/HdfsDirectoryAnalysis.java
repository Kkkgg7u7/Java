package com.account.dto;

import java.util.List;
import java.util.Map;

public class HdfsDirectoryAnalysis {

    private String path;

    private int totalFiles;

    private int totalDirectories;

    private long totalSize;

    private long maxFileSize;

    private long minFileSize;

    private double avgFileSize;

    private List<HdfsFileInfo> largestFiles;

    private Map<String, Integer> fileTypeDistribution;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalDirectories() {
        return totalDirectories;
    }

    public void setTotalDirectories(int totalDirectories) {
        this.totalDirectories = totalDirectories;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(long minFileSize) {
        this.minFileSize = minFileSize;
    }

    public double getAvgFileSize() {
        return avgFileSize;
    }

    public void setAvgFileSize(double avgFileSize) {
        this.avgFileSize = avgFileSize;
    }

    public List<HdfsFileInfo> getLargestFiles() {
        return largestFiles;
    }

    public void setLargestFiles(List<HdfsFileInfo> largestFiles) {
        this.largestFiles = largestFiles;
    }

    public Map<String, Integer> getFileTypeDistribution() {
        return fileTypeDistribution;
    }

    public void setFileTypeDistribution(Map<String, Integer> fileTypeDistribution) {
        this.fileTypeDistribution = fileTypeDistribution;
    }
}
