package org.example.service;

import org.example.config.Config;
import org.example.model.ProductInfo;
import org.example.util.HttpUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * 商品发布服务类
 * 负责处理商品的预购和发布相关功能，包括搜索商品、获取详情、创建预购订单等操作
 */
public class ProductRelease {
    // 定义常量，提高代码可维护性
    private static final int SUCCESS_CODE = 200;
    private static final double SERVICE_CHARGE_RATE = 0.04;  // 4%服务费率

    /**
     * 发布预购商品的主要流程
     * @param name 商品名称
     * @param price 商品价格
     * @return 是否发布成功
     * @throws Exception 当发布过程中出现错误时抛出异常
     */
    public boolean publishPreOrder(String name, double price) throws Exception {
        // 1. 搜索藏品获取 archiveId
        String archiveId = searchArchive(name);
        if (archiveId == null) {
            throw new Exception("未找到相关藏品");
        }

        // 2. 获取商品详细数据
        ProductInfo productInfo = getProductInfo(archiveId);

        // 3. 发起预购请求
        String preOrderKey = createPreOrder(productInfo, price);
        if (preOrderKey == null) {
            throw new Exception("创建预购失败");
        }

        // 4. 确认预购
        return confirmPreOrder(preOrderKey, productInfo, price);
    }

    /**
     * 搜索藏品获取archiveId
     * @param name 藏品名称
     * @return 藏品的archiveId，如果未找到返回null
     * @throws Exception 当搜索请求失败时抛出异常
     */
    private String searchArchive(String name) throws Exception {
        // 构建搜索请求体
        JSONObject searchBody = new JSONObject()
                .put("platformIds", new JSONArray())
                .put("pageNum", 1)
                .put("type", "")
                .put("search", name)
                .put("isTransfer", "")
                .put("goodsTypeList", new JSONArray().put(2).put(3));  // 支持的商品类型

        // 执行搜索请求
        JSONObject searchResult = HttpUtil.post(
                Config.get("SEARCH_URL"),
                searchBody,
                Config.get("TOKEN")
        );

        // 验证响应状态
        if (searchResult.getInt("code") != SUCCESS_CODE) {
            throw new Exception("搜索失败: " + searchResult.getString("msg"));
        }

        // 从第一个结果中提取archiveId
        JSONArray items = searchResult.getJSONArray("data");
        return items.isEmpty() ? null : String.valueOf(items.getJSONObject(0).getLong("archiveId"));
    }

    /**
     * 获取商品详细信息
     * @param archiveId 藏品ID
     * @return 商品详细信息对象
     * @throws Exception 当获取信息失败时抛出异常
     */
    private ProductInfo getProductInfo(String archiveId) throws Exception {
        // 构建请求体
        JSONObject requestBody = new JSONObject()
                .put("archiveId", archiveId)
                .put("platformId", Config.get("PLATFORM_ID"));

        // 执行API请求
        JSONObject response = HttpUtil.post(
                Config.get("ARCHIVE_URL"),
                requestBody,
                Config.get("TOKEN")
        );

        // 验证响应状态
        if (response.getInt("code") != SUCCESS_CODE) {
            throw new Exception("获取商品信息失败: " + response.getString("msg"));
        }

        // 解析响应数据并填充ProductInfo对象
        JSONObject data = response.getJSONObject("data");
        ProductInfo info = new ProductInfo();

        // 设置基本信息
        info.setArchiveId(data.getLong("archiveId"));
        info.setTotalGoodsCount(data.getInt("totalGoodsCount"));
        info.setPlatformId(data.getLong("platformId"));
        info.setPlatformName(data.getString("platformName"));
        info.setPlatformIcon(data.getString("platformIcon"));
        info.setArchiveName(data.getString("archiveName"));
        info.setIssueTime(data.getString("issueTime"));
        info.setCoolingTime(data.getInt("coolingTime"));
        info.setGoodsMinPrice(data.getDouble("goodsMinPrice"));

        // 设置商品状态相关信息
        info.setOpenEarnest(data.getBoolean("isOpenEarnest"));
        info.setArchiveSalesType(data.getInt("archiveSalesType"));
        info.setSellingCount(data.getInt("sellingCount"));
        info.setOpenAuction(data.getBoolean("isOpenAuction"));
        info.setOpenWantBuy(data.getBoolean("isOpenWantBuy"));

        // 解析并设置商品图片
        JSONArray images = data.getJSONArray("archiveImage");
        ArrayList<String> archiveImages = new ArrayList<>();
        for (int i = 0; i < images.length(); i++) {
            archiveImages.add(images.getString(i));
        }
        info.setArchiveImage(archiveImages);

        return info;
    }

