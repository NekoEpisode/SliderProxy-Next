package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.api.server.ServerManager;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

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
        if (command.startsWith("server")) {
            ProxiedServer target = ServerManager.getInstance().getServer(command.split(" ")[1]);
            if (target != null) {
                ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
                if (player != null) {
                    player.connectTo(target);
                }
            }
            return HandleResult.UNFORWARD;
        }
        return HandleResult.FORWARD;
    }
}
