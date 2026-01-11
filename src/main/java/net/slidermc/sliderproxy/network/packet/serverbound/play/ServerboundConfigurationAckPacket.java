package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.client.MinecraftNettyClient;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundConfigurationAckPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundConfigurationAckPacket.class);

    public ServerboundConfigurationAckPacket() {}

    @Override
    public void read(ByteBuf byteBuf) {}

    @Override
    public void write(ByteBuf byteBuf) {}

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player != null) {
            log.debug("收到配置确认包: 玩家={}", player.getName());
            
            // 触发 PlayerPreConfigurationEvent
            net.slidermc.sliderproxy.api.event.events.PlayerPreConfigurationEvent preConfigEvent = 
                new net.slidermc.sliderproxy.api.event.events.PlayerPreConfigurationEvent(player);
            net.slidermc.sliderproxy.api.event.EventRegistry.callEvent(preConfigEvent);

            if (player.isSwitchingServer()) {
                log.debug("玩家正在切换服务器，处理配置确认: 玩家={}", player.getName());
                player.getPlayerConnection().setUpstreamInboundProtocolState(ProtocolState.CONFIGURATION);
                // 通知连接请求处理配置确认
                player.handleConfigurationAck();
            } else {
                // 下游发的Start Configuration
                log.debug("收到ConfigurationAck，但SliderProxy并没有切换服务器，可能是来自下游");
                player.getPlayerConnection().setUpstreamInboundProtocolState(ProtocolState.CONFIGURATION);
                player.getPlayerConnection().setUpstreamOutboundProtocolState(ProtocolState.CONFIGURATION);
                log.debug("设置上游状态到 CONFIGURATION");
                MinecraftNettyClient client = player.getDownstreamClient();
                if (client != null) {
                    client.getChannel()
                            .writeAndFlush(new ServerboundConfigurationAckPacket())
                            .addListener((ChannelFutureListener) future -> {
                                if (future.isSuccess()) {
                                    player.getDownstreamClient().setInboundProtocolState(ProtocolState.CONFIGURATION);
                                    player.getDownstreamClient().setOutboundProtocolState(ProtocolState.CONFIGURATION);
                                    log.debug("ConfigurationAck 已发送，设置下游状态到 CONFIGURATION");
                                } else {
                                    log.error("ConfigurationAck 发送失败", future.cause());
                                }
                            });
                }
            }
            return HandleResult.UNFORWARD;
        }

        return HandleResult.FORWARD;
    }
}