package com.account.dto;

import java.util.Map;

public class HdfsHealthInfo {

    private boolean connected;

    private String defaultFs;

    private String scheme;

    private String uri;

    private String user;

    private String baseDir;

    private long probeMillis;

    private String message;

    private Map<String, String> haConfig;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getDefaultFs() {
        return defaultFs;
    }

    public void setDefaultFs(String defaultFs) {
        this.defaultFs = defaultFs;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public long getProbeMillis() {
        return probeMillis;
    }

    public void setProbeMillis(long probeMillis) {
        this.probeMillis = probeMillis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getHaConfig() {
        return haConfig;
    }

    public void setHaConfig(Map<String, String> haConfig) {
        this.haConfig = haConfig;
    }
}
