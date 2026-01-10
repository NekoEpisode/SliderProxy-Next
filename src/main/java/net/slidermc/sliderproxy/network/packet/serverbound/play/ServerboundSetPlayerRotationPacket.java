package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ServerboundSetPlayerRotationPacket implements IMinecraftPacket {
    private float yaw;
    private float pitch;
    private byte flags;

    public ServerboundSetPlayerRotationPacket() {}

    public ServerboundSetPlayerRotationPacket(float yaw, float pitch, byte flags) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.flags = flags;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.yaw = byteBuf.readFloat();
        this.pitch = byteBuf.readFloat();
        this.flags = byteBuf.readByte();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeFloat(this.yaw);
        byteBuf.writeFloat(this.pitch);
        byteBuf.writeByte(this.flags);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player != null) {
            player.setYaw(yaw);
            player.setPitch(pitch);
        }
        return HandleResult.FORWARD;
    }
}
