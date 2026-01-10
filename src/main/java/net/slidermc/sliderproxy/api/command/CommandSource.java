package net.slidermc.sliderproxy.api.command;

import net.kyori.adventure.text.Component;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 命令源 - 表示执行命令的实体（玩家或控制台）
 */
public interface CommandSource {
    /**
     * 向命令源发送消息
     * 
     * @param message 消息内容
     */
    void sendMessage(String message);
    
    /**
     * 向命令源发送组件消息
     * 
     * @param component 消息组件
     */
    void sendMessage(Component component);
    
    /**
     * 获取命令源的名称
     * 
     * @return 名称
     */
    String getName();
    
    /**
     * 获取命令源的权限等级
     * 
     * @return 权限等级 (0-4)
     */
    int getPermissionLevel();
    
    /**
     * 检查命令源是否有指定权限等级
     * 
     * @param level 权限等级
     * @return 是否有权限
     */
    default boolean hasPermission(int level) {
        return getPermissionLevel() >= level;
    }
    
    /**
     * 如果命令源是玩家，返回玩家对象
     * 
     * @return 玩家对象，如果不是玩家则返回null
     */
    @Nullable
    ProxiedPlayer asPlayer();
    
    /**
     * 检查命令源是否为玩家
     * 
     * @return 是否为玩家
     */
    default boolean isPlayer() {
        return asPlayer() != null;
    }
    
    /**
     * 检查命令源是否为控制台
     * 
     * @return 是否为控制台
     */
    default boolean isConsole() {
        return !isPlayer();
    }
}
