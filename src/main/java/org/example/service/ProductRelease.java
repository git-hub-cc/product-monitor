package org.example.service;

import org.example.config.Config;
import org.example.model.ProductInfo;
import org.example.service.strategy.PreOrderOperation;
import org.example.util.HttpUtil;
import org.example.util.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductRelease {
    private static final Logger logger = Logger.getInstance();
    private final Config config = Config.getInstance();
    private static final int SUCCESS_CODE = 200;
    private static final double SERVICE_CHARGE_RATE = 0.04;
    private static final long PUBLISH_INTERVAL = 12000;

    public static class PublishResult {
        private int successCount = 0;
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public int getSuccessCount() { return successCount; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    public PublishResult publishPreOrder(String name, double price, PreOrderOperation.PublishMode mode) {
        PublishResult result = new PublishResult();

        try {
            String archiveId = searchArchive(name);
            if (archiveId == null) {
                throw new Exception("未找到相关藏品");
            }
            ProductInfo productInfo = getProductInfo(archiveId);

            for (int i = 0; i < mode.getCount(); i++) {
                try {
                    if (i > 0) {
                        logger.log("PreOrder", String.format("等待%d秒后进行第%d次发布...",
                                PUBLISH_INTERVAL/1000, i + 1));
                        Thread.sleep(PUBLISH_INTERVAL);
                    }

                    boolean success = executePublish(productInfo, price);
                    if (success) {
                        result.successCount++;
                        logger.log("PreOrder", String.format("第%d次发布成功", i + 1));
                    }
                } catch (Exception e) {
                    result.addError(String.format("第%d次发布失败: %s", i + 1, e.getMessage()));
                    logger.log("PreOrder", String.format("第%d次发布失败: %s", i + 1,
                            e.getMessage()), Logger.LogLevel.ERROR);
                }
            }
        } catch (Exception e) {
            result.addError("初始化失败: " + e.getMessage());
            logger.log("PreOrder", "发布初始化失败: " + e.getMessage(), Logger.LogLevel.ERROR);
        }

        return result;
    }

    private boolean executePublish(ProductInfo info, double price) throws Exception {
        String preOrderKey = createPreOrder(info, price);
        if (preOrderKey == null) {
            throw new Exception("创建预购失败");
        }
        return confirmPreOrder(preOrderKey, info, price);
    }

    private String searchArchive(String name) throws Exception {
        JSONObject searchBody = new JSONObject()
                .put("platformIds", new JSONArray())
                .put("pageNum", 1)
                .put("type", "")
                .put("search", name)
                .put("isTransfer", "")
                .put("goodsTypeList", new JSONArray().put(2).put(3));

        JSONObject response = HttpUtil.post(
                config.get("SEARCH_URL"),
                searchBody,
                config.get("TOKEN")
        );

        validateResponse(response, "搜索失败");

        JSONArray items = response.getJSONArray("data");
        return items.isEmpty() ? null :
                String.valueOf(items.getJSONObject(0).getLong("archiveId"));
    }

    private ProductInfo getProductInfo(String archiveId) throws Exception {
        JSONObject requestBody = new JSONObject()
                .put("archiveId", archiveId)
                .put("platformId", config.get("PLATFORM_ID"));

        JSONObject response = HttpUtil.post(
                config.get("ARCHIVE_URL"),
                requestBody,
                config.get("TOKEN")
        );

        validateResponse(response, "获取商品信息失败");
        return ProductInfo.fromJson(response.getJSONObject("data"));
    }

    private String createPreOrder(ProductInfo info, double price) throws Exception {
        PreOrderRequestBuilder builder = new PreOrderRequestBuilder(info, price);
        JSONObject requestBody = builder.build();

        JSONObject response = HttpUtil.post(
                config.get("PRE_CREATE_URL"),
                requestBody,
                config.get("TOKEN")
        );

        validateResponse(response, "创建预购订单失败");
        return response.getJSONObject("data").getString("key");
    }

    private boolean confirmPreOrder(String preOrderKey, ProductInfo info, double price) throws Exception {
        JSONObject createResponse = createProduct(preOrderKey, info, price);
        validateResponse(createResponse, "创建商品失败");

        long goodsId = createResponse.getJSONObject("data").getLong("goodsId");
        return confirmPayment(goodsId);
    }

    private JSONObject createProduct(String preOrderKey, ProductInfo info, double price) throws Exception {
        PreOrderRequestBuilder builder = new PreOrderRequestBuilder(info, price);
        JSONObject createBody = builder.build()
                .put("key", preOrderKey);

        return HttpUtil.post(
                config.get("CREATE_URL"),
                createBody,
                config.get("TOKEN")
        );
    }

    private boolean confirmPayment(long goodsId) throws Exception {
        JSONObject payBody = new JSONObject()
                .put("goodsId", goodsId)
                .put("shortName", config.get("SHORT_NAME"))
                .put("devType", config.get("DEV_TYPE"));

        JSONObject response = HttpUtil.post(
                config.get("UNIFIED_PAY_URL"),
                payBody,
                config.get("TOKEN")
        );

        validateResponse(response, "支付确认失败");
        return true;
    }

    private void validateResponse(JSONObject response, String errorMessage) throws Exception {
        if (response.getInt("code") != SUCCESS_CODE) {
            String msg = response.getString("msg");
            logger.log("PreOrder", errorMessage + ": " + msg, Logger.LogLevel.ERROR);
            throw new Exception(errorMessage + ": " + msg);
        }
    }

    private static class PreOrderRequestBuilder {
        private final ProductInfo info;
        private final double price;
        private final double serviceCharge;
        private final double incomeAmount;
        private final long auctionEndTime;

        public PreOrderRequestBuilder(ProductInfo info, double price) {
            this.info = info;
            this.price = price;
            this.serviceCharge = calculateServiceCharge(price);
            this.incomeAmount = calculateIncomeAmount(price, serviceCharge);
            this.auctionEndTime = calculateAuctionEndTime(info.getIssueTime());
        }

        private double calculateServiceCharge(double price) {
            return Math.round(price * SERVICE_CHARGE_RATE * 100.0) / 100.0;
        }

        private double calculateIncomeAmount(double price, double serviceCharge) {
            return Math.round((price - serviceCharge) * 100.0) / 100.0;
        }

        private long calculateAuctionEndTime(String issueTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(issueTime, formatter);
            LocalDateTime delayedTime = dateTime.plusHours(
                    Integer.parseInt(Config.getInstance().get("DELAY_HOURS"))
            );
            return delayedTime.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli() / 1000;
        }

        public JSONObject build() {
            return new JSONObject()
                    .put("name", info.getArchiveName())
                    .put("introduce", "请自行描述")
                    .put("platformId", String.valueOf(info.getPlatformId()))
                    .put("platformName", info.getPlatformName())
                    .put("archiveId", String.valueOf(info.getArchiveId()))
                    .put("issueDate", info.getIssueTime())
                    .put("presellDate", info.getIssueTime())
                    .put("auctionEndTime", auctionEndTime)
                    .put("amount", String.valueOf(price))
                    .put("incomeAmount", String.format("%.2f", incomeAmount))
                    .put("serviceCharge", String.format("%.2f", serviceCharge))
                    .put("auctionExpectAmount", 0.3)
                    .put("downType", 8)
                    .put("dealType", 1)
                    .put("goodsType", 1)
                    .put("imgs", new JSONArray(info.getArchiveImage()))
                    .put("isAutoExtension", true)
                    .put("isPayBondActive", true)
                    .put("collectionNumber", JSONObject.NULL)
                    .put("count", JSONObject.NULL)
                    .put("auctionTimeSetUpId", JSONObject.NULL)
                    .put("publisher", JSONObject.NULL)
                    .put("remark", JSONObject.NULL)
                    .put("technicalSupport", JSONObject.NULL)
                    .put("preferentialId", JSONObject.NULL);
        }
    }
}