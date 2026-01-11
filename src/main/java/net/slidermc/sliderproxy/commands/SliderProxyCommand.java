package net.slidermc.sliderproxy.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.ShutdownManager;
import net.slidermc.sliderproxy.api.command.CommandManager;
import net.slidermc.sliderproxy.api.command.CommandSource;
import net.slidermc.sliderproxy.api.command.SimpleCommand;

/**
 * SliderProxy 主命令
 * 用法: /sliderproxy <help|stop|reload|version>
 */
public class SliderProxyCommand extends SimpleCommand {

    public SliderProxyCommand() {
        super("sliderproxy", "SliderProxy 主命令", 0);
    }

    @Override
    protected LiteralArgumentBuilder<CommandSource> buildCommand() {
        return LiteralArgumentBuilder.<CommandSource>literal("sliderproxy")
                // /sliderproxy help
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(this::executeHelp))
                // /sliderproxy stop
                .then(LiteralArgumentBuilder.<CommandSource>literal("stop")
                        .executes(this::executeStop))
                // /sliderproxy end (别名)
                .then(LiteralArgumentBuilder.<CommandSource>literal("end")
                        .executes(this::executeStop))
                // /sliderproxy version
                .then(LiteralArgumentBuilder.<CommandSource>literal("version")
                        .executes(this::executeVersion))
                // /sliderproxy reload
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .executes(this::executeReload))
                // 无参数时显示帮助
                .executes(this::executeHelp);
    }

    /**
     * 执行帮助命令
     */
    private int executeHelp(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        
        source.sendMessage(Component.text("=== SliderProxy 命令帮助 ===", NamedTextColor.GOLD));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("SliderProxy 命令:", NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  /sliderproxy help - 显示此帮助信息", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  /sliderproxy stop - 关闭代理服务器", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  /sliderproxy version - 显示版本信息", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  /sliderproxy reload - 重载配置文件", NamedTextColor.GRAY));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("已注册的命令:", NamedTextColor.YELLOW));
        
        CommandManager.getInstance().getCommands().forEach((name, cmd) -> {
            String description = cmd.getDescription().isEmpty() ? "无描述" : cmd.getDescription();
            source.sendMessage(Component.text("  /" + name + " - " + description, NamedTextColor.GRAY));
        });
        
        return 1;
    }

    /**
     * 执行停止命令
     */
    private int executeStop(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        
        // 只允许控制台执行
        if (source.isPlayer()) {
            source.sendMessage(Component.text("此命令只能在控制台执行！", NamedTextColor.RED));
            return 0;
        }
        
        source.sendMessage(Component.text("正在关闭代理服务器...", NamedTextColor.YELLOW));
        
        // 使用 ShutdownManager 进行优雅关闭
        new Thread(() -> {
            try {
                Thread.sleep(100); // 短暂延迟让消息显示
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ShutdownManager.getInstance().shutdown();
        }, "Shutdown-Trigger").start();
        
        return 1;
    }

    /**
     * 执行版本命令
     */
    private int executeVersion(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        
        source.sendMessage(Component.text("=== SliderProxy ===", NamedTextColor.GOLD));
        source.sendMessage(Component.text("版本: 1.0.0-SNAPSHOT", NamedTextColor.GREEN));
        source.sendMessage(Component.text("Minecraft 版本: 1.21.4", NamedTextColor.GREEN));
        source.sendMessage(Component.text("作者: SliderMC", NamedTextColor.GREEN));
        
        return 1;
    }

    /**
     * 执行重载命令
     */
    private int executeReload(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        
        // 只允许控制台执行
        if (source.isPlayer()) {
            source.sendMessage(Component.text("此命令只能在控制台执行！", NamedTextColor.RED));
            return 0;
        }
        
        source.sendMessage(Component.text("配置重载功能尚未实现", NamedTextColor.YELLOW));
        // TODO: 实现配置重载逻辑
        
        return 1;
    }
}
