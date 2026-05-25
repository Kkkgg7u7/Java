package com.account.util;

import com.account.config.HadoopProperties;
import com.account.dto.HdfsFileInfo;
import com.account.dto.HdfsHealthInfo;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HDFSUtil {

    private final HadoopProperties properties;

    private FileSystem fileSystem;

    private Configuration configuration;

    private boolean initialized;

    public HDFSUtil(HadoopProperties properties) {
        this.properties = properties;
    }

    private synchronized void ensureInitialized() throws Exception {
        if (initialized) {
            return;
        }
        configuration = new Configuration();
        loadHadoopResource("core-site.xml");
        loadHadoopResource("hdfs-site.xml");
        if (StringUtils.hasText(properties.getDefaultUri())) {
            configuration.set("fs.defaultFS", properties.getDefaultUri());
        }
        configuration.set("hadoop.security.authentication", "simple");
        configuration.setInt("ipc.client.connect.timeout", properties.getConnectTimeoutMs());
        configuration.setInt("io.file.buffer.size", properties.getBufferSize());
        URI defaultUri = URI.create(configuration.get("fs.defaultFS"));
        if ("file".equalsIgnoreCase(defaultUri.getScheme())) {
            fileSystem = FileSystem.get(defaultUri, configuration);
        } else {
            fileSystem = FileSystem.get(defaultUri, configuration, properties.getUser());
        }
        initialized = true;
    }

    private void loadHadoopResource(String fileName) {
        if (!StringUtils.hasText(properties.getConfigLocation())) {
            return;
        }
        File configFile = new File(properties.getConfigLocation(), fileName);
        if (configFile.isFile()) {
            configuration.addResource(new Path(configFile.toURI()));
        }
    }

    public FileSystem getFileSystem() throws Exception {
        ensureInitialized();
        return fileSystem;
    }

    public Configuration getConfiguration() throws Exception {
        ensureInitialized();
        return configuration;
    }

    public HadoopProperties getProperties() {
        return properties;
    }

    public boolean isLocalFileSystemConfigured() {
        String defaultUri = properties.getDefaultUri();
        return StringUtils.hasText(defaultUri) && defaultUri.toLowerCase().startsWith("file:");
    }

    public Path resolvePath(String path) {
        if (StringUtils.hasText(path)) {
            return new Path(path);
        }
        return new Path(properties.getTestDir());
    }

    // ==================== 基础文件操作 API ====================

    /**
     * 判断路径是否存在
     */
    public boolean exists(String path) throws Exception {
        return getFileSystem().exists(resolvePath(path));
    }

    /**
     * 获取文件/目录状态信息
     */
    public HdfsFileInfo getFileStatus(String path) throws Exception {
        FileStatus status = getFileSystem().getFileStatus(resolvePath(path));
        return toInfo(status);
    }

    /**
     * 列出目录下的文件和子目录（非递归）
     */
    public List<HdfsFileInfo> listStatus(String path) throws Exception {
        Path target = resolvePath(path);
        FileSystem fs = getFileSystem();
        if (!fs.exists(target)) {
            return new ArrayList<>();
        }
        FileStatus[] statuses = fs.listStatus(target);
        List<HdfsFileInfo> result = new ArrayList<>(statuses.length);
        for (FileStatus status : statuses) {
            result.add(toInfo(status));
        }
        return result;
    }

    /**
     * 递归列出目录下所有文件
     */
    public List<HdfsFileInfo> listFiles(String path, boolean recursive) throws Exception {
        Path target = resolvePath(path);
        FileSystem fs = getFileSystem();
        if (!fs.exists(target)) {
            return new ArrayList<>();
        }
        if (recursive) {
            RemoteIterator<LocatedFileStatus> iterator = fs.listFiles(target, true);
            List<HdfsFileInfo> result = new ArrayList<>();
            while (iterator.hasNext()) {
                result.add(toInfo(iterator.next()));
            }
            return result;
        }
        return listStatus(path);
    }

    /**
     * 创建目录
     */
    public boolean mkdir(String path) throws Exception {
        return getFileSystem().mkdirs(resolvePath(path));
    }

    /**
     * 上传文件：从 InputStream 写入 HDFS
     */
    public String upload(InputStream inputStream, String path, boolean overwrite) throws Exception {
        Path target = resolvePath(path);
        FileSystem fs = getFileSystem();
        fs.mkdirs(target.getParent());
        try (FSDataOutputStream out = fs.create(target, overwrite)) {
            IOUtils.copyBytes(inputStream, out, properties.getBufferSize(), false);
        }
        return target.toString();
    }

    /**
     * 下载文件：从 HDFS 读取到 OutputStream
     */
    public void download(String path, OutputStream outputStream) throws Exception {
        Path target = resolvePath(path);
        try (FSDataInputStream in = getFileSystem().open(target)) {
            IOUtils.copyBytes(in, outputStream, properties.getBufferSize(), false);
        }
    }

    /**
     * 删除文件或目录
     */
    public boolean delete(String path, boolean recursive) throws Exception {
        return getFileSystem().delete(resolvePath(path), recursive);
    }

    /**
     * 创建文件并返回输出流（供调用方自行写入数据）
     */
    public FSDataOutputStream create(String path, boolean overwrite) throws Exception {
        Path target = resolvePath(path);
        FileSystem fs = getFileSystem();
        fs.mkdirs(target.getParent());
        return fs.create(target, overwrite);
    }

    /**
     * 打开文件并返回输入流（供调用方自行读取数据）
     */
    public FSDataInputStream open(String path) throws Exception {
        return getFileSystem().open(resolvePath(path));
    }

    private HdfsFileInfo toInfo(FileStatus status) {
        return new HdfsFileInfo(
                status.getPath().toString(),
                status.getPath().getName(),
                status.getLen(),
                status.isDirectory(),
                status.getReplication(),
                status.getBlockSize(),
                status.getModificationTime()
        );
    }

    // ==================== 健康检查 ====================

    public HdfsHealthInfo health() {
        HdfsHealthInfo info = new HdfsHealthInfo();
        long start = System.nanoTime();
        info.setBaseDir(properties.getTestDir());
        info.setUser(properties.getUser());
        try {
            FileSystem fs = getFileSystem();
            Configuration conf = getConfiguration();
            fs.exists(new Path(properties.getTestDir()));
            info.setConnected(true);
            info.setDefaultFs(conf.get("fs.defaultFS"));
            info.setScheme(fs.getScheme());
            info.setUri(fs.getUri().toString());
            info.setHaConfig(readHaConfig(conf));
            info.setMessage("Hadoop client is ready.");
        } catch (Exception e) {
            info.setConnected(isLocalFileSystemConfigured());
            info.setDefaultFs(properties.getDefaultUri());
            if (isLocalFileSystemConfigured()) {
                info.setScheme("file");
                info.setUri(properties.getDefaultUri());
                info.setMessage("Using local file fallback for offline performance lab. Hadoop client message: " + e.getMessage());
            } else {
                info.setMessage(e.getMessage());
            }
        }
        info.setProbeMillis((System.nanoTime() - start) / 1_000_000);
        return info;
    }

    private Map<String, String> readHaConfig(Configuration conf) {
        Map<String, String> result = new LinkedHashMap<>();
        putIfPresent(result, "fs.defaultFS", conf);
        putIfPresent(result, "dfs.nameservices", conf);
        String nameservice = conf.get("dfs.nameservices");
        if (StringUtils.hasText(nameservice)) {
            putIfPresent(result, "dfs.ha.namenodes." + nameservice, conf);
            putIfPresent(result, "dfs.client.failover.proxy.provider." + nameservice, conf);
            String nameNodes = conf.get("dfs.ha.namenodes." + nameservice);
            if (StringUtils.hasText(nameNodes)) {
                for (String nameNode : nameNodes.split(",")) {
                    putIfPresent(result, "dfs.namenode.rpc-address." + nameservice + "." + nameNode.trim(), conf);
                }
            }
        }
        putIfPresent(result, "ha.zookeeper.quorum", conf);
        putIfPresent(result, "dfs.ha.automatic-failover.enabled", conf);
        return result;
    }

    private void putIfPresent(Map<String, String> result, String key, Configuration conf) {
        String value = conf.get(key);
        if (StringUtils.hasText(value)) {
            result.put(key, value);
        }
    }

    @PreDestroy
    public synchronized void destroy() throws Exception {
        if (fileSystem != null) {
            fileSystem.close();
            fileSystem = null;
        }
        initialized = false;
    }
}
