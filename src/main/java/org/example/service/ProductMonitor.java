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

public class ProductMonitor {
    private final Product product;
    private final List<ProductObserver> observers = new ArrayList<>();
    private volatile TaskStatus status = TaskStatus.RUNNING;
    private Thread monitoringThread;

    public ProductMonitor(String name, double targetPrice) {
        this.product = new Product(name, targetPrice);
    }

    public void addObserver(ProductObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(String message) {
        Logger.log(product.getName(), message);
        for (ProductObserver observer : observers) {
            observer.update(product, message);
        }
    }

    public Product getProduct() {
        return product;
    }

    public void startMonitoring() {
        monitoringThread = Thread.currentThread();
        product.setStatus("监控中");
        notifyObservers("开始监控商品: " + product.getName() + "，目标价格: " + product.getTargetPrice());

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
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        product.setStatus("已停止");
        notifyObservers("监控已停止");
    }

    public void stopMonitoring() {
        status = TaskStatus.STOPPED;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
    }

    private void searchAndBuy() throws Exception {
        // 搜索商品
        JSONObject searchBody = new JSONObject()
                .put("platformIds", new JSONArray())
                .put("pageNum", 1)
                .put("type", "")
                .put("search", product.getName())
                .put("isTransfer", "")
                .put("goodsTypeList", new JSONArray().put(2).put(3));

        JSONObject searchResult = HttpUtil.post(
                Config.get("SEARCH_URL"),
                searchBody,
                Config.get("TOKEN")
        );

        if (searchResult.getInt("code") != 200) {
            throw new Exception("搜索失败: " + searchResult.getString("msg"));
        }

        JSONArray items = searchResult.getJSONArray("data");
        if (items.isEmpty()) {
            notifyObservers("未找到商品");
            return;
        }

        // 更新商品信息
        JSONObject item = items.getJSONObject(0);
        product.setCurrentPrice(item.getDouble("price"));
        product.setMinPriceGoodsId(item.getLong("minPriceGoodsId"));
        product.setArchiveId(item.getLong("archiveId"));

        notifyObservers("当前价格: " + product.getCurrentPrice());

        // 检查价格
        if (product.getCurrentPrice() > product.getTargetPrice()) {
            product.setStatus("等待中");
            return;
        }

        // 尝试购买
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

    // 在 ProductMonitor 类中添加以下方法

    public TaskStatus getStatus() {
        return status;
    }

    public void updateTargetPrice(double newPrice) {
        product.setTargetPrice(newPrice);
        notifyObservers("更新目标价格为: " + newPrice);
    }

    public void showWindow() {
        // 通知观察者显示窗口
        observers.forEach(observer -> {
            if (observer instanceof ProductWindow) {
                SwingUtilities.invokeLater(() ->
                        ((ProductWindow) observer).setVisible(true));
            }
        });
    }
}