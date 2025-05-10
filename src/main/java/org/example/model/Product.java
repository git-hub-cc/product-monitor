package org.example.model;

public class Product {
    private final String name;
    private double targetPrice;
    private double currentPrice;
    private double avgHistoryPrice;
    private String status;
    private long minPriceGoodsId;
    private long archiveId;

    public Product(String name, double targetPrice) {
        this.name = name;
        this.targetPrice = targetPrice;
        this.status = "初始化";
    }

    public String getName() {
        return name;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getAvgHistoryPrice() {
        return avgHistoryPrice;
    }

    public void setAvgHistoryPrice(double avgHistoryPrice) {
        this.avgHistoryPrice = avgHistoryPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getMinPriceGoodsId() {
        return minPriceGoodsId;
    }

    public void setMinPriceGoodsId(long minPriceGoodsId) {
        this.minPriceGoodsId = minPriceGoodsId;
    }

    public long getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(long archiveId) {
        this.archiveId = archiveId;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }
}