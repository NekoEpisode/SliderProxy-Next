package net.slidermc.sliderproxy.network.encryption;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.slidermc.sliderproxy.api.player.data.GameProfile;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Mojang 会话服务器验证
 */
public class MojangSessionService {
    private static final Logger log = LoggerFactory.getLogger(MojangSessionService.class);
    private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final Gson GSON = new Gson();

    /**
     * 验证玩家会话
     * @param username 玩家用户名
     * @param serverIdHash 服务器ID哈希
     * @param playerIp 玩家IP地址（可选，用于 prevent-proxy-connections）
     * @return 验证结果，包含玩家的 GameProfile 和皮肤属性
     */
    public static CompletableFuture<AuthenticationResult> hasJoined(String username, String serverIdHash, String playerIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(HAS_JOINED_URL);
                urlBuilder.append("?username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
                urlBuilder.append("&serverId=").append(URLEncoder.encode(serverIdHash, StandardCharsets.UTF_8));
                
                // 注意：不传递 IP 参数，因为这可能导致代理环境下验证失败
                // 如果需要 prevent-proxy-connections 功能，可以取消下面的注释
                // if (playerIp != null && !playerIp.isEmpty()) {
                //     urlBuilder.append("&ip=").append(URLEncoder.encode(playerIp, StandardCharsets.UTF_8));
                // }

                String requestUrl = urlBuilder.toString();
                log.debug("Authenticating user {} with URL: {}", username, requestUrl);
                
                URL url = new URL(requestUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Content-Type", "application/json");

                int responseCode = connection.getResponseCode();
                log.debug("Authentication response code for {}: {}", username, responseCode);
                
                if (responseCode == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        log.debug("Authentication response for {}: {}", username, response);
                        return parseResponse(response.toString());
                    }
                } else if (responseCode == 204) {
                    // 204 No Content - 验证失败
                    log.warn("Authentication failed for user {}: No content returned (serverIdHash={})", username, serverIdHash);
                    return null;
                } else {
                    log.error("Authentication request failed with status code: {}", responseCode);
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed to authenticate user {}", username, e);
                return null;
            }
        });
    }

    private static AuthenticationResult parseResponse(String json) {
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            
            String id = obj.get("id").getAsString();
            String name = obj.get("name").getAsString();
            
            // 将无连字符的 UUID 转换为标准格式
            UUID uuid = parseUuid(id);
            
            // 解析属性（皮肤等）
            List<ClientboundLoginSuccessPacket.Property> properties = new ArrayList<>();
            if (obj.has("properties")) {
                JsonArray propsArray = obj.getAsJsonArray("properties");
                for (int i = 0; i < propsArray.size(); i++) {
                    JsonObject prop = propsArray.get(i).getAsJsonObject();
                    String propName = prop.get("name").getAsString();
                    String propValue = prop.get("value").getAsString();
                    String signature = prop.has("signature") ? prop.get("signature").getAsString() : null;
                    properties.add(new ClientboundLoginSuccessPacket.Property(propName, propValue, signature));
                }
            }
            
            return new AuthenticationResult(new GameProfile(name, uuid), properties);
        } catch (Exception e) {
            log.error("Failed to parse authentication response: {}", json, e);
            return null;
        }
    }

    /**
     * 将无连字符的 UUID 字符串转换为 UUID 对象
     */
    private static UUID parseUuid(String id) {
        if (id.length() == 32) {
            // 格式: 11111111222233334444555555555555 -> 11111111-2222-3333-4444-555555555555
            String formatted = id.substring(0, 8) + "-" +
                    id.substring(8, 12) + "-" +
                    id.substring(12, 16) + "-" +
                    id.substring(16, 20) + "-" +
                    id.substring(20);
            return UUID.fromString(formatted);
        }
        return UUID.fromString(id);
    }

    /**
     * 验证结果
     */
    public record AuthenticationResult(
            GameProfile gameProfile,
            List<ClientboundLoginSuccessPacket.Property> properties
    ) {}
}
