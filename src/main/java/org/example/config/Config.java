package org.example.config;

import java.io.*;
import java.util.Properties;

/**
 * 配置管理类
 * 用于加载和管理应用程序的配置信息
 */
public class Config {
    // 存储配置信息的Properties对象
    private static final Properties properties = new Properties();
    // 配置文件路径
    private static final String CONFIG_FILE = "config.properties";

    // 静态代码块，在类加载时执行配置文件加载
    static {
        loadConfig();
    }

    /**
     * 加载配置文件
     * 如果配置文件不存在，则创建默认配置
     */
    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("配置文件不存在，创建默认配置文件");
            createDefaultConfig();
        }
    }

    /**
     * 创建默认配置文件
     * 设置默认的配置项并保存到文件中
     */
    private static void createDefaultConfig() {
        // API认证相关配置
        properties.setProperty("TOKEN", "");           // API访问令牌
        properties.setProperty("ADDRESS_ID", "");      // 地址ID
        properties.setProperty("TIME_MILLISECONDS", "2000"); // 超时时间（毫秒）
        properties.setProperty("SHORT_NAME", "YE");    // 是否同意协议
        properties.setProperty("DEV_TYPE", "2");       // 支付方式
        properties.setProperty("DELAY_HOURS ", "5");       // 预售时间比发行延后数小时
        properties.setProperty("PLATFORM_ID", "741");       // 平台id

        // API端点URL配置
        properties.setProperty("SEARCH_URL", "https://api.x-metash.cn/h5/home/searchApp");
        properties.setProperty("ORDER_URL", "https://api.x-metash.cn/h5/order/unifiedPay");
        properties.setProperty("ARCHIVE_URL", "https://api.x-metash.cn/h5/goods/archive");
        properties.setProperty("PRE_CREATE_URL", "https://api.x-metash.cn/h5/goods/preCreate");
        properties.setProperty("CREATE_URL", "https://api.x-metash.cn/h5/goods/create/v2");
        properties.setProperty("UNIFIED_PAY_URL", "https://api.x-metash.cn/h5/goods/create/unifiedPay");

        // 重试策略配置
        properties.setProperty("MAX_RETRIES", "3");    // 最大重试次数
        properties.setProperty("RETRY_DELAY", "3000"); // 重试延迟（毫秒）

        // 将默认配置保存到文件
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Default Configuration");
        } catch (IOException e) {
            System.err.println("保存默认配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取字符串类型的配置值
     * @param key 配置键
     * @return 配置值
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * 获取整数类型的配置值
     * @param key 配置键
     * @return 配置值转换后的整数
     * @throws NumberFormatException 如果配置值不能转换为整数
     */
    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    /**
     * 获取长整型的配置值
     * @param key 配置键
     * @return 配置值转换后的长整型数
     * @throws NumberFormatException 如果配置值不能转换为长整型
     */
    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }
}