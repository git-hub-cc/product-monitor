package org.example.ui;

import org.example.model.Product;

public interface ProductObserver {
    /**
     * 更新观察者
     * @param product 商品信息
     * @param message 日志消息
     */
    void update(Product product, String message);
}