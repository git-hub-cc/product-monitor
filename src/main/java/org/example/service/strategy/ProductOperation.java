package org.example.service.strategy;

import org.example.model.Product;

public interface ProductOperation {
    void execute(Product product) throws Exception;
}