    /**
     * 创建预购订单
     * @param info 商品信息
     * @param price 商品价格
     * @return 预购订单key
     * @throws Exception 当创建预购订单失败时抛出异常
     */
    private String createPreOrder(ProductInfo info, double price) throws Exception {
        // 计算服务费和实际收入金额
        double serviceCharge = Math.round(price * SERVICE_CHARGE_RATE * 100.0) / 100.0;
        double incomeAmount = Math.round((price - serviceCharge) * 100.0) / 100.0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(info.getIssueTime(), formatter);
        // 延后5小时
        LocalDateTime delayedTime = dateTime.plusHours(Integer.parseInt(Config.get("DELAY_HOURS")));
        // 转换为时间戳（毫秒）
        long timestamp = delayedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 如果需要秒级时间戳，可以除以1000
        long auctionEndTime = timestamp / 1000;

        // 构建预购请求体
        JSONObject requestBody = new JSONObject()
                .put("downType", 8)
                .put("isAutoExtension", true)
                .put("amount", String.valueOf(price))
                .put("auctionEndTime", auctionEndTime)
                .put("auctionExpectAmount", 0.3)
                .put("dealType", 1)
                .put("goodsType", 1)
                .put("imgs", new JSONArray(info.getArchiveImage()))
                .put("introduce", info.getArchiveDescription())
                .put("issueDate", info.getIssueTime())
                .put("name", info.getArchiveName())
                .put("platformId", String.valueOf(info.getPlatformId()))
                .put("presellDate", info.getIssueTime())
                .put("incomeAmount", String.format("%.2f", incomeAmount))
                .put("serviceCharge", String.format("%.2f", serviceCharge))
                .put("isPayBondActive", true)
                .put("platformName", info.getPlatformName())
                .put("archiveId", String.valueOf(info.getArchiveId()))
                .put("introduce", "请自行描述")
                .put("collectionNumber", JSONObject.NULL)
                .put("count", JSONObject.NULL)
                .put("auctionTimeSetUpId", JSONObject.NULL)
                .put("publisher", JSONObject.NULL)
                .put("remark", JSONObject.NULL)
                .put("technicalSupport", JSONObject.NULL)
                .put("preferentialId", JSONObject.NULL);

        // 执行API请求
        JSONObject response = HttpUtil.post(
                Config.get("PRE_CREATE_URL"),
                requestBody,
                Config.get("TOKEN")
        );

        // 验证响应状态
        if (response.getInt("code") != SUCCESS_CODE) {
            throw new Exception("创建预购订单失败: " + response.getString("msg"));
        }

        // 验证并返回预购key
        JSONObject data = response.getJSONObject("data");
        if (!data.getBoolean("flag")) {
            throw new Exception("创建预购订单失败: 服务器拒绝请求");
        }

        return data.getString("key");
    }

    /**
     * 确认预购订单
     * @param preOrderKey 预购订单key
     * @param info 商品信息
     * @return 确认是否成功
     * @throws Exception 当确认过程中出现错误时抛出异常
     */
    private boolean confirmPreOrder(String preOrderKey, ProductInfo info, double price) throws Exception {
        // 计算服务费和实际收入金额
        double serviceCharge = Math.round(price * SERVICE_CHARGE_RATE * 100.0) / 100.0;
        double incomeAmount = Math.round((price - serviceCharge) * 100.0) / 100.0;


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String baseTime = info.getIssueTime();
        Date date = sdf.parse(baseTime);
        // 预售比发行延后五小时
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(Config.get("DELAY_HOURS")));
        String auctionEndTime = sdf.format(calendar.getTime());

        // 第一步：创建商品
        JSONObject createBody = new JSONObject()
                .put("downType", 8)
                .put("isAutoExtension", true)
                .put("amount", String.valueOf(price))
                .put("auctionEndTime", auctionEndTime)
                .put("auctionExpectAmount", 0.3)
                .put("dealType", 1)
                .put("goodsType", 1)
                .put("imgs", new JSONArray(info.getArchiveImage()))
                .put("introduce", info.getArchiveDescription())
                .put("issueDate", info.getIssueTime())
                .put("name", info.getArchiveName())
                .put("platformId", String.valueOf(info.getPlatformId()))
                .put("presellDate", auctionEndTime)
                .put("incomeAmount", String.format("%.2f", incomeAmount))
                .put("serviceCharge", String.format("%.2f", serviceCharge))
                .put("isPayBondActive", true)
                .put("platformName", info.getPlatformName())
                .put("archiveId", String.valueOf(info.getArchiveId()))
                .put("introduce", "请自行描述")
                .put("collectionNumber", JSONObject.NULL)
                .put("count", JSONObject.NULL)
                .put("auctionTimeSetUpId", JSONObject.NULL)
                .put("publisher", JSONObject.NULL)
                .put("remark", JSONObject.NULL)
                .put("technicalSupport", JSONObject.NULL)
                .put("preferentialId", JSONObject.NULL)
                .put("key", preOrderKey);

        JSONObject createResponse = HttpUtil.post(
                Config.get("CREATE_URL"),
                createBody,
                Config.get("TOKEN")
        );

        // 验证创建响应
        if (createResponse.getInt("code") != SUCCESS_CODE) {
            throw new Exception("创建商品失败: " + createResponse.getString("msg"));
        }

        // 获取商品ID
        JSONObject createData = createResponse.getJSONObject("data");
        long goodsId = createData.getLong("goodsId");

        // 第二步：统一支付确认
        JSONObject payBody = new JSONObject()
                .put("goodsId", goodsId)
                .put("shortName", Config.get("SHORT_NAME"))
                .put("devType", Config.get("DEV_TYPE"));

        JSONObject payResponse = HttpUtil.post(
                Config.get("UNIFIED_PAY_URL"),
                payBody,
                Config.get("TOKEN")
        );

        // 验证支付确认响应
        if (payResponse.getInt("code") != SUCCESS_CODE) {
            throw new Exception("支付确认失败: " + payResponse.getString("msg"));
        }

        return true;
    }
}