package net.slidermc.sliderproxy.network.packet.serverbound.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.slidermc.sliderproxy.ProxyStaticValues;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerHandshakeEvent;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundDisconnectLoginPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ServerboundHandshakePacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundHandshakePacket.class);
    private int protocolVersion;
    private String serverAddress;
    private short serverPort;
    private int intent;

    public ServerboundHandshakePacket() {}

    public ServerboundHandshakePacket(int protocolVersion, String serverAddress, short serverPort, int intent) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.intent = intent;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.protocolVersion = MinecraftProtocolHelper.readVarInt(byteBuf);
        this.serverAddress = MinecraftProtocolHelper.readString(byteBuf);
        this.serverPort = byteBuf.readShort();
        this.intent = MinecraftProtocolHelper.readVarInt(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeVarInt(byteBuf, this.protocolVersion);
        MinecraftProtocolHelper.writeString(byteBuf, this.serverAddress);
        byteBuf.writeShort(this.serverPort);
        MinecraftProtocolHelper.writeVarInt(byteBuf, this.intent);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        // 触发握手事件
        PlayerHandshakeEvent handshakeEvent = new PlayerHandshakeEvent(
            serverAddress, 
            serverPort, 
            protocolVersion, 
            intent
        );
        EventRegistry.callEvent(handshakeEvent);
        
        // 握手事件不可取消，因为协议状态必须切换
        
        switch (this.intent) {
            case 1 -> {
                // Status
                ctx.channel().attr(PlayerConnection.KEY).get().setUpstreamInboundProtocolState(ProtocolState.STATUS); // 将(预测)客户端的状态设置为Status(Motd请求)
                ctx.channel().attr(PlayerConnection.KEY).get().setUpstreamOutboundProtocolState(ProtocolState.STATUS); // 切换代理自身的状态设置为Status
            }

            case 2 -> {
                // Login
                ctx.channel().attr(PlayerConnection.KEY).get().setUpstreamInboundProtocolState(ProtocolState.LOGIN); // 将(预测)客户端的状态设置为Login(登录请求)
                ctx.channel().attr(PlayerConnection.KEY).get().setUpstreamOutboundProtocolState(ProtocolState.LOGIN); // 切换代理自身的状态设置为Login

                if (protocolVersion != ProxyStaticValues.PROTOCOL_VERSION) {
                    String translate = TranslateManager.translate("sliderproxy.network.connection.kick.protocol", ProxyStaticValues.PROTOCOL_VERSION);
                    if (translate == null)
                        translate = "SliderProxy不支持你使用的版本！需要使用协议版本 " + ProxyStaticValues.PROXY_VERSION;
                    ctx.channel().writeAndFlush(new ClientboundDisconnectLoginPacket(Component.text(translate).color(NamedTextColor.RED)));
                }
            }

            case 3 -> {
                ctx.channel().attr(PlayerConnection.KEY).get().setUpstreamInboundProtocolState(ProtocolState.LOGIN); // 将(预测)客户端的状态设置为Login(登录请求)
                ctx.channel().attr(PlayerConnection.KEY).get().setUpstreamOutboundProtocolState(ProtocolState.LOGIN); // 切换代理自身的状态设置为Login

                if (protocolVersion != ProxyStaticValues.PROTOCOL_VERSION) {
                    String translate = TranslateManager.translate("sliderproxy.network.connection.kick.protocol", ProxyStaticValues.PROTOCOL_VERSION);
                    if (translate == null)
                        translate = "SliderProxy不支持你使用的版本！需要使用协议版本 " + ProxyStaticValues.PROXY_VERSION;
                    ctx.channel().writeAndFlush(new ClientboundDisconnectLoginPacket(Component.text(translate).color(NamedTextColor.RED)));
                }
            }
        }
        return HandleResult.UNFORWARD;
    }

    public int getIntent() {
        return intent;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public short getServerPort() {
        return serverPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ServerboundHandshakePacket that = (ServerboundHandshakePacket) o;
        return protocolVersion == that.protocolVersion && serverPort == that.serverPort && intent == that.intent && Objects.equals(serverAddress, that.serverAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolVersion, serverAddress, serverPort, intent);
    }

    @Override
    public String toString() {
        return "ServerboundHandshakePacket{" +
                "protocolVersion=" + protocolVersion +
                ", serverAddress='" + serverAddress + '\'' +
                ", serverPort=" + serverPort +
                ", intent=" + intent +
                '}';
    }

    public void setServerPort(short serverPort) {
        this.serverPort = serverPort;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public void setIntent(int intent) {
        this.intent = intent;
    }
}
