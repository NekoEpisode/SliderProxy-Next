package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.ProtocolState;
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

            if (player.isSwitchingServer()) {
                log.debug("玩家正在切换服务器，处理配置确认: 玩家={}", player.getName());

                // 设置上游入站状态为配置状态
                player.getPlayerConnection().setUpstreamInboundProtocolState(ProtocolState.CONFIGURATION);

                // 通知连接请求处理配置确认
                player.handleConfigurationAck();

                // 不转发这个包，因为是服务器切换流程的一部分
                return HandleResult.UNFORWARD;
            }
        }

        // 如果不是服务器切换，正常转发
        return HandleResult.FORWARD;
    }
}