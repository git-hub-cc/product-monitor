package org.example.service;

import org.example.config.Config;
import org.example.model.Product;
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

/**
 * 商品监控类
 * 负责监控商品价格并在达到目标价格时自动购买
 */
public class ProductMonitor {
    // 被监控的商品
    private final Product product;
    // 观察者列表，用于通知UI更新
    private final List<ProductObserver> observers = new ArrayList<>();
    // 监控任务状态
    private volatile TaskStatus status = TaskStatus.RUNNING;
    // 监控线程
    private Thread monitoringThread;

    /**
     * 构造函数
     * @param name 商品名称
     * @param targetPrice 目标价格
     */
    public ProductMonitor(String name, double targetPrice) {
        this.product = new Product(name, targetPrice);
    }

    /**
     * 添加观察者
     * @param observer 观察者对象
     */
    public void addObserver(ProductObserver observer) {
        if (observer != null) {  // 添加空值检查
            observers.add(observer);
        }
    }

    /**
     * 通知所有观察者
     * @param message 通知消息
     */
    public void notifyObservers(String message) {
        if (message != null) {  // 添加空值检查
            Logger.log(product.getName(), message);
            for (ProductObserver observer : observers) {
                observer.update(product, message);
            }
        }
    }

    /**
     * 获取商品信息
     * @return 商品对象
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 开始监控商品
     */
    public void startMonitoring() {
        monitoringThread = Thread.currentThread();
        product.setStatus("监控中");
        notifyObservers(String.format("开始监控商品: %s，目标价格: %.2f", product.getName(), product.getTargetPrice()));

        while (status == TaskStatus.RUNNING) {
            try {
                searchAndBuy();
                if (status != TaskStatus.RUNNING) break;

                TimeUnit.MILLISECONDS.sleep(Config.getLong("TIME_MILLISECONDS"));
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                notifyObservers("发生错误: " + e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(3);  // 错误发生后等待3秒再重试
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        product.setStatus("已停止");
        notifyObservers("监控已停止");
    }

    /**
     * 停止监控
     */
    public void stopMonitoring() {
        status = TaskStatus.STOPPED;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
    }

    /**
     * 搜索商品并尝试购买
     * @throws Exception 当搜索或购买过程中发生错误时抛出
     */
    private void searchAndBuy() throws Exception {
        // 构建搜索请求体
        JSONObject searchBody = buildSearchRequest();

        // 执行搜索请求
        JSONObject searchResult = HttpUtil.post(
                Config.get("SEARCH_URL"),
                searchBody,
                Config.get("TOKEN")
        );

        // 检查搜索结果
        validateSearchResult(searchResult);

        JSONArray items = searchResult.getJSONArray("data");
        if (items.isEmpty()) {
            notifyObservers("未找到商品");
            return;
        }

        // 更新商品信息
        updateProductInfo(items.getJSONObject(0));

        // 检查价格是否满足购买条件
        if (!shouldBuy()) {
            return;
        }

        // 执行购买操作
        executePurchase();
    }

    /**
     * 构建搜索请求体
     */
    private JSONObject buildSearchRequest() {
        return new JSONObject()
                .put("platformIds", new JSONArray())
                .put("pageNum", 1)
                .put("type", "")
                .put("search", product.getName())
                .put("isTransfer", "")
                .put("goodsTypeList", new JSONArray().put(2).put(3));
    }

    /**
     * 验证搜索结果
     */
    private void validateSearchResult(JSONObject searchResult) throws Exception {
        if (searchResult.getInt("code") != 200) {
            throw new Exception("搜索失败: " + searchResult.getString("msg"));
        }
    }

    /**
     * 更新商品信息
     */
    private void updateProductInfo(JSONObject item) {
        product.setCurrentPrice(item.getDouble("price"));
        product.setMinPriceGoodsId(item.getLong("minPriceGoodsId"));
        product.setArchiveId(item.getLong("archiveId"));
        notifyObservers(String.format("当前价格: %.2f", product.getCurrentPrice()));
    }

    /**
     * 检查是否应该购买
     */
    private boolean shouldBuy() {
        if (product.getCurrentPrice() > product.getTargetPrice()) {
            product.setStatus("等待中");
            return false;
        }
        return true;
    }

    /**
     * 执行购买操作
     */
    private void executePurchase() throws Exception {
        product.setStatus("正在下单");
        JSONObject orderBody = new JSONObject()
                .put("addressId", Config.getInt("ADDRESS_ID"))
                .put("goodsId", product.getMinPriceGoodsId())
                .put("shortName", Config.get("SHORT_NAME"))
                .put("devType", Config.getInt("DEV_TYPE"));

        JSONObject orderResult = HttpUtil.post(
                Config.get("ORDER_URL"),
                orderBody,
                Config.get("TOKEN")
        );

        handleOrderResult(orderResult);
    }

    /**
     * 处理订单结果
     */
    private void handleOrderResult(JSONObject orderResult) {
        if (orderResult.getInt("code") == 200) {
            String orderNo = orderResult.getJSONObject("data").optString("orderNo", "未知订单号");
            product.setStatus("下单成功");
            notifyObservers("🎉 下单成功，订单号: " + orderNo);
            stopMonitoring();
        } else {
            product.setStatus("下单失败");
            notifyObservers("下单失败: " + orderResult.getString("msg"));
        }
    }

    /**
     * 获取当前监控状态
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * 更新目标价格
     * @param newPrice 新的目标价格
     */
    public void updateTargetPrice(double newPrice) {
        if (newPrice > 0) {  // 添加价格有效性检查
            product.setTargetPrice(newPrice);
            notifyObservers(String.format("更新目标价格为: %.2f", newPrice));
        }
    }

    /**
     * 显示监控窗口
     */
    public void showWindow() {
        observers.stream()
                .filter(observer -> observer instanceof ProductWindow)
                .forEach(observer -> SwingUtilities.invokeLater(() ->
                        ((ProductWindow) observer).setVisible(true)));
    }
}