package org.example.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 简单的日志记录工具类
 * 用于将日志信息写入文件并同时输出到控制台
 */
public class Logger {
    // 日期格式化对象，用于生成日志文件名
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    // 时间格式化对象，用于日志消息的时间戳
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    // 日志文件存放的根目录
    private static final String LOG_DIR = "logs";

    /**
     * 记录日志信息
     * @param productName 产品名称，用于生成日志文件名
     * @param message 需要记录的日志消息
     */
    public static synchronized void log(String productName, String message) {
        // 安全处理文件名，移除不合法字符
        String safeProductName = sanitizeFileName(productName);
        String fileName = LOG_DIR + File.separator +
                safeProductName + "_" +
                DATE_FORMAT.format(new Date()) + ".log";

        // 确保日志目录存在
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("无法创建日志目录: " + e.getMessage());
            return;
        }

        // 构建日志消息，包含时间戳
        String logMessage = String.format("[%s] %s",
                TIME_FORMAT.format(new Date()),
                message);

        // 写入日志文件并输出到控制台
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(logMessage);
            System.out.println(logMessage);

        } catch (IOException e) {
            System.err.println("写入日志失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理文件名中的非法字符
     * @param fileName 原始文件名
     * @return 处理后的安全文件名
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        // 替换文件名中的非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}