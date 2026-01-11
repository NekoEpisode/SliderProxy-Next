package net.slidermc.sliderproxy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.ProxyShutdownEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.plugin.PluginManager;
import net.slidermc.sliderproxy.api.plugin.PluginManagerHolder;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 关闭管理器 - 负责代理服务器的优雅关闭
 */
public class ShutdownManager {
    private static final Logger log = LoggerFactory.getLogger(ShutdownManager.class);
    private static final ShutdownManager INSTANCE = new ShutdownManager();
    
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private SliderProxyServer server;
    
    private ShutdownManager() {}
    
    public static ShutdownManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 设置服务器实例
     */
    public void setServer(SliderProxyServer server) {
        this.server = server;
    }
    
    /**
     * 注册 ShutdownHook
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (shuttingDown.compareAndSet(false, true)) {
                log.info("检测到关闭信号，正在执行优雅关闭...");
                performShutdown();
            }
        }, "Shutdown-Hook"));
        log.debug("已注册 ShutdownHook");
    }
    
    /**
     * 主动触发关闭
     */
    public void shutdown() {
        if (shuttingDown.compareAndSet(false, true)) {
            log.info(TranslateManager.translate("sliderproxy.shutdown.starting"));
            performShutdown();
            System.exit(0);
        } else {
            log.warn("服务器已经在关闭中...");
        }
    }
    
    /**
     * 检查是否正在关闭
     */
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
    
    /**
     * 执行关闭流程
     */
    private void performShutdown() {
        try {
            // 1. 踢出所有玩家
            kickAllPlayers();
            
            // 2. 禁用所有插件
            disablePlugins();
            
            // 3. 关闭网络服务
            closeServer();
            
            // 4. 清理资源
            cleanup();
            
            log.info(TranslateManager.translate("sliderproxy.shutdown.complete"));
        } catch (Exception e) {
            log.error("关闭过程中发生错误", e);
        }
    }
    
    /**
     * 踢出所有玩家
     */
    private void kickAllPlayers() {
        PlayerManager playerManager = PlayerManager.getInstance();
        int playerCount = playerManager.getPlayerCount();
        
        if (playerCount > 0) {
            log.info(TranslateManager.translate("sliderproxy.shutdown.kicking", playerCount));
            
            String kickText = TranslateManager.translate("sliderproxy.shutdown.kickmessage");
            Component kickMessage = Component.text(
                kickText != null ? kickText : "Server is shutting down",
                NamedTextColor.RED
            );
            
            for (ProxiedPlayer player : playerManager.getAllPlayers()) {
                try {
                    player.kick(kickMessage);
                } catch (Exception e) {
                    log.warn("踢出玩家 {} 时出错: {}", player.getName(), e.getMessage());
                }
            }
            
            // 清空玩家数据
            playerManager.clearAllPlayers();
        }
    }
    
    /**
     * 禁用所有插件
     */
    private void disablePlugins() {
        try {
            PluginManager pluginManager = PluginManagerHolder.getInstance();
            if (pluginManager != null) {
                int pluginCount = pluginManager.getLoadedPlugins().size();
                if (pluginCount > 0) {
                    log.info(TranslateManager.translate("sliderproxy.shutdown.disabling", pluginCount));
                    pluginManager.disablePlugins();
                }
            }
        } catch (Exception e) {
            log.error("禁用插件时出错", e);
        }
    }
    
    /**
     * 关闭网络服务
     */
    private void closeServer() {
        if (server != null) {
            log.info(TranslateManager.translate("sliderproxy.shutdown.network"));
            try {
                // 触发代理关闭事件
                EventRegistry.callEvent(new ProxyShutdownEvent());
                
                server.close();
            } catch (Exception e) {
                log.error("关闭网络服务时出错", e);
            }
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        log.debug("正在清理资源...");
        // 可以在这里添加其他清理逻辑
    }
}
