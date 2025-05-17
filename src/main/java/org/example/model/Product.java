package org.example.model;

/**
 * 商品实体类
 * 用于存储和管理商品的基本信息、价格信息及状态
 */
public class Product {
    // 商品名称
    private final String name;
    // 目标价格
    private double targetPrice;
    // 当前价格
    private double currentPrice;
    // 历史平均价格
    private double avgHistoryPrice;
    // 商品状态
    private String status;
    // 最低价格商品ID
    private long minPriceGoodsId;
    // 藏品ID
    private long archiveId;

    /**
     * 带参构造函数
     * @param name 商品名称
     * @param targetPrice 目标价格
     */
    public Product(String name, double targetPrice) {
        this.name = name;
        this.targetPrice = targetPrice;
        this.status = "初始化";
    }

    /**
     * 获取商品名称
     * @return 商品名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取目标价格
     * @return 目标价格
     */
    public double getTargetPrice() {
        return targetPrice;
    }

    /**
     * 获取当前价格
     * @return 当前价格
     */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /**
     * 设置当前价格
     * @param currentPrice 当前价格
     */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    /**
     * 获取历史平均价格
     * @return 历史平均价格
     */
    public double getAvgHistoryPrice() {
        return avgHistoryPrice;
    }

    /**
     * 设置历史平均价格
     * @param avgHistoryPrice 历史平均价格
     */
    public void setAvgHistoryPrice(double avgHistoryPrice) {
        this.avgHistoryPrice = avgHistoryPrice;
    }

    /**
     * 获取商品状态
     * @return 商品状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置商品状态
     * @param status 商品状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取最低价格商品ID
     * @return 最低价格商品ID
     */
    public long getMinPriceGoodsId() {
        return minPriceGoodsId;
    }

    /**
     * 设置最低价格商品ID
     * @param minPriceGoodsId 最低价格商品ID
     */
    public void setMinPriceGoodsId(long minPriceGoodsId) {
        this.minPriceGoodsId = minPriceGoodsId;
    }

    /**
     * 获取藏品ID
     * @return 藏品ID
     */
    public long getArchiveId() {
        return archiveId;
    }

    /**
     * 设置藏品ID
     * @param archiveId 藏品ID
     */
    public void setArchiveId(long archiveId) {
        this.archiveId = archiveId;
    }

    /**
     * 设置目标价格
     * @param targetPrice 目标价格
     */
    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }
}