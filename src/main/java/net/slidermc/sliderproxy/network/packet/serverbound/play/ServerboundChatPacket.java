package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerChatEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundChatPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundChatPacket.class);

    private String message;
    private long timestamp;
    private long salt;
    private byte[] signature; // 256 bytes when present
    private int messageCount;
    private byte[] acknowledged; // Fixed BitSet (20 bits = 3 bytes)
    private byte checksum;

    public ServerboundChatPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {
        this.message = MinecraftProtocolHelper.readString(byteBuf);
        this.timestamp = byteBuf.readLong();
        this.salt = byteBuf.readLong();
        
        // 读取可选签名 (Prefixed Optional Byte Array)
        boolean hasSignature = byteBuf.readBoolean();
        if (hasSignature) {
            this.signature = new byte[256];
            byteBuf.readBytes(this.signature);
        } else {
            this.signature = null;
        }
        
        this.messageCount = MinecraftProtocolHelper.readVarInt(byteBuf);
        
        // Fixed BitSet (20 bits = 3 bytes)
        this.acknowledged = new byte[3];
        byteBuf.readBytes(this.acknowledged);
        
        // Checksum
        this.checksum = byteBuf.readByte();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, message);
        byteBuf.writeLong(timestamp);
        byteBuf.writeLong(salt);
        
        if (signature != null && signature.length == 256) {
            byteBuf.writeBoolean(true);
            byteBuf.writeBytes(signature);
        } else {
            byteBuf.writeBoolean(false);
        }
        
        MinecraftProtocolHelper.writeVarInt(byteBuf, messageCount);
        byteBuf.writeBytes(acknowledged);
        byteBuf.writeByte(checksum);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        PlayerConnection connection = PlayerConnection.fromChannel(ctx.channel());
        if (connection == null) {
            return HandleResult.FORWARD;
        }

        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByConnection(connection);
        if (player == null) {
            return HandleResult.FORWARD;
        }

        // 触发聊天事件
        PlayerChatEvent event = new PlayerChatEvent(player, message);
        EventRegistry.callEvent(event);

        if (event.isCancelled()) {
            log.debug("Chat message from {} was cancelled: {}", player.getName(), message);
            return HandleResult.UNFORWARD;
        }

        // 如果消息被修改，需要重新构建数据包
        // 注意：由于聊天签名机制，修改消息内容会导致签名验证失败
        // 在 online-mode 服务器上，这可能导致玩家被踢出
        if (!event.getMessage().equals(message)) {
            log.debug("Chat message from {} was modified: {} -> {}", player.getName(), message, event.getMessage());
            // 更新消息内容（签名将失效）
            this.message = event.getMessage();
            this.signature = null; // 清除签名，因为消息已被修改
        }

        return HandleResult.FORWARD;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSalt() {
        return salt;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public byte[] getAcknowledged() {
        return acknowledged;
    }

    public byte getChecksum() {
        return checksum;
    }
}
