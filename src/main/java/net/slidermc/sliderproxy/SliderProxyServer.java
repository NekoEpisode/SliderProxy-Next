package net.slidermc.sliderproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.slidermc.sliderproxy.network.netty.upstream.UpstreamChannelInitializer;
import net.slidermc.sliderproxy.translate.TranslateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class SliderProxyServer {
    private static final Logger log = LoggerFactory.getLogger(SliderProxyServer.class);
    private final InetSocketAddress address;
    private ServerBootstrap serverBootstrap;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public SliderProxyServer(InetSocketAddress address) {
        this.address = address;
    }

    public void run() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new UpstreamChannelInitializer());

        serverBootstrap.bind(address).syncUninterruptibly();
        log.info(TranslateManager.translate("sliderproxy.startup", address.getHostName(), address.getPort()));
    }

    public void close() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        System.out.println("Proxy server stopped.");
    }
}
