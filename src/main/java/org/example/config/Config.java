package org.example.config;

import org.example.util.Logger;
import java.io.*;
import java.util.Properties;

public class Config {
    private static final Logger logger = Logger.getInstance();
    private static volatile Config instance;
    private final Properties properties;

    // 配置常量
    public static final String USER_TOKEN = "USER_TOKEN";
    public static final String USER_NICKNAME = "USER_NICKNAME";
    public static final String USER_PHONE = "USER_PHONE";
    private static final String CONFIG_FILE = "config.properties";

    private Config() {
        this.properties = new Properties();
        loadConfig();
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Updated Configuration");
            logger.log("Config", "配置文件已保存", Logger.LogLevel.INFO);
        } catch (IOException e) {
            logger.log("Config", "保存配置文件失败: " + e.getMessage(), Logger.LogLevel.ERROR);
            throw new ConfigException("保存配置文件失败", e);
        }
    }

    public boolean isUserLoggedIn() {
        String token = get(USER_TOKEN);
        return token != null && !token.isEmpty();
    }

    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            logger.log("Config", "配置文件加载成功", Logger.LogLevel.INFO);
        } catch (IOException e) {
            logger.log("Config", "配置文件不存在，创建默认配置", Logger.LogLevel.WARN);
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
        // API配置
        properties.setProperty("TIME_MILLISECONDS", "2000");
        properties.setProperty("SHORT_NAME", "YE");
        properties.setProperty("DEV_TYPE", "2");
        properties.setProperty("DELAY_HOURS", "5");
        properties.setProperty("PLATFORM_ID", "741");
        properties.setProperty("CLIENT_TYPE", "ios");

        // API URLs
        properties.setProperty("SEARCH_URL", "https://api.x-metash.cn/h5/home/searchApp");
        properties.setProperty("ORDER_URL", "https://api.x-metash.cn/h5/order/unifiedPay");
        properties.setProperty("ARCHIVE_URL", "https://api.x-metash.cn/h5/goods/archive");
        properties.setProperty("PRE_CREATE_URL", "https://api.x-metash.cn/h5/goods/preCreate");
        properties.setProperty("CREATE_URL", "https://api.x-metash.cn/h5/goods/create/v2");
        properties.setProperty("UNIFIED_PAY_URL", "https://api.x-metash.cn/h5/goods/create/unifiedPay");
        properties.setProperty("LOGIN_URL", "https://api.x-metash.cn/h5/login");
        properties.setProperty("ADDRESS_URL", "https://api.x-metash.cn/h5/address/list");
        properties.setProperty("GOODS_DETAILS", "https://api.x-metash.cn/h5/goods/details");
        properties.setProperty("XM_URL", "https://xmeta.x-metash.cn/prod/xmeta_mall/index.html");

        // 重试配置
        properties.setProperty("MAX_RETRIES", "3");
        properties.setProperty("RETRY_DELAY_MS", "3000");

        saveConfig();
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }
}
