package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ServerboundSetPlayerPositionPacket implements IMinecraftPacket {
    private double x;
    private double feetY;
    private double z;
    private byte flags;

    public ServerboundSetPlayerPositionPacket() {}

    public ServerboundSetPlayerPositionPacket(double x, double feetY, double z, byte flags) {
        this.x = x;
        this.feetY = feetY;
        this.z = z;
        this.flags = flags;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.x = byteBuf.readDouble();
        this.feetY = byteBuf.readDouble();
        this.z = byteBuf.readDouble();
        this.flags = byteBuf.readByte();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeDouble(this.x);
        byteBuf.writeDouble(this.feetY);
        byteBuf.writeDouble(this.z);
        byteBuf.writeByte(this.flags);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player != null) {
            player.setX(x);
            player.setY(feetY);
            player.setZ(z);
        }
        return HandleResult.FORWARD;
    }
}
