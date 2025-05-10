package org.example.service.state;

import org.example.service.ProductMonitor;

public interface TaskState {
    void handle(ProductMonitor monitor);
}