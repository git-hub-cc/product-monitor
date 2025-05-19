package org.example.service.strategy;

import org.example.config.Config;
import org.example.model.Product;
import org.example.ui.MainWindow;
import org.example.util.HttpUtil;
import org.example.util.Logger;
import org.json.JSONObject;

import java.awt.*;

public class BuyOperation implements ProductOperation {
    private static final Logger logger = Logger.getInstance();
    private final Config config = Config.getInstance();

    private final boolean continuousBuying;
    private boolean hasBought = false;
    private int successCount = 0;
    private int failureCount = 0;
    private double totalSpent = 0.0;

    public BuyOperation(boolean continuousBuying) {
        this.continuousBuying = continuousBuying;
    }
    private String getBuyingModeDescription() {
        return continuousBuying ? "持续购买" : "单次购买";
    }

    @Override
    public void execute(Product product) throws Exception {
        if (!continuousBuying && hasBought) {
            logger.log(product.getName(),
                    "单次购买已完成，不再继续购买",
                    Logger.LogLevel.INFO);
            return;
        }

        try {
            logger.log(product.getName(),
                    String.format("执行%s操作", getBuyingModeDescription()),
                    Logger.LogLevel.INFO);
            executePurchase(product);
        } catch (Exception e) {
            failureCount++;
            String errorMsg = String.format("%s失败 (第%d次失败): %s",
                    getBuyingModeDescription(), failureCount, e.getMessage());
            logger.log(product.getName(), errorMsg, Logger.LogLevel.ERROR);
            throw e;
        }
    }

    private void handleSuccessfulPurchase(JSONObject response, Product product) {
        successCount++;
        hasBought = true;
        totalSpent += product.getCurrentPrice();

        String orderNo = response.getJSONObject("data")
                .optString("orderNo", "未知订单号");

        String successMsg = String.format(
                "%s成功！\n" +
                        "订单号: %s\n" +
                        "购买价格: %.2f\n" +
                        "累计购买: %d次\n" +
                        "总花费: %.2f",
                getBuyingModeDescription(),
                orderNo,
                product.getCurrentPrice(),
                successCount,
                totalSpent
        );

        logger.log(product.getName(), successMsg, Logger.LogLevel.INFO);
        product.setStatus(getBuyingModeDescription() + "成功");

        if (!continuousBuying) {
            logger.log(product.getName(), "单次购买完成，停止监控", Logger.LogLevel.INFO);
        }
    }

    public String getStatistics() {
        return String.format(
                "购买统计:\n" +
                        "模式: %s\n" +
                        "成功次数: %d\n" +
                        "失败次数: %d\n" +
                        "总花费: %.2f\n" +
                        "平均价格: %.2f\n" +
                        "状态: %s",
                getBuyingModeDescription(),
                successCount,
                failureCount,
                totalSpent,
                successCount > 0 ? totalSpent / successCount : 0,
                hasBought && !continuousBuying ? "已完成" : "进行中"
        );
    }


    private void executePurchase(Product product) throws Exception {
        logPurchaseAttempt(product);

        JSONObject orderBody = new JSONObject()
                .put("addressId", getSelectedAddressId())
                .put("goodsId", product.getMinPriceGoodsId())
                .put("shortName", config.get("SHORT_NAME"))
                .put("devType", config.getInt("DEV_TYPE"));

        JSONObject response = HttpUtil.post(
                config.get("ORDER_URL"),
                orderBody,
                config.get("TOKEN")
        );
        Toolkit.getDefaultToolkit().beep();

        handlePurchaseResponse(response, product);
    }

    private long getSelectedAddressId() {
        if (MainWindow.addressComboBox == null ||
                MainWindow.addressComboBox.getSelectedItem() == null) {
            throw new IllegalStateException("未选择收货地址");
        }
        return MainWindow.addressComboBox.getItemAt(
                MainWindow.addressComboBox.getSelectedIndex()).getId();
    }

    private void logPurchaseAttempt(Product product) {
        String attemptMsg = String.format(
                "尝试购买商品: %s\n" +
                        "当前价格: %.2f\n" +
                        "目标价格: %.2f\n" +
                        "商品ID: %d\n" +
                        "已成功次数: %d\n" +
                        "总花费: %.2f",
                product.getName(),
                product.getCurrentPrice(),
                product.getTargetPrice(),
                product.getMinPriceGoodsId(),
                successCount,
                totalSpent
        );
        logger.log(product.getName(), attemptMsg, Logger.LogLevel.INFO);
    }

    private void handlePurchaseResponse(JSONObject response, Product product) throws Exception {
        if (response.getInt("code") == 200) {
            handleSuccessfulPurchase(response, product);
        } else {
            handleFailedPurchase(response, product);
        }
    }

    private void handleFailedPurchase(JSONObject response, Product product) throws Exception {
        failureCount++;
        String errorMsg = String.format(
                "购买失败 (第%d次)\n" +
                        "错误代码: %d\n" +
                        "错误信息: %s",
                failureCount,
                response.getInt("code"),
                response.getString("msg")
        );

        logger.log(product.getName(), errorMsg, Logger.LogLevel.ERROR);
        product.setStatus("购买失败");
        throw new Exception(response.getString("msg"));
    }


    public void reset() {
        hasBought = false;
        successCount = 0;
        failureCount = 0;
        totalSpent = 0.0;
    }

    // Getters
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public double getTotalSpent() { return totalSpent; }
    public boolean isContinuousBuying() { return continuousBuying; }
}