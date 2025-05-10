package org.example.util;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Logger {
    private static volatile Logger instance;
    private static final String LOG_DIR = "logs";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int MAX_LOG_FILES = 7; // 保留最近7天的日志

    private final BlockingQueue<LogEntry> logQueue;
    private final Thread loggerThread;
    private volatile boolean running;

    private static final int DEFAULT_LOG_BUFFER_SIZE = 1000; // 日志缓冲区大小
    private final Map<String, Deque<LogEntry>> logBuffers = new ConcurrentHashMap<>();
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();

    public interface LogListener {
        void onNewLog(String source, String message, LogLevel level, Date timestamp);
    }

    public void addListener(LogListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    public void log(String source, String message, LogLevel level) {
        LogEntry entry = new LogEntry(source, message, level);

        // 添加到缓冲区
        logBuffers.computeIfAbsent(source, k -> new LinkedBlockingDeque<>(DEFAULT_LOG_BUFFER_SIZE))
                .offerLast(entry);

        // 通知所有监听器
        for (LogListener listener : listeners) {
            listener.onNewLog(source, message, level, new Date(entry.timestamp));
        }

        // 写入队列
        try {
            logQueue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 获取指定来源的历史日志
    public List<LogEntry> getHistoryLogs(String source) {
        Deque<LogEntry> buffer = logBuffers.get(source);
        return buffer != null ? new ArrayList<>(buffer) : new ArrayList<>();
    }

    private Logger() {
        this.logQueue = new LinkedBlockingQueue<>();
        this.running = true;
        this.loggerThread = new Thread(this::processLogQueue);
        this.loggerThread.setDaemon(true);
        this.loggerThread.start();

        createLogDirectory();
        cleanOldLogs();
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    private void createLogDirectory() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("无法创建日志目录: " + e.getMessage());
        }
    }

    public void log(String productName, String message) {
        log(productName, message, LogLevel.INFO);
    }

    private void processLogQueue() {
        while (running || !logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.take();
                writeLog(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void writeLog(LogEntry entry) {
        String fileName = getLogFileName(entry.productName);
        String logMessage = formatLogMessage(entry);

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(fileName),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(logMessage);
            writer.newLine();

            // 同时输出到控制台
            System.out.println(logMessage);
        } catch (IOException e) {
            System.err.println("写入日志失败: " + e.getMessage());
        }
    }

    private String getLogFileName(String productName) {
        String safeProductName = sanitizeFileName(productName);
        return String.format("%s/%s_%s.log",
                LOG_DIR,
                safeProductName,
                DATE_FORMAT.format(new Date()));
    }

    private String formatLogMessage(LogEntry entry) {
        return String.format("[%s][%s] %s",
                TIME_FORMAT.format(new Date()),
                entry.level,
                entry.message);
    }

    private String sanitizeFileName(String fileName) {
        return fileName == null ? "unknown" :
                fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public void cleanOldLogs() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                return;
            }

            List<Path> logFiles = Files.list(logDir)
                    .filter(path -> path.toString().endsWith(".log"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2)
                                    .compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .collect(Collectors.toList());

            if (logFiles.size() > MAX_LOG_FILES) {
                for (int i = MAX_LOG_FILES; i < logFiles.size(); i++) {
                    Files.delete(logFiles.get(i));
                }
            }
        } catch (IOException e) {
            System.err.println("清理日志文件失败: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        loggerThread.interrupt();
        try {
            loggerThread.join(5000); // 等待最多5秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class LogEntry {
        final String productName;
        final String message;
        final LogLevel level;
        final long timestamp;

        LogEntry(String productName, String message, LogLevel level) {
            this.productName = productName;
            this.message = message;
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum LogLevel {
        DEBUG("调试"),
        INFO("信息"),
        WARN("警告"),
        ERROR("错误");

        private final String description;

        LogLevel(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}