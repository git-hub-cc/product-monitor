package org.example.service.state;

import org.example.service.ProductMonitor;

public class StoppedState implements TaskState {
    @Override
    public void handle(ProductMonitor monitor) {
        monitor.getProduct().setStatus("已停止");
        monitor.notifyObservers("监控已停止");
    }
}