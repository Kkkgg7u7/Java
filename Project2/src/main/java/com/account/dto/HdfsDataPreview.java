package com.account.dto;

import java.util.List;

public class HdfsDataPreview {

    private String path;

    private long totalLines;

    private List<String> sampleLines;

    private String[] headers;

    private int previewLineCount;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(long totalLines) {
        this.totalLines = totalLines;
    }

    public List<String> getSampleLines() {
        return sampleLines;
    }

    public void setSampleLines(List<String> sampleLines) {
        this.sampleLines = sampleLines;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    public int getPreviewLineCount() {
        return previewLineCount;
    }

    public void setPreviewLineCount(int previewLineCount) {
        this.previewLineCount = previewLineCount;
    }
}
