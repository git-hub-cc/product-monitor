package org.example.util;

import org.example.config.Config;
import org.json.JSONObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

public class HttpUtil {
    private static final Logger logger = Logger.getInstance();
    private static final Config config = Config.getInstance();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int MAX_RETRIES = config.getInt("MAX_RETRIES");
    private static final long RETRY_DELAY_MS = config.getInt("RETRY_DELAY_MS");

    public static JSONObject post(String url, JSONObject body, String token)
            throws Exception {
        int retries = 0;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            try {
                return executePost(url, body, token);
            } catch (Exception e) {
                lastException = e;
                retries++;
                if (retries < MAX_RETRIES) {
                    logger.log("HTTP", String.format("请求失败，第%d次重试", retries),
                            Logger.LogLevel.WARN);
                    Thread.sleep(RETRY_DELAY_MS * retries);
                }
            }
        }

        String errorMsg = "请求失败，已重试" + MAX_RETRIES + "次";
        logger.log("HTTP", errorMsg, Logger.LogLevel.ERROR);
        throw new Exception(errorMsg, lastException);
    }

    private static JSONObject executePost(String url, JSONObject body,
                                          String token) throws Exception {
        HttpRequest request = createRequest(url, body, token);

        // 打印详细的请求信息
        logger.log("HTTP", String.format("""
            发送请求:
            URL: %s
            Headers: %s
            Body: %s""",
                        url,
                        request.headers().map().toString(),
                        body.toString(2)),
                Logger.LogLevel.DEBUG);

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        // 打印详细的响应信息
        logger.log("HTTP", String.format("""
            收到响应:
            Status: %d
            Headers: %s
            Body: %s""",
                        response.statusCode(),
                        response.headers().map().toString(),
                        new JSONObject(response.body()).toString(2)),
                Logger.LogLevel.DEBUG);

        if (response.statusCode() >= 500) {
            String errorMsg = "服务器错误: " + response.statusCode();
            logger.log("HTTP", errorMsg, Logger.LogLevel.ERROR);
            throw new Exception(errorMsg);
        }

        return new JSONObject(response.body());
    }

    private static HttpRequest createRequest(String url, JSONObject body,
                                             String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", token);
        }

        return builder.POST(HttpRequest.BodyPublishers.ofString(
                body.toString())).build();
    }
}