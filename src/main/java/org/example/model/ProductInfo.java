package org.example.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProductInfo {
    private long archiveId;
    private int totalGoodsCount;
    private long platformId;
    private String platformName;
    private String platformIcon;
    private List<String> archiveImage;
    private String archiveName;
    private String issueTime;
    private int coolingTime;
    private double goodsMinPrice;
    private boolean isOpenEarnest;
    private int archiveSalesType;
    private int sellingCount;
    private boolean isOpenAuction;
    private boolean isOpenWantBuy;
    private String archiveDescription;

    public ProductInfo() {
        this.archiveImage = new ArrayList<>();
    }

    // Getters and setters
    public long getArchiveId() { return archiveId; }
    public void setArchiveId(long archiveId) { this.archiveId = archiveId; }
    public int getTotalGoodsCount() { return totalGoodsCount; }
    public void setTotalGoodsCount(int totalGoodsCount) { this.totalGoodsCount = totalGoodsCount; }
    public long getPlatformId() { return platformId; }
    public void setPlatformId(long platformId) { this.platformId = platformId; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public String getPlatformIcon() { return platformIcon; }
    public void setPlatformIcon(String platformIcon) { this.platformIcon = platformIcon; }
    public List<String> getArchiveImage() { return archiveImage; }
    public void setArchiveImage(List<String> archiveImage) { this.archiveImage = archiveImage; }
    public String getArchiveName() { return archiveName; }
    public void setArchiveName(String archiveName) { this.archiveName = archiveName; }
    public String getIssueTime() { return issueTime; }
    public void setIssueTime(String issueTime) { this.issueTime = issueTime; }
    public int getCoolingTime() { return coolingTime; }
    public void setCoolingTime(int coolingTime) { this.coolingTime = coolingTime; }
    public double getGoodsMinPrice() { return goodsMinPrice; }
    public void setGoodsMinPrice(double goodsMinPrice) { this.goodsMinPrice = goodsMinPrice; }
    public boolean isOpenEarnest() { return isOpenEarnest; }
    public void setOpenEarnest(boolean openEarnest) { isOpenEarnest = openEarnest; }
    public int getArchiveSalesType() { return archiveSalesType; }
    public void setArchiveSalesType(int archiveSalesType) { this.archiveSalesType = archiveSalesType; }
    public int getSellingCount() { return sellingCount; }
    public void setSellingCount(int sellingCount) { this.sellingCount = sellingCount; }
    public boolean isOpenAuction() { return isOpenAuction; }
    public void setOpenAuction(boolean openAuction) { isOpenAuction = openAuction; }
    public boolean isOpenWantBuy() { return isOpenWantBuy; }
    public void setOpenWantBuy(boolean openWantBuy) { isOpenWantBuy = openWantBuy; }
    public String getArchiveDescription() { return archiveDescription; }
    public void setArchiveDescription(String archiveDescription) { this.archiveDescription = archiveDescription; }

    public static ProductInfo fromJson(JSONObject json) {
        ProductInfo info = new ProductInfo();

        info.setArchiveId(json.optLong("archiveId"));
        info.setTotalGoodsCount(json.optInt("totalGoodsCount"));
        info.setPlatformId(json.optLong("platformId"));
        info.setPlatformName(json.optString("platformName"));
        info.setPlatformIcon(json.optString("platformIcon"));
        info.setArchiveName(json.optString("archiveName"));
        info.setIssueTime(json.optString("issueTime"));
        info.setCoolingTime(json.optInt("coolingTime"));
        info.setGoodsMinPrice(json.optDouble("goodsMinPrice"));
        info.setOpenEarnest(json.optBoolean("isOpenEarnest"));
        info.setArchiveSalesType(json.optInt("archiveSalesType"));
        info.setSellingCount(json.optInt("sellingCount"));
        info.setOpenAuction(json.optBoolean("isOpenAuction"));
        info.setOpenWantBuy(json.optBoolean("isOpenWantBuy"));
        info.setArchiveDescription(json.optString("archiveDescription"));

        JSONArray imageArray = json.optJSONArray("archiveImage");
        if (imageArray != null) {
            List<String> images = new ArrayList<>();
            for (int i = 0; i < imageArray.length(); i++) {
                images.add(imageArray.optString(i));
            }
            info.setArchiveImage(images);
        }

        return info;
    }
}