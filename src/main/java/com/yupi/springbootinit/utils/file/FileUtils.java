package com.yupi.springbootinit.utils.file;

import cn.hutool.core.io.resource.InputStreamResource;
import cn.hutool.core.io.resource.Resource;

import com.yupi.springbootinit.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 文件处理工具类
 *
 * @author dev_shd
 */
@Slf4j
public class FileUtils {
    /**
     * 字符常量：斜杠 {@code '/'}
     */
    public static final char SLASH = '/';

    /**
     * 字符常量：反斜杠 {@code '\\'}
     */
    public static final char BACKSLASH = '\\';

    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    /**
     * 输出指定文件的byte数组
     *
     * @param filePath 文件路径
     * @param os       输出流
     * @return
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            flag = file.delete();
        }
        return flag;
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 检查文件是否可下载
     *
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownload(String resource) {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, "..")) {
            return false;
        }
        // 判断是否在允许下载的文件规则内
        return ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, FileTypeUtils.getFileType(resource));
    }

    /**
     * 下载文件名重新编码
     *
     * @param request  请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 返回文件名
     *
     * @param filePath 文件
     * @return 文件名
     */
    public static String getName(String filePath) {
        if (null == filePath) {
            return null;
        }
        int len = filePath.length();
        if (0 == len) {
            return filePath;
        }
        if (isFileSeparator(filePath.charAt(len - 1))) {
            // 以分隔符结尾的去掉结尾分隔符
            len--;
        }

        int begin = 0;
        char c;
        for (int i = len - 1; i > -1; i--) {
            c = filePath.charAt(i);
            if (isFileSeparator(c)) {
                // 查找最后一个路径分隔符（/或者\）
                begin = i + 1;
                break;
            }
        }

        return filePath.substring(begin, len);
    }

    /**
     * 是否为Windows或者Linux（Unix）文件分隔符<br>
     * Windows平台下分隔符为\，Linux（Unix）为/
     *
     * @param c 字符
     * @return 是否为Windows或者Linux（Unix）文件分隔符
     */
    public static boolean isFileSeparator(char c) {
        return SLASH == c || BACKSLASH == c;
    }

    /**
     * 下载文件名重新编码
     *
     * @param response     响应对象
     * @param realFileName 真实文件名
     * @return
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.setHeader("Content-disposition", contentDispositionValue.toString());
        response.setHeader("download-filename", percentEncodedFileName);
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");
    }

    /**
     * 创建文件夹
     *
     * @param path
     * @return
     */
    public static File createFolder(String path) {
        File file = new File(path);
        if (!file.exists()) file.mkdirs();
        return file;
    }

    /**
     * 查询指定文件夹下包含指定文件名的文件列表
     */
    public static List<File> searchFiles(File folderFile, String targetFileString) {
        // 生成文件过滤器
//        FileFilter fileFilter = new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//               return file.isFile() && file.getName().contains(targetFileString);
//            }
//        };
        // 生成文件Array
        File[] filteredFiles = folderFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().contains(targetFileString);
            }
        });
        // 将文件Array转换成List
        List<File> fileList = new ArrayList<File>();
        if (filteredFiles != null) {
            Collections.addAll(fileList, filteredFiles);
        }
        return fileList;
    }

    public static List<File> searchFolder(File folderFile, String targetFileString) {
        // 生成文件过滤器
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && file.getName().contains(targetFileString);
            }
        };
        // 生成文件Array
        File[] filteredFiles = folderFile.listFiles(fileFilter);
        // 将文件Array转换成List
        List<File> fileList = new ArrayList<File>();
        if (filteredFiles != null) {
            Collections.addAll(fileList, filteredFiles);
        }
        return fileList;
    }

    /**
     * 查询指定文件夹下包含指定文件名的文件列表
     */
    public static List<File> searchFiles(List<String> filePaths) {
        // 生成文件Array
        if (filePaths == null || filePaths.isEmpty()) return Collections.emptyList();
        List<File> files = new ArrayList<>();
        filePaths.forEach(filePath -> {
            File file = new File(filePath);
            if (file.exists()) files.add(file);
        });
        return files;
    }

    /**
     * 给文件List进行时间排序
     */
    public static void sortFilesByTime(List<File> files) {
        files.sort(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.compare(o1.lastModified(), o2.lastModified());
            }
        });
    }

    /**
     * 下载文件
     *
     * @param file     文件
     * @param response 相应
     */
    public static void downLoadFile(File file, HttpServletResponse response) {
        try {
            String fileName = file.getName();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "utf-8"));
            response.setHeader("download-filename", fileName);
            FileInputStream fis = new FileInputStream(file);
            ServletOutputStream os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            fis.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载文件，分片下载
     *
     * @param file 文件
     */
    public static ResponseEntity<Resource> downLoadRangeFile(File file, String range) {
        try {
            long fileSize = Files.size(file.toPath());
            long rangeStart = 0, rangeEnd = fileSize - 1;
            try {
                String[] splitRange = range.split("-");
                rangeStart = Long.parseLong(splitRange[0].trim());
                if (range.length() > 1) rangeEnd = Long.parseLong(splitRange[1].trim());
            } catch (Exception e) {
                log.error("分片下载获取分片失败，进行全量下载");
            }
            if (rangeStart > rangeEnd || rangeEnd >= fileSize) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            long chunkSize = rangeEnd - rangeStart + 1;
            InputStream inputStream = Files.newInputStream(file.toPath());
            inputStream.skip(rangeStart);

            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
//            httpHeaders.add(HttpHeaders.CONTENT_RANGE, "bytes " + range);
//            httpHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(httpHeaders)
                    .contentLength(chunkSize)
                    .body(new InputStreamResource(inputStream));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * 获取指定为文件夹下的所有文件夹内容
     *
     * @param folder
     */
    public static void findSubDirectories(File folder, List<File> listFile) {
        if (folder == null) return;
        File[] subDirs = folder.listFiles(File::isDirectory);
        assert subDirs != null;
        for (File subdir : subDirs) {
            if (subdir.exists() && subdir.isDirectory()) {  // 确保子文件夹实际上存在并确实是目录
                listFile.add(subdir);   // 打印子目录名
            }
        }
    }

    /**
     * 获取当前文件路径下所 指定文件名的所有文件
     *
     * @param folder
     */
    public static void findSubFile(File folder, String fileName, List<File> listFile) {
        if (folder == null) return;
        File[] subDirs = folder.listFiles();
        if (subDirs == null) return;
        for (File subdir : subDirs) {
            if (subdir.exists() && subdir.isFile() && subdir.getName().contains(fileName)) {
                listFile.add(subdir);
            }
            findSubFile(subdir, fileName, listFile);
        }
    }

    /**
     * 将指定文件复制到目标文件夹下
     *
     * @param sourceFile     指定文件信息
     * @param targetFilePath 目标文件信息
     */
    public static void copyFileToTargetFolder(File sourceFile, String targetFilePath) {
        Path sourcePath = Paths.get(sourceFile.toURI());
        File targetFile = FileUtils.createFolder(targetFilePath);
        Path targetPath = Paths.get(URI.create(targetFile.toURI() + sourceFile.getName()));


        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件复制成功！源文件：{}， 目标文件：{}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("文件复制失败！源文件：{}， 目标文件：{}，错误信息：{}", sourcePath, targetPath, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
