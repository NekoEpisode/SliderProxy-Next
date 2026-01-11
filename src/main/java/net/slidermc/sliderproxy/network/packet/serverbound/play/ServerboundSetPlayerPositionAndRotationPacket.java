package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerPositionEvent;
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
            // 触发位置事件
            PlayerPositionEvent positionEvent = new PlayerPositionEvent(
                player, 
                x, 
                feetY, 
                z, 
                yaw, 
                pitch, 
                (flags & 0x01) != 0 // onGround flag
            );
            EventRegistry.callEvent(positionEvent);
            
            // 如果事件被取消，不更新位置也不转发
            if (positionEvent.isCancelled()) {
                return HandleResult.UNFORWARD;
            }
            
            // 应用可能被修改的值
            player.setX(positionEvent.getX());
            player.setY(positionEvent.getY());
            player.setZ(positionEvent.getZ());
            player.setYaw(positionEvent.getYaw());
            player.setPitch(positionEvent.getPitch());
            
            // 如果值被修改，更新数据包
            if (positionEvent.getX() != x || positionEvent.getY() != feetY || positionEvent.getZ() != z ||
                positionEvent.getYaw() != yaw || positionEvent.getPitch() != pitch) {
                this.x = positionEvent.getX();
                this.feetY = positionEvent.getY();
                this.z = positionEvent.getZ();
                this.yaw = positionEvent.getYaw();
                this.pitch = positionEvent.getPitch();
            }
        }
        return HandleResult.FORWARD;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getFeetY() {
        return feetY;
    }

    public void setFeetY(double feetY) {
        this.feetY = feetY;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }
}
