package net.slidermc.sliderproxy.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/**
 * 表示一个可执行的命令
 */
public interface Command {
    /**
     * 获取命令的Brigadier构建器
     * 
     * @return 命令的LiteralArgumentBuilder
     */
    LiteralArgumentBuilder<CommandSource> getCommandBuilder();
    
    /**
     * 获取命令名称
     * 
     * @return 命令名称
     */
    String getName();
    
    /**
     * 获取命令描述
     * 
     * @return 命令描述
     */
    default String getDescription() {
        return "";
    }
    
    /**
     * 获取命令所需的权限等级
     * 
     * @return 权限等级 (0-4)
     */
    default int getPermissionLevel() {
        return 0;
    }
}
