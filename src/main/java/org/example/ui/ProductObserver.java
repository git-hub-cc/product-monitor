package org.example.ui;

import org.example.model.Product;

public interface ProductObserver {
    void update(Product product, String message);
}