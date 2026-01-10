package net.slidermc.sliderproxy.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.api.command.CommandSource;
import net.slidermc.sliderproxy.api.command.SimpleCommand;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.api.server.ServerManager;
import net.slidermc.sliderproxy.translate.TranslateManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 服务器切换命令
 * 用法: /server [服务器名称]
 */
public class ServerCommand extends SimpleCommand {

    public ServerCommand() {
        super("server", "切换到指定服务器或查看当前服务器", 0);
    }

    @Override
    protected LiteralArgumentBuilder<CommandSource> buildCommand() {
        return LiteralArgumentBuilder.<CommandSource>literal("server")
                // /server <服务器名称>
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("name", StringArgumentType.word())
                        .suggests(this::suggestServers)
                        .executes(this::executeConnect))
                // /server (无参数，显示当前服务器)
                .executes(this::executeShowCurrent);
    }

    /**
     * 执行服务器切换
     */
    private int executeConnect(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String serverName = StringArgumentType.getString(context, "name");

        // 检查是否为玩家
        if (!source.isPlayer()) {
            source.sendMessage(Component.text(TranslateManager.translate("sliderproxy.command.server.playeronly"), NamedTextColor.RED));
            return 0;
        }

        ProxiedPlayer player = source.asPlayer();
        if (player == null) return 0;

        // 获取目标服务器
        ProxiedServer targetServer = ServerManager.getInstance().getServer(serverName);
        if (targetServer == null) {
            source.sendMessage(Component.text(player.translate("sliderproxy.command.server.notfound", serverName), NamedTextColor.RED));
            return 0;
        }

        // 检查是否已经在该服务器
        if (player.getConnectedServer() != null && player.getConnectedServer().equals(targetServer)) {
            source.sendMessage(Component.text(player.translate("sliderproxy.command.server.already", serverName), NamedTextColor.YELLOW));
            return 0;
        }

        // 执行连接
        player.connectTo(targetServer).whenComplete((result, throwable) -> {
            if (throwable != null) {
                source.sendMessage(Component.text(player.translate("sliderproxy.command.server.connectfailed", serverName, throwable.getMessage()), NamedTextColor.RED));
            }
        });

        return 1;
    }

    /**
     * 显示当前服务器
     */
    private int executeShowCurrent(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        // 检查是否为玩家
        if (!source.isPlayer()) {
            source.sendMessage(Component.text(Objects.requireNonNull(TranslateManager.translate("sliderproxy.command.server.playeronly")), NamedTextColor.RED));
            return 0;
        }

        ProxiedPlayer player = source.asPlayer();
        if (player == null) return 0;
        
        ProxiedServer currentServer = player.getConnectedServer();

        if (currentServer == null) {
            source.sendMessage(Component.text(player.translate("sliderproxy.command.server.notconnected"), NamedTextColor.YELLOW));
        } else {
            source.sendMessage(Component.text(player.translate("sliderproxy.command.server.current", currentServer.getName()), NamedTextColor.GREEN));
        }

        return 1;
    }

    /**
     * 提供服务器名称建议
     */
    private CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        // 获取所有服务器名称并添加到建议中
        ServerManager.getInstance().getAllServers().forEach(server -> builder.suggest(server.getName()));
        return builder.buildFuture();
    }
}
