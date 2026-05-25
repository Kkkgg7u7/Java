package com.account.dto;

public class HdfsFileInfo {

    private String path;

    private String name;

    private long length;

    private boolean directory;

    private short replication;

    private long blockSize;

    private long modificationTime;

    public HdfsFileInfo() {
    }

    public HdfsFileInfo(String path, String name, long length, boolean directory, short replication, long blockSize, long modificationTime) {
        this.path = path;
        this.name = name;
        this.length = length;
        this.directory = directory;
        this.replication = replication;
        this.blockSize = blockSize;
        this.modificationTime = modificationTime;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public short getReplication() {
        return replication;
    }

    public void setReplication(short replication) {
        this.replication = replication;
    }

    public long getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public long getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }
}
