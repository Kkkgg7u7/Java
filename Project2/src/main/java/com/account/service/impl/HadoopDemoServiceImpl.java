package com.account.service.impl;

import com.account.config.HadoopProperties;
import com.account.dto.HdfsDataPreview;
import com.account.dto.HdfsDirectoryAnalysis;
import com.account.dto.HdfsFileInfo;
import com.account.dto.HdfsPerformanceResult;
import com.account.dto.HdfsSizeStatistics;
import com.account.service.HadoopDemoService;
import com.account.util.HDFSUtil;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class HadoopDemoServiceImpl implements HadoopDemoService {

    private static final String[] CATEGORIES = {"餐饮", "交通", "购物", "娱乐", "住房", "医疗", "教育", "工资", "奖金", "投资"};

    private static final String[] ACCOUNTS = {"微信钱包", "支付宝余额", "工商银行卡", "现金钱包"};

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String CSV_HEADER = "record_id,type,amount,category,account,record_date,payload";

    private final HDFSUtil hdfsUtil;

    private final HadoopProperties properties;

    public HadoopDemoServiceImpl(HDFSUtil hdfsUtil, HadoopProperties properties) {
        this.hdfsUtil = hdfsUtil;
        this.properties = properties;
    }

    @Override
    public List<HdfsFileInfo> list(String path, boolean recursive) throws Exception {
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            try {
                return listLocal(path, recursive);
            } catch (Exception e) {
                return hdfsUtil.listFiles(path, recursive);
            }
        }
        return hdfsUtil.listFiles(path, recursive);
    }

    @Override
    public boolean mkdir(String path) throws Exception {
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            Files.createDirectories(resolveLocalPath(path));
            return true;
        }
        return hdfsUtil.mkdir(path);
    }

    @Override
    public String upload(String path, MultipartFile file, boolean overwrite) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            java.nio.file.Path dir = resolveLocalPath(path);
            Files.createDirectories(dir);
            java.nio.file.Path target = dir.resolve(safeName(file.getOriginalFilename()));
            if (!overwrite && Files.exists(target)) {
                throw new IllegalArgumentException("文件已存在");
            }
            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(target)) {
                IOUtils.copyBytes(in, out, properties.getBufferSize(), false);
            }
            return target.toUri().toString();
        }
        String hdfsPath = (StringUtils.hasText(path) ? path : properties.getTestDir())
                + "/" + safeName(file.getOriginalFilename());
        return hdfsUtil.upload(file.getInputStream(), hdfsPath, overwrite);
    }

    @Override
    public void download(String path, HttpServletResponse response) throws Exception {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("下载路径不能为空");
        }
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            java.nio.file.Path target = resolveLocalPath(path);
            if (Files.isDirectory(target)) {
                throw new IllegalArgumentException("目录不能直接下载");
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + target.getFileName());
            try (InputStream in = Files.newInputStream(target);
                 OutputStream out = response.getOutputStream()) {
                IOUtils.copyBytes(in, out, properties.getBufferSize(), false);
            }
            return;
        }
        HdfsFileInfo fileInfo = hdfsUtil.getFileStatus(path);
        if (fileInfo.isDirectory()) {
            throw new IllegalArgumentException("目录不能直接下载");
        }
        String fileName = new Path(path).getName();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        hdfsUtil.download(path, response.getOutputStream());
    }

    @Override
    public boolean delete(String path, boolean recursive) throws Exception {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("删除路径不能为空");
        }
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            java.nio.file.Path target = resolveLocalPath(path);
            if (!Files.exists(target)) {
                return false;
            }
            if (Files.isDirectory(target) && recursive) {
                try (Stream<java.nio.file.Path> stream = Files.walk(target)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
                }
                return true;
            }
            return Files.deleteIfExists(target);
        }
        return hdfsUtil.delete(path, recursive);
    }

    @Override
    public HdfsPerformanceResult runPerformanceDemo(int files, long records, int payloadSize, boolean clean) throws Exception {
        int normalizedFiles = Math.max(1, Math.min(files, 32));
        long normalizedRecords = Math.max(1, Math.min(records, 2_000_000L));
        int normalizedPayloadSize = Math.max(16, Math.min(payloadSize, 4096));
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            try {
                return runLocalPerformanceDemo(normalizedFiles, normalizedRecords, normalizedPayloadSize, clean);
            } catch (Exception e) {
                throw new RuntimeException("本地性能测试失败: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            }
        }
        try {
            return runHdfsPerformanceDemo(normalizedFiles, normalizedRecords, normalizedPayloadSize, clean);
        } catch (Exception e) {
            throw new RuntimeException("HDFS性能测试失败: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }

    private HdfsPerformanceResult runHdfsPerformanceDemo(int files, long records, int payloadSize, boolean clean) throws Exception {
        String runDirPath = properties.getTestDir() + "/run-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        hdfsUtil.mkdir(runDirPath);

        long totalStart = System.nanoTime();
        long bytes = 0;
        long checksum = 0;
        long writeStart = System.nanoTime();
        long remaining = records;
        for (int fileIndex = 0; fileIndex < files; fileIndex++) {
            long fileRecords = remaining / (files - fileIndex);
            remaining -= fileRecords;
            String filePath = runDirPath + "/" + String.format(Locale.ROOT, "records-%02d.csv", fileIndex + 1);
            try (FSDataOutputStream out = hdfsUtil.create(filePath, true)) {
                WriteStats stats = writeRecords(out, fileIndex, fileRecords, payloadSize);
                bytes += stats.bytes;
                checksum += stats.checksum;
            }
        }
        long writeMillis = elapsedMillis(writeStart);

        long readStart = System.nanoTime();
        long readRecords = 0;
        long readChecksum = 0;
        List<HdfsFileInfo> fileInfos = hdfsUtil.listFiles(runDirPath, false);
        for (HdfsFileInfo fileInfo : fileInfos) {
            try (FSDataInputStream in = hdfsUtil.open(fileInfo.getPath());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), properties.getBufferSize())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (CSV_HEADER.equals(line)) {
                        continue;
                    }
                    readRecords++;
                    readChecksum += line.length();
                }
            }
        }
        long readMillis = elapsedMillis(readStart);
        long totalMillis = elapsedMillis(totalStart);
        if (clean) {
            hdfsUtil.delete(runDirPath, true);
        }

        HdfsPerformanceResult result = new HdfsPerformanceResult();
        result.setTestName("HDFS sequential write/read throughput lab");
        result.setPath(runDirPath);
        result.setFiles(files);
        result.setRecords(readRecords);
        result.setBytes(bytes);
        result.setWriteMillis(writeMillis);
        result.setReadMillis(readMillis);
        result.setTotalMillis(totalMillis);
        result.setWriteMbPerSecond(mbPerSecond(bytes, writeMillis));
        result.setReadMbPerSecond(mbPerSecond(bytes, readMillis));
        result.setRecordsPerSecond(perSecond(readRecords, totalMillis));
        result.setChecksum(readChecksum);
        result.setStorageUri(hdfsUtil.getFileSystem().getUri().toString());
        result.setUsedLocalFileSystem(false);
        result.setMessage("Generated checksum=" + checksum + ", read checksum=" + readChecksum);
        return result;
    }

    private HdfsPerformanceResult runLocalPerformanceDemo(int files, long records, int payloadSize, boolean clean) throws Exception {
        java.nio.file.Path baseDir = resolveLocalPath(null);
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }
        java.nio.file.Path runDir = baseDir
                .resolve("run-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        Files.createDirectories(runDir);

        long totalStart = System.nanoTime();
        long bytes = 0;
        long checksum = 0;
        long writeStart = System.nanoTime();
        long remaining = records;
        for (int fileIndex = 0; fileIndex < files; fileIndex++) {
            long fileRecords = remaining / (files - fileIndex);
            remaining -= fileRecords;
            java.nio.file.Path file = runDir.resolve(String.format(Locale.ROOT, "records-%02d.csv", fileIndex + 1));
            try (OutputStream out = Files.newOutputStream(file)) {
                WriteStats stats = writeRecords(out, fileIndex, fileRecords, payloadSize);
                bytes += stats.bytes;
                checksum += stats.checksum;
            }
        }
        long writeMillis = elapsedMillis(writeStart);

        long readStart = System.nanoTime();
        long readRecords = 0;
        long readChecksum = 0;
        try (Stream<java.nio.file.Path> stream = Files.list(runDir)) {
            List<java.nio.file.Path> localFiles = new ArrayList<>();
            stream.filter(Files::isRegularFile).forEach(localFiles::add);
            for (java.nio.file.Path file : localFiles) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (CSV_HEADER.equals(line)) {
                            continue;
                        }
                        readRecords++;
                        readChecksum += line.length();
                    }
                }
            }
        }
        long readMillis = elapsedMillis(readStart);
        long totalMillis = elapsedMillis(totalStart);
        if (clean) {
            deleteLocalTree(runDir);
        }

        HdfsPerformanceResult result = new HdfsPerformanceResult();
        result.setTestName("Local file fallback sequential write/read throughput lab");
        result.setPath(runDir.toUri().toString());
        result.setFiles(files);
        result.setRecords(readRecords);
        result.setBytes(bytes);
        result.setWriteMillis(writeMillis);
        result.setReadMillis(readMillis);
        result.setTotalMillis(totalMillis);
        result.setWriteMbPerSecond(mbPerSecond(bytes, writeMillis));
        result.setReadMbPerSecond(mbPerSecond(bytes, readMillis));
        result.setRecordsPerSecond(perSecond(readRecords, totalMillis));
        result.setChecksum(readChecksum);
        result.setStorageUri(properties.getDefaultUri());
        result.setUsedLocalFileSystem(true);
        result.setMessage("Generated checksum=" + checksum + ", read checksum=" + readChecksum);
        return result;
    }

    private WriteStats writeRecords(OutputStream out, int fileIndex, long records, int payloadSize) throws Exception {
        WriteStats stats = new WriteStats();
        byte[] headerBytes = (CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8);
        out.write(headerBytes);
        stats.bytes += headerBytes.length;
        LocalDate baseDate = LocalDate.now().minusDays(180);
        String payload = fixedPayload(payloadSize);
        for (long i = 0; i < records; i++) {
            long sequence = fileIndex * 1_000_000_000L + i;
            int type = sequence % 5 == 0 ? 1 : 0;
            BigDecimal amount = BigDecimal.valueOf((sequence % 50000) / 100.0 + 1).setScale(2, RoundingMode.HALF_UP);
            String category = CATEGORIES[(int) (sequence % CATEGORIES.length)];
            String account = ACCOUNTS[(int) (sequence % ACCOUNTS.length)];
            String line = sequence + "," + type + "," + amount + "," + category + "," + account + ","
                    + baseDate.plusDays(sequence % 180).format(DATE_FORMATTER) + "," + payload + "\n";
            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            stats.bytes += bytes.length;
            stats.checksum += line.trim().length();
        }
        return stats;
    }

    private List<HdfsFileInfo> listLocal(String path, boolean recursive) throws Exception {
        java.nio.file.Path target = resolveLocalPath(path);
        List<HdfsFileInfo> files = new ArrayList<>();
        if (!Files.exists(target)) {
            return files;
        }
        if (recursive) {
            try (Stream<java.nio.file.Path> stream = Files.walk(target)) {
                stream.filter(item -> !item.equals(target)).forEach(item -> files.add(toLocalInfo(item)));
            }
            return files;
        }
        try (Stream<java.nio.file.Path> stream = Files.list(target)) {
            stream.forEach(item -> files.add(toLocalInfo(item)));
        }
        return files;
    }

    private HdfsFileInfo toLocalInfo(java.nio.file.Path path) {
        try {
            boolean directory = Files.isDirectory(path);
            return new HdfsFileInfo(
                    path.toUri().toString(),
                    path.getFileName().toString(),
                    directory ? 0 : Files.size(path),
                    directory,
                    (short) 1,
                    0,
                    Files.getLastModifiedTime(path).toMillis()
            );
        } catch (Exception e) {
            return new HdfsFileInfo(path.toUri().toString(), path.getFileName().toString(), 0, Files.isDirectory(path), (short) 1, 0, 0);
        }
    }

    private java.nio.file.Path resolveLocalPath(String path) {
        String rawPath = StringUtils.hasText(path) ? path : properties.getTestDir();
        if (rawPath.startsWith("file:")) {
            return Paths.get(java.net.URI.create(rawPath));
        }
        return Paths.get(rawPath);
    }

    private void deleteLocalTree(java.nio.file.Path path) throws Exception {
        try (Stream<java.nio.file.Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        }
    }

    private void deleteQuietly(java.nio.file.Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // Best-effort cleanup for experiment output.
        }
    }

    private String fixedPayload(int payloadSize) {
        String seed = "springboot-hadoop-performance-lab-account-record-";
        StringBuilder builder = new StringBuilder(payloadSize);
        while (builder.length() < payloadSize) {
            builder.append(seed);
        }
        return builder.substring(0, payloadSize);
    }

    private String safeName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "upload-" + System.currentTimeMillis();
        }
        return originalFilename.replaceAll("[\\\\/]+", "_");
    }

    private long elapsedMillis(long startNano) {
        return Math.max(1, (System.nanoTime() - startNano) / 1_000_000);
    }

    private double mbPerSecond(long bytes, long millis) {
        double mb = bytes / 1024.0 / 1024.0;
        return round(mb / (millis / 1000.0));
    }

    private double perSecond(long count, long millis) {
        return round(count / (millis / 1000.0));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static class WriteStats {

        private long bytes;

        private long checksum;
    }

    // ==================== 2号任务：大数据业务逻辑 ====================

    @Override
    public HdfsSizeStatistics getSizeStatistics(String path, int topN) throws Exception {
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            return getLocalSizeStatistics(path, topN);
        }
        List<HdfsFileInfo> allFiles = hdfsUtil.listFiles(path, true);
        HdfsSizeStatistics stats = new HdfsSizeStatistics();
        stats.setPath(path);

        long totalSize = 0;
        int fileCount = 0;
        int dirCount = 0;
        for (HdfsFileInfo f : allFiles) {
            if (f.isDirectory()) {
                dirCount++;
            } else {
                fileCount++;
                totalSize += f.getLength();
            }
        }
        stats.setTotalSize(totalSize);
        stats.setFileCount(fileCount);
        stats.setDirectoryCount(dirCount);
        stats.setSizeMb(round(totalSize / 1024.0 / 1024.0));
        stats.setSizeGb(round(totalSize / 1024.0 / 1024.0 / 1024.0));

        List<HdfsFileInfo> sorted = new ArrayList<>(allFiles);
        sorted.sort((a, b) -> Long.compare(b.getLength(), a.getLength()));
        stats.setTopFiles(sorted.subList(0, Math.min(topN, sorted.size())));
        return stats;
    }

    private HdfsSizeStatistics getLocalSizeStatistics(String path, int topN) throws Exception {
        java.nio.file.Path target = resolveLocalPath(path);
        List<HdfsFileInfo> allFiles = listLocal(path, true);
        HdfsSizeStatistics stats = new HdfsSizeStatistics();
        stats.setPath(path);

        long totalSize = 0;
        int fileCount = 0;
        int dirCount = 0;
        for (HdfsFileInfo f : allFiles) {
            if (f.isDirectory()) {
                dirCount++;
            } else {
                fileCount++;
                totalSize += f.getLength();
            }
        }
        stats.setTotalSize(totalSize);
        stats.setFileCount(fileCount);
        stats.setDirectoryCount(dirCount);
        stats.setSizeMb(round(totalSize / 1024.0 / 1024.0));
        stats.setSizeGb(round(totalSize / 1024.0 / 1024.0 / 1024.0));

        List<HdfsFileInfo> sorted = new ArrayList<>(allFiles);
        sorted.sort((a, b) -> Long.compare(b.getLength(), a.getLength()));
        stats.setTopFiles(sorted.subList(0, Math.min(topN, sorted.size())));
        return stats;
    }

    @Override
    public HdfsDirectoryAnalysis analyzeDirectory(String path) throws Exception {
        List<HdfsFileInfo> allItems;
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            allItems = listLocal(path, true);
        } else {
            allItems = hdfsUtil.listFiles(path, true);
        }

        HdfsDirectoryAnalysis analysis = new HdfsDirectoryAnalysis();
        analysis.setPath(path);

        int fileCount = 0;
        int dirCount = 0;
        long totalSize = 0;
        long maxFileSize = 0;
        long minFileSize = Long.MAX_VALUE;
        Map<String, Integer> typeDist = new LinkedHashMap<>();

        for (HdfsFileInfo item : allItems) {
            if (item.isDirectory()) {
                dirCount++;
            } else {
                fileCount++;
                totalSize += item.getLength();
                if (item.getLength() > maxFileSize) {
                    maxFileSize = item.getLength();
                }
                if (item.getLength() < minFileSize) {
                    minFileSize = item.getLength();
                }
                String ext = getFileExtension(item.getName());
                typeDist.merge(ext, 1, Integer::sum);
            }
        }

        analysis.setTotalFiles(fileCount);
        analysis.setTotalDirectories(dirCount);
        analysis.setTotalSize(totalSize);
        analysis.setMaxFileSize(fileCount > 0 ? maxFileSize : 0);
        analysis.setMinFileSize(fileCount > 0 ? minFileSize : 0);
        analysis.setAvgFileSize(fileCount > 0 ? round((double) totalSize / fileCount) : 0);
        analysis.setFileTypeDistribution(typeDist);

        List<HdfsFileInfo> sorted = new ArrayList<>(allItems);
        sorted.sort((a, b) -> Long.compare(b.getLength(), a.getLength()));
        analysis.setLargestFiles(sorted.subList(0, Math.min(10, sorted.size())));

        return analysis;
    }

    @Override
    public HdfsDataPreview previewData(String path, int previewLines) throws Exception {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("预览路径不能为空");
        }
        int maxLines = Math.max(1, Math.min(previewLines, 100));
        HdfsDataPreview preview = new HdfsDataPreview();
        preview.setPath(path);

        if (hdfsUtil.isLocalFileSystemConfigured()) {
            java.nio.file.Path target = resolveLocalPath(path);
            if (Files.isDirectory(target)) {
                throw new IllegalArgumentException("目录不能直接预览");
            }
            try (BufferedReader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
                readPreview(reader, preview, maxLines);
            }
            return preview;
        }

        HdfsFileInfo fileInfo = hdfsUtil.getFileStatus(path);
        if (fileInfo.isDirectory()) {
            throw new IllegalArgumentException("目录不能直接预览");
        }
        try (FSDataInputStream in = hdfsUtil.open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), properties.getBufferSize())) {
            readPreview(reader, preview, maxLines);
        }
        return preview;
    }

    private void readPreview(BufferedReader reader, HdfsDataPreview preview, int maxLines) throws Exception {
        List<String> lines = new ArrayList<>();
        long totalLines = 0;
        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (firstLine && line.contains(",")) {
                preview.setHeaders(line.split(","));
                firstLine = false;
                continue;
            }
            firstLine = false;
            if (lines.size() < maxLines) {
                lines.add(line);
            }
            totalLines++;
        }
        preview.setTotalLines(totalLines);
        preview.setSampleLines(lines);
        preview.setPreviewLineCount(lines.size());
    }

    @Override
    public int batchDelete(List<String> paths, boolean recursive) throws Exception {
        if (paths == null || paths.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (String path : paths) {
            try {
                if (delete(path, recursive)) {
                    deleted++;
                }
            } catch (Exception ignored) {
                // 单条失败不影响批量操作
            }
        }
        return deleted;
    }

    private String getFileExtension(String name) {
        if (name == null || !name.contains(".")) {
            return "other";
        }
        int lastDot = name.lastIndexOf('.');
        String ext = name.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return ext.isEmpty() ? "other" : ext;
    }
}
