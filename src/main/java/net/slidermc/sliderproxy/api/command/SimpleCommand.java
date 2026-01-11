package net.slidermc.sliderproxy.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/**
 * 简单命令抽象类
 * 提供基础的命令实现框架
 */
public abstract class SimpleCommand implements Command {
    private final String name;
    private final String description;
    private final int permissionLevel;
    private final String[] aliases;
    
    public SimpleCommand(String name) {
        this(name, "", 0);
    }
    
    public SimpleCommand(String name, String description) {
        this(name, description, 0);
    }
    
    public SimpleCommand(String name, String description, int permissionLevel) {
        this(name, description, permissionLevel, new String[0]);
    }
    
    public SimpleCommand(String name, String description, int permissionLevel, String... aliases) {
        this.name = name;
        this.description = description;
        this.permissionLevel = permissionLevel;
        this.aliases = aliases;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public int getPermissionLevel() {
        return permissionLevel;
    }
    
    @Override
    public String[] getAliases() {
        return aliases;
    }
    
    /**
     * 构建命令
     * 子类需要实现此方法来定义命令的具体结构
     */
    protected abstract LiteralArgumentBuilder<CommandSource> buildCommand();
    
    @Override
    public LiteralArgumentBuilder<CommandSource> getCommandBuilder() {
        LiteralArgumentBuilder<CommandSource> builder = buildCommand();
        
        // 添加权限检查
        if (permissionLevel > 0) {
            builder.requires(source -> source.hasPermission(permissionLevel));
        }
        
        return builder;
    }
}
