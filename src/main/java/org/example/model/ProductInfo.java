package org.example.model;

import java.util.List;

/**
 * 产品信息实体类
 * 用于存储和管理藏品相关的详细信息
 */
public class ProductInfo {
    // 藏品ID
    private long archiveId;
    // 商品总数量
    private int totalGoodsCount;
    // 平台ID
    private long platformId;
    // 平台名称
    private String platformName;
    // 平台图标URL
    private String platformIcon;
    // 藏品图片列表
    private List<String> archiveImage;
    // 藏品名称
    private String archiveName;
    // 发布时间
    private String issueTime;
    // 冷却时间（单位：秒）
    private int coolingTime;
    // 商品最低价格
    private double goodsMinPrice;
    // 是否开启定金
    private boolean isOpenEarnest;
    // 藏品销售类型
    private int archiveSalesType;
    // 在售数量
    private int sellingCount;
    // 是否开启拍卖
    private boolean isOpenAuction;
    // 是否开启求购
    private boolean isOpenWantBuy;
    // 藏品描述
    private String archiveDescription;

    /**
     * 无参构造函数
     */
    public ProductInfo() {
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
     * 获取商品总数量
     * @return 商品总数量
     */
    public int getTotalGoodsCount() {
        return totalGoodsCount;
    }

    /**
     * 设置商品总数量
     * @param totalGoodsCount 商品总数量
     */
    public void setTotalGoodsCount(int totalGoodsCount) {
        this.totalGoodsCount = totalGoodsCount;
    }

    /**
     * 获取平台ID
     * @return 平台ID
     */
    public long getPlatformId() {
        return platformId;
    }

    /**
     * 设置平台ID
     * @param platformId 平台ID
     */
    public void setPlatformId(long platformId) {
        this.platformId = platformId;
    }

    /**
     * 获取平台名称
     * @return 平台名称
     */
    public String getPlatformName() {
        return platformName;
    }

    /**
     * 设置平台名称
     * @param platformName 平台名称
     */
    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    /**
     * 获取平台图标
     * @return 平台图标URL
     */
    public String getPlatformIcon() {
        return platformIcon;
    }

    /**
     * 设置平台图标
     * @param platformIcon 平台图标URL
     */
    public void setPlatformIcon(String platformIcon) {
        this.platformIcon = platformIcon;
    }

    /**
     * 获取藏品图片列表
     * @return 藏品图片URL列表
     */
    public List<String> getArchiveImage() {
        return archiveImage;
    }

    /**
     * 设置藏品图片列表
     * @param archiveImage 藏品图片URL列表
     */
    public void setArchiveImage(List<String> archiveImage) {
        this.archiveImage = archiveImage;
    }

    /**
     * 获取藏品名称
     * @return 藏品名称
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * 设置藏品名称
     * @param archiveName 藏品名称
     */
    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    /**
     * 获取发布时间
     * @return 发布时间
     */
    public String getIssueTime() {
        return issueTime;
    }

    /**
     * 设置发布时间
     * @param issueTime 发布时间
     */
    public void setIssueTime(String issueTime) {
        this.issueTime = issueTime;
    }

    /**
     * 获取冷却时间
     * @return 冷却时间（秒）
     */
    public int getCoolingTime() {
        return coolingTime;
    }

    /**
     * 设置冷却时间
     * @param coolingTime 冷却时间（秒）
     */
    public void setCoolingTime(int coolingTime) {
        this.coolingTime = coolingTime;
    }

    /**
     * 获取商品最低价格
     * @return 商品最低价格
     */
    public double getGoodsMinPrice() {
        return goodsMinPrice;
    }

    /**
     * 设置商品最低价格
     * @param goodsMinPrice 商品最低价格
     */
    public void setGoodsMinPrice(double goodsMinPrice) {
        this.goodsMinPrice = goodsMinPrice;
    }

    /**
     * 判断是否开启定金
     * @return 是否开启定金
     */
    public boolean isOpenEarnest() {
        return isOpenEarnest;
    }

    /**
     * 设置是否开启定金
     * @param openEarnest 是否开启定金
     */
    public void setOpenEarnest(boolean openEarnest) {
        this.isOpenEarnest = openEarnest;
    }

    /**
     * 获取藏品销售类型
     * @return 藏品销售类型
     */
    public int getArchiveSalesType() {
        return archiveSalesType;
    }

    /**
     * 设置藏品销售类型
     * @param archiveSalesType 藏品销售类型
     */
    public void setArchiveSalesType(int archiveSalesType) {
        this.archiveSalesType = archiveSalesType;
    }

    /**
     * 获取在售数量
     * @return 在售数量
     */
    public int getSellingCount() {
        return sellingCount;
    }

    /**
     * 设置在售数量
     * @param sellingCount 在售数量
     */
    public void setSellingCount(int sellingCount) {
        this.sellingCount = sellingCount;
    }

    /**
     * 判断是否开启拍卖
     * @return 是否开启拍卖
     */
    public boolean isOpenAuction() {
        return isOpenAuction;
    }

    /**
     * 设置是否开启拍卖
     * @param openAuction 是否开启拍卖
     */
    public void setOpenAuction(boolean openAuction) {
        this.isOpenAuction = openAuction;
    }

    /**
     * 判断是否开启求购
     * @return 是否开启求购
     */
    public boolean isOpenWantBuy() {
        return isOpenWantBuy;
    }

    /**
     * 设置是否开启求购
     * @param openWantBuy 是否开启求购
     */
    public void setOpenWantBuy(boolean openWantBuy) {
        this.isOpenWantBuy = openWantBuy;
    }

    /**
     * 获取藏品描述
     * @return 藏品描述
     */
    public String getArchiveDescription() {
        return archiveDescription;
    }

    /**
     * 设置藏品描述
     * @param archiveDescription 藏品描述
     */
    public void setArchiveDescription(String archiveDescription) {
        this.archiveDescription = archiveDescription;
    }
}