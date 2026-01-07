package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.api.server.ServerManager;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;

import java.util.Objects;

public class ServerboundChatCommandPacket implements IMinecraftPacket {
    private String command;

    public ServerboundChatCommandPacket() {}

    public ServerboundChatCommandPacket(String command) {
        this.command = command;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        String command = MinecraftProtocolHelper.readString(byteBuf);
        if (command.length() > 32767) throw new IllegalArgumentException("命令长度过长");
        this.command = command;
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, command);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player == null) return HandleResult.FORWARD;
        if (command.startsWith("server")) {
            String[] commands = command.split(" ");
            if (commands.length < 2) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(TranslateManager.translate("sliderproxy.command.server.noargs"))));
                return HandleResult.UNFORWARD;
            }
            ProxiedServer target = ServerManager.getInstance().getServer(commands[1]);
            if (target != null) {
                player.connectTo(target);
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(TranslateManager.translate("sliderproxy.command.server.notfound", commands[1]))));
            }
            return HandleResult.UNFORWARD;
        }
        return HandleResult.FORWARD;
    }
}
