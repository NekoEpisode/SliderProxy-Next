package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundSetPlayerPositionAndRotationPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundSetPlayerPositionAndRotationPacket.class);
    private double x;
    private double feetY;
    private double z;
    private float yaw;
    private float pitch;
    private byte flags;

    public ServerboundSetPlayerPositionAndRotationPacket() {}

    public ServerboundSetPlayerPositionAndRotationPacket(double x, double feetY, double z, float yaw, float pitch, byte flags) {
        this.x = x;
        this.feetY = feetY;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.flags = flags;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.x = byteBuf.readDouble();
        this.feetY = byteBuf.readDouble();
        this.z = byteBuf.readDouble();
        this.yaw = byteBuf.readFloat();
        this.pitch = byteBuf.readFloat();
        this.flags = byteBuf.readByte();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeDouble(this.x);
        byteBuf.writeDouble(this.feetY);
        byteBuf.writeDouble(this.z);
        byteBuf.writeFloat(this.yaw);
        byteBuf.writeFloat(this.pitch);
        byteBuf.writeByte(this.flags);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player != null) {
            player.setX(x);
            player.setY(feetY);
            player.setZ(z);
            player.setYaw(yaw);
            player.setPitch(pitch);
        }
        return HandleResult.FORWARD;
    }
}
