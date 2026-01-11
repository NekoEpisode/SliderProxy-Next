package net.slidermc.sliderproxy.network.packet.clientbound.status;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.slidermc.sliderproxy.ProxyStaticValues;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

import java.util.List;
import java.util.UUID;

public class ClientboundStatusResponsePacket implements IMinecraftPacket {
    private int max;
    private int online;
    private List<PlayerInfo> sample;
    private String description;
    private boolean enforceSecureChat;
    private String version;
    private int protocolVersion;

    public ClientboundStatusResponsePacket() {
        this.version = ProxyStaticValues.MOTD_VERSION;
        this.protocolVersion = ProxyStaticValues.PROTOCOL_VERSION;
    }

    public ClientboundStatusResponsePacket(int max, int online, List<PlayerInfo> sample, String minimessageDescription, boolean enforceSecureChat, String version, int protocolVersion) {
        this.max = max;
        this.online = online;
        this.sample = sample;
        this.description = minimessageDescription;
        this.enforceSecureChat = enforceSecureChat;
        this.version = version;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void write(ByteBuf byteBuf) {
        Gson gson = new Gson();
        JsonObject root = new JsonObject();

        // version
        JsonObject versionObj = new JsonObject();
        versionObj.addProperty("name", version);
        versionObj.addProperty("protocol", protocolVersion);
        root.add("version", versionObj);

        // players
        JsonObject playersObj = new JsonObject();
        playersObj.addProperty("max", max);
        playersObj.addProperty("online", online);
        if (sample != null && !sample.isEmpty()) {
            JsonArray sampleArr = new JsonArray();
            for (PlayerInfo p : sample) {
                JsonObject playerJson = new JsonObject();
                playerJson.addProperty("name", p.name());
                playerJson.addProperty("id", p.uuid().toString());
                sampleArr.add(playerJson);
            }
            playersObj.add("sample", sampleArr);
        }
        root.add("players", playersObj);

        // description
        if (description != null) {
            Component component = MiniMessage.miniMessage().deserialize(description);

            String jsonDescription = GsonComponentSerializer.gson().serialize(component);

            JsonObject descObj = gson.fromJson(jsonDescription, JsonObject.class);
            root.add("description", descObj);

            root.add("description", descObj);
        }

        // enforceSecureChat
        root.addProperty("enforcesSecureChat", enforceSecureChat);

        String json = gson.toJson(root);
        MinecraftProtocolHelper.writeString(byteBuf, json);
    }

    @Override
    public void read(ByteBuf byteBuf) {
        String json = MinecraftProtocolHelper.readString(byteBuf);
        if (json.isEmpty()) return;

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(json, JsonObject.class);

        // version
        if (root.has("version")) {
            JsonObject ver = root.getAsJsonObject("version");
            this.version = ver.has("name") ? ver.get("name").getAsString() : "Unknown";
            this.protocolVersion = ver.has("protocol") ? ver.get("protocol").getAsInt() : 0;
        }

        // players
        if (root.has("players")) {
            JsonObject playersObj = root.getAsJsonObject("players");
            this.max = playersObj.has("max") ? playersObj.get("max").getAsInt() : 20;
            this.online = playersObj.has("online") ? playersObj.get("online").getAsInt() : 0;

            if (playersObj.has("sample")) {
                JsonArray sampleArr = playersObj.getAsJsonArray("sample");
                this.sample = new java.util.ArrayList<>();
                for (int i = 0; i < sampleArr.size(); i++) {
                    JsonObject p = sampleArr.get(i).getAsJsonObject();
                    UUID id = p.has("id") ? UUID.fromString(p.get("id").getAsString()) : UUID.randomUUID();
                    String name = p.has("name") ? p.get("name").getAsString() : "Unknown";
                    this.sample.add(new PlayerInfo(id, name));
                }
            }
        }

        // description
        if (root.has("description")) {
            JsonObject desc = root.getAsJsonObject("description");
            this.description = desc.has("text") ? desc.get("text").getAsString() : "";
        }

        // enforceSecureChat
        this.enforceSecureChat = root.has("enforcesSecureChat") && root.get("enforcesSecureChat").getAsBoolean();
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        // 将 MiniMessage 字符串转换为 Component
        net.kyori.adventure.text.Component descriptionComponent = this.description != null 
            ? net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(this.description)
            : net.kyori.adventure.text.Component.empty();
        
        // 触发 ProxyPingEvent
        net.slidermc.sliderproxy.api.event.events.ProxyPingEvent pingEvent = 
            new net.slidermc.sliderproxy.api.event.events.ProxyPingEvent(
                descriptionComponent,
                this.max, 
                this.online, 
                this.version, 
                this.protocolVersion,
                null // favicon
            );
        net.slidermc.sliderproxy.api.event.EventRegistry.callEvent(pingEvent);
        
        // 应用事件中的修改
        this.max = pingEvent.getMaxPlayers();
        this.online = pingEvent.getOnlinePlayers();
        this.version = pingEvent.getVersion();
        this.protocolVersion = pingEvent.getProtocol();
        
        // 将 Component 转换回 MiniMessage 字符串
        if (pingEvent.getDescription() != null) {
            this.description = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(pingEvent.getDescription());
        }
        
        return HandleResult.UNFORWARD;
    }

    public record PlayerInfo(UUID uuid, String name) {}

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public List<PlayerInfo> getSample() {
        return sample;
    }

    public void setSample(List<PlayerInfo> sample) {
        this.sample = sample;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnforceSecureChat() {
        return enforceSecureChat;
    }

    public void setEnforceSecureChat(boolean enforceSecureChat) {
        this.enforceSecureChat = enforceSecureChat;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
}