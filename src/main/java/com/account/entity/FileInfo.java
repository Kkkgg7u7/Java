package com.account.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** File or directory name (without path) */
    private String name;

    /** Full HDFS path */
    private String path;

    /** Whether this is a directory */
    private boolean isDirectory;

    /** File size in bytes */
    private long size;

    /** Last modification time (epoch millis) */
    private long modificationTime;

    /** Owner of the file */
    private String owner;

    /** Replication factor */
    private short replication;

    /** Block size in bytes */
    private long blockSize;

    /** Permission string (e.g., rwxr-xr-x) */
    private String permission;
}
