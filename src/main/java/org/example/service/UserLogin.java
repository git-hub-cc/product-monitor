package org.example.service;

import org.example.config.Config;
import org.example.model.AddressItem;
import org.example.util.HttpUtil;
import org.example.util.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserLogin {
    private static final Logger logger = Logger.getInstance();
    private static volatile UserLogin instance;
    private String token;
    private String nickName;
    private String phone;

    private UserLogin() {}

    public static UserLogin getInstance() {
        if (instance == null) {
            synchronized (UserLogin.class) {
                if (instance == null) {
                    instance = new UserLogin();
                }
            }
        }
        return instance;
    }

    public boolean login(String phone, String code) throws Exception {
        validateLoginInput(phone, code);

        JSONObject requestBody = new JSONObject()
                .put("phone", phone)
                .put("code", code)
                .put("client", Config.getInstance().get("CLIENT_TYPE"));

        try {
            JSONObject response = HttpUtil.post(
                    Config.getInstance().get("LOGIN_URL"),
                    requestBody,
                    ""
            );

            if (response.getInt("code") == 200) {
                handleSuccessfulLogin(response.getJSONObject("data"));
                Config.getInstance().set("LOGIN_INFO", String.valueOf(response.getJSONObject("data")));
                logger.log("Login", "登录成功: " + nickName, Logger.LogLevel.INFO);
                return true;
            } else {
                throw new Exception("登录失败: " + response.getString("msg"));
            }
        } catch (Exception e) {
            logout();
            logger.log("Login", "登录失败: " + e.getMessage(), Logger.LogLevel.ERROR);
            throw e;
        }
    }

    private void validateLoginInput(String phone, String code) {
        if (phone == null || phone.isEmpty() || code == null || code.isEmpty()) {
            throw new IllegalArgumentException("手机号和验证码不能为空");
        }
    }

    private void handleSuccessfulLogin(JSONObject data) {
        this.token = data.getString("token");
        this.nickName = data.getString("nickName");
        this.phone = data.getString("phone");

        Config config = Config.getInstance();
        config.set("TOKEN", "Bearer " + this.token);
        config.set("USER_NICKNAME", this.nickName);
        config.set("USER_PHONE", this.phone);
        config.saveConfig();
    }

    public List<AddressItem> getAddresses() throws Exception {
        validateLoginStatus();

        JSONObject requestBody = createAddressRequestBody();
        JSONObject response = HttpUtil.post(
                Config.getInstance().get("ADDRESS_URL"),
                requestBody,
                token
        );

        return parseAddressResponse(response);
    }

    private void validateLoginStatus() {
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("用户未登录");
        }
    }

    private JSONObject createAddressRequestBody() {
        return new JSONObject()
                .put("id", "")
                .put("platformName", "鲸探")
                .put("remark", "")
                .put("address", this.phone)
                .put("platformId", Config.getInstance().get("PLATFORM_ID"))
                .put("isDefault", 0);
    }

    private List<AddressItem> parseAddressResponse(JSONObject response) throws Exception {
        if (response.getInt("code") != 200) {
            throw new Exception("获取地址失败: " + response.getString("msg"));
        }

        List<AddressItem> addresses = new ArrayList<>();
        JSONArray data = response.getJSONArray("data");

        for (int i = 0; i < data.length(); i++) {
            JSONObject addressObj = data.getJSONObject(i);
            if (isValidAddress(addressObj)) {
                addresses.add(createAddressItem(addressObj));
            }
        }

        if (addresses.isEmpty()) {
            throw new Exception("未找到可用的地址");
        }

        return addresses;
    }

    private boolean isValidAddress(JSONObject addressObj) {
        return addressObj.has("id") &&
                addressObj.has("address") &&
                addressObj.has("platformName") &&
                addressObj.has("isEnablePlatform") &&
                addressObj.getBoolean("isEnablePlatform");
    }

    private AddressItem createAddressItem(JSONObject addressObj) {
        return new AddressItem(
                addressObj.getLong("id"),
                addressObj.getString("address")
        );
    }

    public void logout() {
        this.token = null;
        this.nickName = null;
        this.phone = null;
        logger.log("Login", "用户已登出", Logger.LogLevel.INFO);
    }

    public void setLoginInfo(String token, String nickName, String phone) {
        this.token = token;
        this.nickName = nickName;
        this.phone = phone;
    }

    // Getters
    public String getToken() { return token; }
    public String getNickName() { return nickName; }
    public String getPhone() { return phone; }
}