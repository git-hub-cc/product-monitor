package org.example.service.strategy;

import org.example.model.Product;
import org.example.service.ProductRelease;
import org.example.util.Logger;

public class PreOrderOperation implements ProductOperation {
    private static final Logger logger = Logger.getInstance();
    private final ProductRelease productRelease;
    private final PublishMode publishMode;
    private volatile boolean isRunning = true;
    private int successCount = 0;
    private int attemptCount = 0;

    public enum PublishMode {
        SINGLE(1, "单次发布"),
        TRIPLE(3, "多次发布");

        private final int count;
        private final String description;

        PublishMode(int count, String description) {
            this.count = count;
            this.description = description;
        }

        public int getCount() { return count; }
        public String getDescription() { return description; }
    }

    public PreOrderOperation(PublishMode mode) {
        this.productRelease = new ProductRelease();
        this.publishMode = mode;
    }

    @Override
    public void execute(Product product) throws Exception {
        if (!isRunning) {
            return;
        }

        attemptCount++;
        try {
            product.setStatus(String.format("正在发布 (%d/%d)",
                    attemptCount, publishMode.getCount()));

            ProductRelease.PublishResult result = productRelease.publishPreOrder(
                    product.getName(),
                    product.getTargetPrice(),
                    convertToReleaseMode(publishMode)
            );

            handlePublishResult(product, result);

            if (attemptCount >= publishMode.getCount()) {
                completeOperation(product);
            }

        } catch (Exception e) {
            handleError(product, e);
        }
    }

    private PublishMode convertToReleaseMode(PublishMode mode) {
        return mode == PublishMode.SINGLE ? PublishMode.SINGLE : PublishMode.TRIPLE;
    }

    private void handlePublishResult(Product product, ProductRelease.PublishResult result) {
        successCount += result.getSuccessCount();

        StringBuilder status = new StringBuilder();
        status.append(String.format("发布进度: %d/%d\n", attemptCount, publishMode.getCount()));
        status.append(String.format("成功次数: %d\n", successCount));

        if (result.hasErrors()) {
            status.append("错误信息:\n");
            for (String error : result.getErrors()) {
                status.append("- ").append(error).append("\n");
            }
        }

        product.setStatus(status.toString());
        logger.log(product.getName(), status.toString());
    }

    private void handleError(Product product, Exception e) {
        String errorMsg = String.format("发布失败 (尝试 %d/%d): %s",
                attemptCount, publishMode.getCount(), e.getMessage());
        product.setStatus(errorMsg);
        logger.log(product.getName(), errorMsg, Logger.LogLevel.ERROR);
    }

    private void completeOperation(Product product) {
        isRunning = false;
        String finalStatus = String.format(
                "发布完成\n总尝试次数: %d\n成功次数: %d",
                attemptCount,
                successCount
        );
        product.setStatus(finalStatus);
        logger.log(product.getName(), finalStatus);
    }

    public void stop() {
        isRunning = false;
    }

    public String getStatistics() {
        return String.format(
                "发布统计:\n" +
                        "发布模式: %s\n" +
                        "尝试次数: %d/%d\n" +
                        "成功次数: %d\n" +
                        "成功率: %.1f%%",
                publishMode.description,
                attemptCount,
                publishMode.count,
                successCount,
                attemptCount > 0 ? (successCount * 100.0 / attemptCount) : 0
        );
    }

    // Getters
    public boolean isRunning() { return isRunning; }
    public int getSuccessCount() { return successCount; }
    public int getAttemptCount() { return attemptCount; }
    public PublishMode getPublishMode() { return publishMode; }
}