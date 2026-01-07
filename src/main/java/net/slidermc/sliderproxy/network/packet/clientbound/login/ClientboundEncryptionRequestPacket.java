package net.slidermc.sliderproxy.network.packet.clientbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

/**
 * Encryption Request 数据包 (Clientbound, Login State, Packet ID: 0x01)
 * 
 * 协议格式:
 * - Server ID: String (20) - 服务器ID，vanilla服务器总是发送空字符串
 * - Public Key: Prefixed Array of Byte - 服务器的公钥（DER编码）
 * - Verify Token: Prefixed Array of Byte - 随机验证令牌
 * - Should Authenticate: Boolean - 是否应该通过Mojang服务器验证
 */
public class ClientboundEncryptionRequestPacket implements IMinecraftPacket {

    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;
    private boolean shouldAuthenticate;

    public ClientboundEncryptionRequestPacket() {}

    public ClientboundEncryptionRequestPacket(String serverId, byte[] publicKey, byte[] verifyToken, boolean shouldAuthenticate) {
        this.serverId = serverId;
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
        this.shouldAuthenticate = shouldAuthenticate;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.serverId = MinecraftProtocolHelper.readString(byteBuf);
        this.publicKey = MinecraftProtocolHelper.readPrefixedByteArray(byteBuf);
        this.verifyToken = MinecraftProtocolHelper.readPrefixedByteArray(byteBuf);
        this.shouldAuthenticate = byteBuf.readBoolean();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, serverId);
        MinecraftProtocolHelper.writePrefixedByteArray(byteBuf, publicKey);
        MinecraftProtocolHelper.writePrefixedByteArray(byteBuf, verifyToken);
        byteBuf.writeBoolean(shouldAuthenticate);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        // 这是服务端发送给客户端的包，代理不需要处理
        return HandleResult.FORWARD;
    }

    public String getServerId() {
        return serverId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public boolean isShouldAuthenticate() {
        return shouldAuthenticate;
    }
}
