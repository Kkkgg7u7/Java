package com.account.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total size in bytes */
    private long totalSize;

    /** Number of files */
    private long fileCount;

    /** Number of directories */
    private long dirCount;

    /** The HDFS path */
    private String path;
}
