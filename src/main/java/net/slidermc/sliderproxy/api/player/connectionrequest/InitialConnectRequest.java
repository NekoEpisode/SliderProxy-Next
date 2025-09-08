package net.slidermc.sliderproxy.api.player.connectionrequest;

import io.netty.channel.Channel;
import net.slidermc.sliderproxy.RunningData;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.server.ProxiedServer;
import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.netty.CompressionDecoder;
import net.slidermc.sliderproxy.network.netty.CompressionEncoder;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundSetCompressionPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 首次连接请求 - 处理玩家初始登录流程
 */
public class InitialConnectRequest extends ConnectRequest {
    private static final Logger log = LoggerFactory.getLogger(InitialConnectRequest.class);

    public InitialConnectRequest(ProxiedPlayer player, ProxiedServer targetServer) {
        super(player, targetServer, ConnectReason.INITIAL_CONNECT);
    }

    @Override
    protected CompletableFuture<Void> preConnect() {
        // 首次连接不需要特殊的准备工作
        // 协议状态已经在握手阶段设置完毕
        log.debug("首次连接准备: 玩家={}", player.getName());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> postConnect() {
        // 发送登录成功包并切换到配置状态
        return CompletableFuture.runAsync(() -> {
            player.getPlayerConnection().getUpstreamChannel().eventLoop().execute(() -> {
                try {
                    Channel ch = player.getPlayerConnection().getUpstreamChannel();
                    int threshold = RunningData.configuration.getInt("proxy.compress-threshold");

                    ch.eventLoop().execute(() -> { // FIXME: 我也不知道为什么这里一给上游加压缩器就爆炸，可能需要修复，给下游加似乎没问题
                       /* // 先明文发出 Set Compression
                        ch.writeAndFlush(new ClientboundSetCompressionPacket(threshold))
                                .addListener(f -> {
                                    if (!f.isSuccess()) {
                                        // 如果发送失败就别再装编码器了
                                        return;
                                    }
                                    // 确保发完后再装压缩器
                                    ch.pipeline().addAfter("frame-decoder", "compression-decoder",
                                            new CompressionDecoder(threshold));
                                    ch.pipeline().addBefore("packet-encoder", "compression-encoder",
                                            new CompressionEncoder(threshold));
                                });*/

                        // 发送登录成功包
                        ClientboundLoginSuccessPacket loginSuccessPacket = new ClientboundLoginSuccessPacket(
                                player.getGameProfile().uuid(),
                                player.getGameProfile().name(),
                                List.of()
                        );
                        player.getPlayerConnection().getUpstreamChannel().writeAndFlush(loginSuccessPacket);

                        // 切换到配置状态
                        player.getPlayerConnection().setUpstreamOutboundProtocolState(ProtocolState.CONFIGURATION);
                    });

                    log.info("首次连接完成: 玩家={}, 服务器={}",
                            player.getName(), targetServer.getName());

                } catch (Exception e) {
                    log.error("首次连接后处理失败: 玩家={}", player.getName(), e);
                    throw new RuntimeException(e);
                }
            });
        });
    }
}