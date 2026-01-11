package net.slidermc.sliderproxy.network.packet.serverbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.ReceivePluginMessageEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ServerboundPluginMessagePacket implements IMinecraftPacket {
    private Key identifier;
    private byte[] data;

    @Override
    public void read(ByteBuf byteBuf) {
        this.identifier = Key.key(MinecraftProtocolHelper.readString(byteBuf));
        int remainingBytes = byteBuf.readableBytes();
        if (remainingBytes > 32767) {
            throw new RuntimeException("Plugin message data too large: " + remainingBytes + " bytes");
        }
        this.data = new byte[remainingBytes];
        byteBuf.readBytes(this.data);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, identifier.namespace() + ":" + identifier.value());
        byteBuf.writeBytes(data);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        // 检测客户端品牌信息
        if (identifier.asString().equals("minecraft:brand")) {
            ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
            if (player != null) {
                try {
                    // 读取品牌字符串（VarInt 长度 + UTF-8 字符串）
                    io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.wrappedBuffer(data);
                    String brand = MinecraftProtocolHelper.readString(buf);
                    buf.release();
                    
                    // 触发 PlayerClientBrandEvent
                    net.slidermc.sliderproxy.api.event.events.PlayerClientBrandEvent brandEvent = 
                        new net.slidermc.sliderproxy.api.event.events.PlayerClientBrandEvent(player, brand);
                    EventRegistry.callEvent(brandEvent);
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        
        ReceivePluginMessageEvent event = new ReceivePluginMessageEvent(identifier, data, ReceivePluginMessageEvent.From.UPSTREAM, ctx.channel());
        EventRegistry.callEvent(event);
        switch (event.getResult()) {
            case HANDLE_AND_FORWARD -> {
                this.data = event.getData();
                this.identifier = event.getIdentifier();
                ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
                if (player != null) {
                    MinecraftNettyClient client = player.getDownstreamClient();
                    if (client != null) {
                        client.getChannel().writeAndFlush(this);
                    }
                }
                return HandleResult.UNFORWARD;
            }
            case HANDLE_AND_NOT_FORWARD -> {
                this.data = event.getData();
                this.identifier = event.getIdentifier();
                return HandleResult.UNFORWARD;
            }
            case FORWARD -> {
                // 不使用修改后的值，直接转发原内容
                ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
                if (player != null) {
                    MinecraftNettyClient client = player.getDownstreamClient();
                    if (client != null) {
                        client.getChannel().writeAndFlush(this);
                    }
                }
                return HandleResult.UNFORWARD;
            }
            default -> {
                return HandleResult.UNFORWARD;
            }
        }
    }

    public Key getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Key identifier) {
        this.identifier = identifier;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
