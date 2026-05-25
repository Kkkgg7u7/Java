package com.account.controller;

import com.account.dto.HdfsDataPreview;
import com.account.dto.HdfsDirectoryAnalysis;
import com.account.dto.HdfsFileInfo;
import com.account.dto.HdfsPerformanceResult;
import com.account.dto.HdfsSizeStatistics;
import com.account.config.HadoopProperties;
import com.account.service.HadoopDemoService;
import com.account.util.HDFSUtil;
import com.account.util.Result;
import org.apache.hadoop.fs.Path;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/hdfs", "/api/hdfs"})
public class HDFSController {

    private final HDFSUtil hdfsUtil;

    private final HadoopDemoService hadoopDemoService;

    private final HadoopProperties hadoopProperties;

    public HDFSController(HDFSUtil hdfsUtil, HadoopDemoService hadoopDemoService, HadoopProperties hadoopProperties) {
        this.hdfsUtil = hdfsUtil;
        this.hadoopDemoService = hadoopDemoService;
        this.hadoopProperties = hadoopProperties;
    }

    @GetMapping("/health")
    public Result health(HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        return Result.success(hdfsUtil.health());
    }

    @GetMapping("/list")
    public Result list(@RequestParam(value = "path", required = false) String path,
                       @RequestParam(value = "recursive", defaultValue = "false") boolean recursive,
                       HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        try {
            return Result.success(hadoopDemoService.list(guardReadPath(path), recursive));
        } catch (Exception e) {
            return Result.error("HDFS 列表读取失败：" + e.getMessage());
        }
    }

    @PostMapping("/mkdir")
    public Result mkdir(@RequestParam("path") String path, HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        if (path == null || path.trim().isEmpty()) {
            return Result.error("路径不能为空");
        }
        try {
            String safePath = guardWritePath(path);
            return hadoopDemoService.mkdir(safePath) ? Result.success("目录创建成功") : Result.error("目录创建失败");
        } catch (Exception e) {
            return Result.error("HDFS 目录创建失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result upload(@RequestParam(value = "path", required = false) String path,
                         @RequestParam("file") MultipartFile file,
                         @RequestParam(value = "overwrite", defaultValue = "true") boolean overwrite,
                         HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            return Result.success("上传成功", hadoopDemoService.upload(guardWritePath(path), file, overwrite));
        } catch (Exception e) {
            return Result.error("HDFS 上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/download")
    public void download(@RequestParam("path") String path, HttpServletResponse response, HttpSession session) throws Exception {
        if (session.getAttribute("userId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return;
        }
        hadoopDemoService.download(guardReadPath(path), response);
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam("path") String path,
                         @RequestParam(value = "recursive", defaultValue = "true") boolean recursive,
                         HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        if (path == null || path.trim().isEmpty()) {
            return Result.error("删除路径不能为空");
        }
        try {
            String safePath = guardDeletePath(path);
            return hadoopDemoService.delete(safePath, recursive) ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("HDFS 删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public Result batchDelete(@RequestBody Map<String, Object> params, HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) params.get("paths");
        Boolean recursive = (Boolean) params.getOrDefault("recursive", true);
        if (paths == null || paths.isEmpty()) {
            return Result.error("路径列表不能为空");
        }
        try {
            List<String> safePaths = new ArrayList<>(paths.size());
            for (String path : paths) {
                safePaths.add(guardDeletePath(path));
            }
            int deleted = hadoopDemoService.batchDelete(safePaths, recursive != null && recursive);
            return Result.success("批量删除完成，成功删除 " + deleted + " 个", deleted);
        } catch (Exception e) {
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/performance")
    public Result performance(@RequestParam(value = "files", defaultValue = "4") int files,
                              @RequestParam(value = "records", defaultValue = "20000") long records,
                              @RequestParam(value = "payloadSize", defaultValue = "128") int payloadSize,
                              @RequestParam(value = "clean", defaultValue = "false") boolean clean,
                              HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        if (files < 1 || files > 32) {
            return Result.error("文件数范围：1-32");
        }
        if (records < 1 || records > 2000000) {
            return Result.error("记录数范围：1-2000000");
        }
        if (payloadSize < 16 || payloadSize > 4096) {
            return Result.error("单条填充字节范围：16-4096");
        }
        try {
            HdfsPerformanceResult result = hadoopDemoService.runPerformanceDemo(files, records, payloadSize, clean);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("HDFS 性能测试失败：" + e.getMessage());
        }
    }

    @GetMapping("/size-statistics")
    public Result sizeStatistics(@RequestParam(value = "path", required = false) String path,
                                 @RequestParam(value = "topN", defaultValue = "10") int topN,
                                 HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        try {
            HdfsSizeStatistics stats = hadoopDemoService.getSizeStatistics(guardReadPath(path), topN);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("文件大小统计失败：" + e.getMessage());
        }
    }

    @GetMapping("/directory-analysis")
    public Result directoryAnalysis(@RequestParam(value = "path", required = false) String path, HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        try {
            HdfsDirectoryAnalysis analysis = hadoopDemoService.analyzeDirectory(guardReadPath(path));
            return Result.success(analysis);
        } catch (Exception e) {
            return Result.error("目录分析失败：" + e.getMessage());
        }
    }

    @GetMapping("/data-preview")
    public Result dataPreview(@RequestParam("path") String path,
                              @RequestParam(value = "lines", defaultValue = "20") int lines,
                              HttpSession session) {
        Result<?> authResult = requireLogin(session);
        if (authResult != null) {
            return authResult;
        }
        if (path == null || path.trim().isEmpty()) {
            return Result.error("预览路径不能为空");
        }
        try {
            HdfsDataPreview preview = hadoopDemoService.previewData(guardReadPath(path), lines);
            return Result.success(preview);
        } catch (Exception e) {
            return Result.error("数据预览失败：" + e.getMessage());
        }
    }

    private Result<?> requireLogin(HttpSession session) {
        return session.getAttribute("userId") == null ? Result.error(401, "请先登录") : null;
    }

    private String guardReadPath(String path) {
        String target = hasText(path) ? path.trim() : hadoopProperties.getTestDir();
        if (!isUnderExperimentDir(target)) {
            throw new IllegalArgumentException("只能访问实验目录：" + hadoopProperties.getTestDir());
        }
        return target;
    }

    private String guardWritePath(String path) {
        return guardReadPath(path);
    }

    private String guardDeletePath(String path) {
        String target = guardReadPath(path);
        if (samePath(target, hadoopProperties.getTestDir())) {
            throw new IllegalArgumentException("为避免误删，不能直接删除实验根目录");
        }
        return target;
    }

    private boolean isUnderExperimentDir(String path) {
        String target = normalize(path);
        String base = normalize(hadoopProperties.getTestDir());
        return target.equals(base) || target.startsWith(base.endsWith("/") ? base : base + "/");
    }

    private boolean samePath(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String path) {
        if (!hasText(path)) {
            return normalize(hadoopProperties.getTestDir());
        }
        String value = path.trim();
        if (value.startsWith("file:")) {
            return java.nio.file.Paths.get(java.net.URI.create(value)).toAbsolutePath().normalize().toString();
        }
        if (hdfsUtil.isLocalFileSystemConfigured()) {
            return java.nio.file.Paths.get(value).toAbsolutePath().normalize().toString();
        }
        String normalized = new Path(value).toUri().normalize().getPath();
        return normalized.endsWith("/") && normalized.length() > 1
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
