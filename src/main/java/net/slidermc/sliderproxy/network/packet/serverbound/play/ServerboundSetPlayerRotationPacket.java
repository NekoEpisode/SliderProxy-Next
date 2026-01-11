package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerPositionEvent;
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
            // 触发位置事件（只有旋转）
            PlayerPositionEvent positionEvent = new PlayerPositionEvent(
                player, 
                player.getX(), 
                player.getY(), 
                player.getZ(), 
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
            player.setYaw(positionEvent.getYaw());
            player.setPitch(positionEvent.getPitch());
            
            // 如果值被修改，更新数据包
            if (positionEvent.getYaw() != yaw || positionEvent.getPitch() != pitch) {
                this.yaw = positionEvent.getYaw();
                this.pitch = positionEvent.getPitch();
            }
        }
        return HandleResult.FORWARD;
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
