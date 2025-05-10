package org.example.service.state;

import org.example.service.ProductMonitor;

public class RunningState implements TaskState {
    @Override
    public void handle(ProductMonitor monitor) {
        monitor.getProduct().setStatus("监控中");
        monitor.notifyObservers("开始监控商品");
    }
}