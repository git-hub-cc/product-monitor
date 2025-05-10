package org.example.service;

import org.example.model.Product;
import org.example.util.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProductMonitorFactory {
    private static final Logger logger = Logger.getInstance();
    private static final Map<String, ProductMonitor> monitors = new ConcurrentHashMap<>();

    public static ProductMonitor createMonitor(String name, double price) {
        logger.log("Factory", String.format("创建监控: %s, 价格: %.2f", name, price));
        return monitors.computeIfAbsent(name,
                k -> new ProductMonitor(new Product.Builder(name)
                        .targetPrice(price)
                        .build()));
    }

    public static void removeMonitor(String name) {
        ProductMonitor monitor = monitors.remove(name);
        if (monitor != null) {
            monitor.stopMonitoring();
            logger.log("Factory", "移除监控: " + name);
        }
    }

    public static ProductMonitor getMonitor(String name) {
        return monitors.get(name);
    }

    public static Map<String, ProductMonitor> getAllMonitors() {
        return new ConcurrentHashMap<>(monitors);
    }

    public static void shutdown() {
        monitors.values().forEach(ProductMonitor::stopMonitoring);
        monitors.clear();
        logger.log("Factory", "关闭所有监控");
    }
}