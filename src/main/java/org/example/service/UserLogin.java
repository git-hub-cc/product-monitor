package org.example.service;

import org.example.config.Config;
import org.example.model.AddressItem;
import org.example.util.HttpUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户登录服务类
 * 负责处理用户登录认证相关功能
 */
public class UserLogin {
    // 定义常量
    private static final int SUCCESS_CODE = 200;

    private String token;
    private String nickName;
    private String phone;

    /**
     * 执行登录操作
     * @param phone 手机号
     * @param code 验证码
     * @return 是否登录成功
     * @throws Exception 当登录过程中出现错误时抛出异常
     */
    public boolean login(String phone, String code) throws Exception {
        // 输入验证
        if (phone == null || phone.isEmpty() || code == null || code.isEmpty()) {
            throw new IllegalArgumentException("手机号和验证码不能为空");
        }

        // 构建登录请求体
        JSONObject requestBody = new JSONObject()
                .put("phone", phone)
                .put("code", code)
                .put("client", Config.get("CLIENT_TYPE"));

        try {
            // 使用HttpUtil发送请求
            JSONObject response = HttpUtil.post(
                    Config.get("LOGIN_URL"),
                    requestBody,
                    "" // 登录不需要token
            );

            if (response.getInt("code") == SUCCESS_CODE) {
                JSONObject data = response.getJSONObject("data");
                // 验证必需字段
                if (!data.has("token") || !data.has("nickName") || !data.has("phone")) {
                    throw new Exception("登录响应数据不完整");
                }

                // 保存登录信息
                this.token = data.getString("token");
                this.nickName = data.getString("nickName");
                this.phone = data.getString("phone");

                Config.setKey("TOKEN", "Bearer"+this.token);
                return true;
            } else {
                throw new Exception("登录失败: " + response.optString("msg", "未知错误"));
            }
        } catch (Exception e) {
            logout(); // 清除任何部分登录状态
            throw new Exception("登录过程出错: " + e.getMessage());
        }
    }

    /**
     * 获取用户地址列表
     * @return 地址列表
     * @throws Exception 如果获取地址失败
     */
    public List<AddressItem> getAddresses() throws Exception {
        // 验证登录状态
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("用户未登录");
        }
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject()
                    .put("id", "")
                    .put("platformName", "鲸探")
                    .put("remark", "")
                    .put("address", this.phone)
                    .put("platformId", Config.get("PLATFORM_ID"))
                    .put("isDefault", 0);


            // 发送请求获取地址列表
            JSONObject response = HttpUtil.post(
                    Config.get("ADDRESS_URL"),
                    requestBody,
                    token
            );
            if (response.getInt("code") == SUCCESS_CODE) {
                JSONArray data = response.getJSONArray("data");
                List<AddressItem> addresses = new ArrayList<>();
                // 解析地址数据
                for (int i = 0; i < data.length(); i++) {
                    JSONObject addressObj = data.getJSONObject(i);

                    // 验证必需字段
                    if (!addressObj.has("id") || !addressObj.has("address") ||
                            !addressObj.has("platformName") || !addressObj.has("isEnablePlatform")) {
                        continue;  // 跳过不完整的地址数据
                    }
                    // 只添加已启用的平台地址
                    if (addressObj.getBoolean("isEnablePlatform")) {
                        addresses.add(new AddressItem(
                                addressObj.getLong("id"),
                                addressObj.getString("address")
                        ));
                    }
                }
                if (addresses.isEmpty()) {
                    throw new Exception("未找到可用的地址");
                }
                return addresses;
            } else {
                throw new Exception("获取地址失败: " + response.optString("msg", "未知错误"));
            }
        } catch (Exception e) {
            throw new Exception("获取地址过程出错: " + e.getMessage());
        }
    }

    /**
     * 获取登录token
     * @return 登录token
     */
    public String getToken() {
        return token;
    }

    /**
     * 获取用户昵称
     * @return 用户昵称
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * 获取用户手机号
     * @return 用户手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 退出登录
     */
    public void logout() {
        this.token = null;
        this.nickName = null;
        this.phone = null;
    }
}