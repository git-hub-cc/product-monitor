package org.example.service;

import org.example.config.Config;
import org.example.model.Product;
import org.example.service.state.*;
import org.example.service.strategy.BuyOperation;
import org.example.service.strategy.PreOrderOperation;
import org.example.service.strategy.ProductOperation;
import org.example.ui.ProductObserver;
import org.example.ui.ProductWindow;
import org.example.util.HttpUtil;
import org.example.util.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProductMonitor {
    private static final Logger logger = Logger.getInstance();
    private final Config config = Config.getInstance();

    private final Product product;
    private final List<ProductObserver> observers;
    private volatile TaskState currentState;
    private Thread monitoringThread;
    private ProductOperation operation;
    private final AtomicBoolean running;
    private final AtomicLong startTime;
    private final AtomicLong totalRunningTime;
    private final AtomicLong checkCount;
    private final AtomicLong errorCount;
    private volatile long monitorFrequency;
    private volatile int maxRetries;
    private volatile long retryDelay;

    public ProductMonitor(Product product) {
        this.product = product;
        this.observers = new ArrayList<>();
        this.currentState = new StoppedState();
        this.running = new AtomicBoolean(false);
        this.startTime = new AtomicLong(0);
        this.totalRunningTime = new AtomicLong(0);
        this.checkCount = new AtomicLong(0);
        this.errorCount = new AtomicLong(0);
        this.monitorFrequency = config.getLong("TIME_MILLISECONDS");
        this.maxRetries = config.getInt("MAX_RETRIES");
        this.retryDelay = config.getLong("RETRY_DELAY");
    }

    public void startMonitoring() {
        if (running.compareAndSet(false, true)) {
            setState(new RunningState());
            startTime.set(System.currentTimeMillis());

            monitoringThread = new Thread(() -> {
                logger.log(product.getName(), "开始监控", Logger.LogLevel.INFO);
                while (isRunning()) {
                    try {
                        searchAndBuy();
                        checkCount.incrementAndGet();
                        TimeUnit.MILLISECONDS.sleep(monitorFrequency);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        handleError(e);
                        retryOperation();
                    }
                }
                updateTotalRunningTime();
                setState(new StoppedState());
            });

            monitoringThread.setName("Monitor-" + product.getName());
            monitoringThread.start();
        }
    }

    public void stopMonitoring() {
        if (running.compareAndSet(true, false)) {
            if (monitoringThread != null) {
                monitoringThread.interrupt();
                monitoringThread = null;
            }
            updateTotalRunningTime();
            setState(new StoppedState());
            logger.log(product.getName(), "停止监控", Logger.LogLevel.INFO);
        }
    }

    public void pauseMonitoring() {
        if (running.get()) {
            setState(new PausedState());
            updateTotalRunningTime();
        }
    }

    public void resumeMonitoring() {
        if (!running.get()) {
            startMonitoring();
        }
    }

    private void searchAndBuy() throws Exception {
        if (!isRunning()) {
            return;
        }

        SearchResult result = searchProduct();
        if (result.isEmpty()) {
            notifyObservers("未找到商品");
            return;
        }

        updateProductInfo(result.getFirstItem());

        if (shouldBuy()) {
            executePurchase();
        }
    }

    private SearchResult searchProduct() throws Exception {
        JSONObject searchBody = new JSONObject()
                .put("platformIds", new JSONArray())
                .put("pageNum", 1)
                .put("type", "")
                .put("search", product.getName())
                .put("isTransfer", "")
                .put("goodsTypeList", new JSONArray().put(2).put(3));

        JSONObject response = HttpUtil.post(
                config.get("SEARCH_URL"),
                searchBody,
                config.get("TOKEN")
        );

        if (response.getInt("code") != 200) {
            throw new MonitorException("搜索失败: " + response.getString("msg"));
        }

        return new SearchResult(response.getJSONArray("data"));
    }

    private void updateProductInfo(JSONObject item) {
        double currentPrice = item.getDouble("price");
        product.setCurrentPrice(currentPrice);
        product.setMinPriceGoodsId(item.getLong("minPriceGoodsId"));
        product.setArchiveId(item.getLong("archiveId"));
        notifyObservers(String.format("当前价格: %.2f", currentPrice));
    }

    private boolean shouldBuy() {
        if (product.getCurrentPrice() > product.getTargetPrice()) {
            product.setStatus("等待中");
            return false;
        }
        return true;
    }

    private void executePurchase() throws Exception {
        if (operation != null) {
            product.setStatus("正在执行操作");
            operation.execute(product);
        }
    }

    private void retryOperation() {
        int retryCount = 0;
        while (retryCount < maxRetries && isRunning()) {
            try {
                TimeUnit.MILLISECONDS.sleep(retryDelay);
                logger.log(product.getName(),
                        String.format("第%d次重试", retryCount + 1),
                        Logger.LogLevel.WARN);
                searchAndBuy();
                return;
            } catch (Exception e) {
                retryCount++;
                errorCount.incrementAndGet();
                logger.log(product.getName(),
                        String.format("重试失败(%d/%d): %s",
                                retryCount, maxRetries, e.getMessage()),
                        Logger.LogLevel.ERROR);
            }
        }
    }

    private void handleError(Exception e) {
        String errorMessage = String.format("监控错误: %s", e.getMessage());
        logger.log(product.getName(), errorMessage, Logger.LogLevel.ERROR);
        notifyObservers(errorMessage);
    }

    private void updateTotalRunningTime() {
        if (startTime.get() > 0) {
            long currentRunTime = System.currentTimeMillis() - startTime.get();
            totalRunningTime.addAndGet(currentRunTime);
            startTime.set(0);
        }
    }

    public void setMonitorFrequency(long frequency) {
        this.monitorFrequency = frequency;
        logger.log(product.getName(),
                String.format("更新监控频率: %d毫秒", frequency),
                Logger.LogLevel.INFO);
    }

    public void setRetryParameters(int maxRetries, long retryDelay) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        logger.log(product.getName(),
                String.format("更新重试参数: 最大重试次数=%d, 重试延迟=%d毫秒",
                        maxRetries, retryDelay),
                Logger.LogLevel.INFO);
    }

    public void setState(TaskState state) {
        this.currentState = state;
        state.handle(this);
    }

    public void setOperation(ProductOperation operation) {
        this.operation = operation;
    }

    public void updateTargetPrice(double newPrice) {
        if (newPrice > 0) {
            product.setTargetPrice(newPrice);
            notifyObservers(String.format("更新目标价格为: %.2f", newPrice));
        }
    }

    public void addObserver(ProductObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void removeObserver(ProductObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String message) {
        if (message != null) {
            for (ProductObserver observer : observers) {
                observer.update(product, message);
            }
        }
    }


    public void showWindow() {
        observers.stream()
                .filter(observer -> observer instanceof ProductWindow)
                .forEach(observer -> SwingUtilities.invokeLater(() ->
                        ((ProductWindow) observer).setVisible(true)));
    }


    public void reset() {
        stopMonitoring();
        checkCount.set(0);
        errorCount.set(0);
        totalRunningTime.set(0);
        product.setStatus("初始化");
        notifyObservers("监控器已重置");
    }

    public void setPreOrderOperation(PreOrderOperation.PublishMode mode) {
        PreOrderOperation operation = new PreOrderOperation(mode);
        setOperation(operation);
        product.setStatus("准备发布: " + mode.getDescription());
    }

    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("监控统计信息:\n");
        stats.append("商品名称: ").append(product.getName()).append("\n");
        stats.append("目标价格: ").append(String.format("%.2f", product.getTargetPrice())).append("\n");
        stats.append("当前价格: ").append(String.format("%.2f", product.getCurrentPrice())).append("\n");
        stats.append("当前状态: ").append(product.getStatus()).append("\n");
        stats.append("运行时间: ").append(formatRunningTime(getRunningTime())).append("\n");
        stats.append("检查次数: ").append(checkCount.get()).append("\n");
        stats.append("错误次数: ").append(errorCount.get()).append("\n");
        stats.append("错误率: ").append(String.format("%.2f%%", calculateErrorRate()));

        if (operation instanceof BuyOperation) {
            stats.append("\n\n").append(((BuyOperation) operation).getStatistics());
        } else if (operation instanceof PreOrderOperation) {
            stats.append("\n\n").append(((PreOrderOperation) operation).getStatistics());
        }

        return stats.toString();
    }

    private String formatRunningTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%d小时%d分钟%d秒",
                hours,
                minutes % 60,
                seconds % 60);
    }

    private double calculateErrorRate() {
        long totalChecks = checkCount.get();
        if (totalChecks == 0) {
            return 0.0;
        }
        return (errorCount.get() * 100.0) / totalChecks;
    }

    public long getRunningTime() {
        long time = totalRunningTime.get();
        if (isRunning()) {
            time += System.currentTimeMillis() - startTime.get();
        }
        return time;
    }

    // Getters
    public boolean isRunning() { return running.get(); }
    public Product getProduct() { return product; }
    public TaskState getCurrentState() { return currentState; }
    public long getMonitorFrequency() { return monitorFrequency; }
    public long getCheckCount() { return checkCount.get(); }
    public long getErrorCount() { return errorCount.get(); }

    private static class SearchResult {
        private final List<JSONObject> items;

        public SearchResult(JSONArray items) {
            this.items = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                this.items.add(items.getJSONObject(i));
            }
        }

        public List<JSONObject> getItems() {
            return new ArrayList<>(items); // 返回副本以保护内部状态
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public JSONObject getFirstItem() {
            if (isEmpty()) {
                throw new IllegalStateException("搜索结果为空");
            }
            return items.get(0);
        }

        public int size() {
            return items.size();
        }
    }
}

