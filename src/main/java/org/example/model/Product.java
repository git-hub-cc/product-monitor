package org.example.model;

public class Product {
    private final String name;
    private double targetPrice;
    private double currentPrice;
    private double avgHistoryPrice;
    private String status;
    private long minPriceGoodsId;
    private long archiveId;

    private Product(Builder builder) {
        this.name = builder.name;
        this.targetPrice = builder.targetPrice;
        this.status = "初始化";
    }

    public static class Builder {
        private final String name;
        private double targetPrice;

        public Builder(String name) {
            this.name = name;
        }

        public Builder targetPrice(double price) {
            this.targetPrice = price;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }

    // Getters and setters
    public String getName() { return name; }
    public double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getMinPriceGoodsId() { return minPriceGoodsId; }
    public void setMinPriceGoodsId(long minPriceGoodsId) { this.minPriceGoodsId = minPriceGoodsId; }
    public long getArchiveId() { return archiveId; }
    public void setArchiveId(long archiveId) { this.archiveId = archiveId; }
}