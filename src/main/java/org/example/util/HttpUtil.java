package org.example.util;

import org.json.JSONObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class HttpUtil {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static JSONObject post(String url, JSONObject body, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }
}