package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ServerboundKeepAlivePlayPacket implements IMinecraftPacket {
    private long keepAliveId;

    public ServerboundKeepAlivePlayPacket() {}

    public ServerboundKeepAlivePlayPacket(long keepAliveId) {
        this.keepAliveId = keepAliveId;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.keepAliveId = byteBuf.readLong();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeLong(this.keepAliveId);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        return HandleResult.FORWARD; // 暂时不处理KeepAlive，透传
    }

    public long getKeepAliveId() {
        return keepAliveId;
    }

    public void setKeepAliveId(long keepAliveId) {
        this.keepAliveId = keepAliveId;
    }
}
