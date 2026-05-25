package com.account.dto;

import java.util.List;

public class HdfsSizeStatistics {

    private String path;

    private long totalSize;

    private int fileCount;

    private int directoryCount;

    private double sizeMb;

    private double sizeGb;

    private List<HdfsFileInfo> topFiles;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getDirectoryCount() {
        return directoryCount;
    }

    public void setDirectoryCount(int directoryCount) {
        this.directoryCount = directoryCount;
    }

    public double getSizeMb() {
        return sizeMb;
    }

    public void setSizeMb(double sizeMb) {
        this.sizeMb = sizeMb;
    }

    public double getSizeGb() {
        return sizeGb;
    }

    public void setSizeGb(double sizeGb) {
        this.sizeGb = sizeGb;
    }

    public List<HdfsFileInfo> getTopFiles() {
        return topFiles;
    }

    public void setTopFiles(List<HdfsFileInfo> topFiles) {
        this.topFiles = topFiles;
    }
}
