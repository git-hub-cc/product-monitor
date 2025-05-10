package org.example.model;

public class ProductListItem {
    private final String name;
    private final double targetPrice;

    public ProductListItem(String name, double targetPrice) {
        this.name = name;
        this.targetPrice = targetPrice;
    }

    public String getName() {
        return name;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    @Override
    public String toString() {
        return String.format("%s - 目标价格: %.2f", name, targetPrice);
    }
}
