package com.account.dto;

public class HdfsPerformanceResult {

    private String testName;

    private String path;

    private int files;

    private long records;

    private long bytes;

    private long writeMillis;

    private long readMillis;

    private long totalMillis;

    private double writeMbPerSecond;

    private double readMbPerSecond;

    private double recordsPerSecond;

    private long checksum;

    private String storageUri;

    private boolean usedLocalFileSystem;

    private String message;

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getFiles() {
        return files;
    }

    public void setFiles(int files) {
        this.files = files;
    }

    public long getRecords() {
        return records;
    }

    public void setRecords(long records) {
        this.records = records;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public long getWriteMillis() {
        return writeMillis;
    }

    public void setWriteMillis(long writeMillis) {
        this.writeMillis = writeMillis;
    }

    public long getReadMillis() {
        return readMillis;
    }

    public void setReadMillis(long readMillis) {
        this.readMillis = readMillis;
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    public void setTotalMillis(long totalMillis) {
        this.totalMillis = totalMillis;
    }

    public double getWriteMbPerSecond() {
        return writeMbPerSecond;
    }

    public void setWriteMbPerSecond(double writeMbPerSecond) {
        this.writeMbPerSecond = writeMbPerSecond;
    }

    public double getReadMbPerSecond() {
        return readMbPerSecond;
    }

    public void setReadMbPerSecond(double readMbPerSecond) {
        this.readMbPerSecond = readMbPerSecond;
    }

    public double getRecordsPerSecond() {
        return recordsPerSecond;
    }

    public void setRecordsPerSecond(double recordsPerSecond) {
        this.recordsPerSecond = recordsPerSecond;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public void setStorageUri(String storageUri) {
        this.storageUri = storageUri;
    }

    public boolean isUsedLocalFileSystem() {
        return usedLocalFileSystem;
    }

    public void setUsedLocalFileSystem(boolean usedLocalFileSystem) {
        this.usedLocalFileSystem = usedLocalFileSystem;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
