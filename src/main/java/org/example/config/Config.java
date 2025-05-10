package org.example.config;

import java.io.*;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        properties.setProperty("TOKEN", ""); // your_token_here
        properties.setProperty("TIME_MILLISECONDS", "2000");
        properties.setProperty("ADDRESS_ID", ""); // your_address_id
        properties.setProperty("SHORT_NAME", "YE");
        properties.setProperty("DEV_TYPE", "2");
        properties.setProperty("SEARCH_URL", "https://api.x-metash.cn/h5/home/searchApp");
        properties.setProperty("ORDER_URL", "https://api.x-metash.cn/h5/order/unifiedPay");
        properties.setProperty("MAX_RETRIES", "3");
        properties.setProperty("RETRY_DELAY", "1000");

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Default Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }
}