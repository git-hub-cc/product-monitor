package org.example.service.state;

import org.example.service.ProductMonitor;

public class PausedState implements TaskState {
    @Override
    public void handle(ProductMonitor monitor) {
        monitor.getProduct().setStatus("已暂停");
        monitor.notifyObservers("监控已暂停");
    }
}