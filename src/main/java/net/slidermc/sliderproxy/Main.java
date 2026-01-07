package net.slidermc.sliderproxy;

import net.slidermc.sliderproxy.api.config.YamlConfiguration;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.plugin.PluginManager;
import net.slidermc.sliderproxy.api.plugin.PluginManagerHolder;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.api.server.ServerManager;
import net.slidermc.sliderproxy.listener.ReceivePluginMessageEventHandler;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.encryption.ServerEncryptionManager;
import net.slidermc.sliderproxy.network.packet.NetworkPacketRegistry;
import net.slidermc.sliderproxy.network.packet.PacketDirection;
import net.slidermc.sliderproxy.network.packet.clientbound.ClientboundPluginMessagePacket;
import net.slidermc.sliderproxy.network.packet.clientbound.configuration.ClientboundDisconnectConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.configuration.ClientboundFinishConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.configuration.ClientboundKeepAliveConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundDisconnectLoginPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundEncryptionRequestPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundSetCompressionPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.*;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundPongResponsePacket;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundStatusResponsePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.ServerboundPluginMessagePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.configuration.ServerboundClientInformationConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.configuration.ServerboundFinishConfigurationAckPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.configuration.ServerboundKeepAliveConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.handshake.ServerboundHandshakePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundEncryptionResponsePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundHelloPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundLoginAcknowledgePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.play.ServerboundChatCommandPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.play.ServerboundClientInformationPlayPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.play.ServerboundConfigurationAckPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.play.ServerboundKeepAlivePlayPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.status.ServerboundPingRequestPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.status.ServerboundStatusRequestPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Map;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws FileNotFoundException {
        // 初始化语言系统
        initLanguages();

        registerClientboundPackets();
        registerServerboundPackets();

        File configFile = new File("./config.yml");
        if (!configFile.exists()) {
            log.info(TranslateManager.translate("sliderproxy.config.creating"));
            try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("config.yml")) {
                if (inputStream == null) {
                    log.error(TranslateManager.translate("sliderproxy.config.create.notfound"));
                    return;
                }
                Files.copy(inputStream, configFile.toPath());
                log.info(TranslateManager.translate("sliderproxy.config.create.success"));
            } catch (Exception e) {
                log.error(TranslateManager.translate("sliderproxy.config.create.error", e));
                return;
            }
        }

        YamlConfiguration yamlConfiguration = new YamlConfiguration(configFile);
        RunningData.configuration = yamlConfiguration;
        ServerManager serverManager = ServerManager.getInstance();

        // 读取并设置语言配置
        String language = yamlConfiguration.getString("proxy.language", "zh_cn");
        if (TranslateManager.isLanguageRegistered(language)) {
            TranslateManager.setCurrentLanguage(language);
            log.info(TranslateManager.translate("sliderproxy.config.language.set", language));
        }

        // 注册监听器
        registerListeners();

        // 初始化插件系统
        initPlugins();

        // 读取代理配置
        String host = yamlConfiguration.getString("proxy.host", "0.0.0.0");
        int port = yamlConfiguration.getInt("proxy.port", 25565);

        // 读取并创建服务器
        loadServersFromConfig(yamlConfiguration, serverManager);

        // 如果启用 online-mode，初始化加密管理器
        boolean onlineMode = yamlConfiguration.getBoolean("proxy.online-mode", true);
        if (onlineMode) {
            ServerEncryptionManager.getInstance();
            log.info(TranslateManager.translate("sliderproxy.encryption.initialized"));
        }

        SliderProxyServer server = new SliderProxyServer(new InetSocketAddress(host, port));
        server.run();
    }

    private static void registerListeners() {
        EventRegistry.registerListener(new ReceivePluginMessageEventHandler());
    }

    /**
     * 初始化插件系统
     */
    private static void initPlugins() {
        File pluginsFolder = new File("./plugins");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }

        // 初始化插件管理器
        PluginManagerHolder.initialize(pluginsFolder);

        // 加载所有插件
        PluginManager pluginManager = PluginManagerHolder.getInstance();
        pluginManager.loadPlugins();

        // 启用所有插件
        pluginManager.enablePlugins();

        log.info(TranslateManager.translate("sliderproxy.plugins.initialized", pluginManager.getLoadedPlugins().size()));
    }

    /**
     * 从配置文件加载服务器配置并创建ProxiedServer实例
     */
    private static void loadServersFromConfig(YamlConfiguration config, ServerManager serverManager) {
        // 获取servers配置节
        Object serversObj = config.get("servers");
        if (!(serversObj instanceof Map)) {
            log.warn(TranslateManager.translate("sliderproxy.config.servers.notfound"));
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> serversMap = (Map<String, Object>) serversObj;
        int serverCount = 0;

        for (Map.Entry<String, Object> entry : serversMap.entrySet()) {
            String serverName = entry.getKey();
            Object serverConfigObj = entry.getValue();

            if (!(serverConfigObj instanceof Map)) {
                log.warn(TranslateManager.translate("sliderproxy.config.server.invalid", serverName));
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> serverConfig = (Map<String, Object>) serverConfigObj;

            // 读取服务器地址和端口
            String address = getStringFromMap(serverConfig, "address", "127.0.0.1");
            int port = getIntFromMap(serverConfig, "port", 25565);

            // 创建ProxiedServer实例
            InetSocketAddress serverAddress = new InetSocketAddress(address, port);
            ProxiedServer proxiedServer = new ProxiedServer(serverAddress, serverName);

            // 添加到服务器管理器
            serverManager.addServer(proxiedServer);
            serverCount++;

            log.info(TranslateManager.translate("sliderproxy.config.server.loaded",
                    serverName, address, port));
        }

        log.info(TranslateManager.translate("sliderproxy.config.servers.loaded", serverCount));

        // 检查默认服务器是否存在
        String defaultServerName = config.getString("proxy.default-server", "lobby");
        if (serverManager.getServer(defaultServerName) == null) {
            log.warn(TranslateManager.translate("sliderproxy.config.defaultserver.notfound", defaultServerName));
        }
    }

    /**
     * 从Map中安全获取字符串值
     */
    private static String getStringFromMap(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        } else if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    /**
     * 从Map中安全获取整数值
     */
    private static int getIntFromMap(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static void initLanguages() {
        TranslateManager.loadFromResource("zh_cn", "lang/zh_cn.json");
        TranslateManager.loadFromResource("en_us", "lang/en_us.json");
    }

    private static void registerClientboundPackets() {
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.STATUS, 0x00, ClientboundStatusResponsePacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.STATUS, 0x01, ClientboundPongResponsePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x02, ClientboundLoginSuccessPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x00, ClientboundDisconnectLoginPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x01, ClientboundEncryptionRequestPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x03, ClientboundSetCompressionPacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x04, ClientboundKeepAliveConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x03, ClientboundFinishConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x02, ClientboundDisconnectConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x01, ClientboundPluginMessagePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x6F, ClientboundStartConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x26, ClientboundKeepAlivePlayPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x1C, ClientboundDisconnectPlayPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x18, ClientboundPluginMessagePacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x72, ClientboundSystemChatPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x2B, ClientboundLoginPlayPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x73, ClientboundSoundEffectPacket.class);
    }

    private static void registerServerboundPackets() {
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.HANDSHAKE, 0x00, ServerboundHandshakePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.STATUS, 0x00, ServerboundStatusRequestPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.STATUS, 0x01, ServerboundPingRequestPacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.LOGIN, 0x00, ServerboundHelloPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.LOGIN, 0x01, ServerboundEncryptionResponsePacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.LOGIN, 0x03, ServerboundLoginAcknowledgePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x04, ServerboundKeepAliveConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x03, ServerboundFinishConfigurationAckPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x02, ServerboundPluginMessagePacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x00, ServerboundClientInformationConfigurationPacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x1B, ServerboundKeepAlivePlayPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x15, ServerboundPluginMessagePacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x06, ServerboundChatCommandPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x0F, ServerboundConfigurationAckPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x0D, ServerboundClientInformationPlayPacket.class);
    }
}
