package net.slidermc.sliderproxy.network.packet.clientbound.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.netty.CompressionDecoder;
import net.slidermc.sliderproxy.network.netty.CompressionEncoder;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientboundSetCompressionPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundSetCompressionPacket.class);

    private int threshold;

    public ClientboundSetCompressionPacket() {}

    public ClientboundSetCompressionPacket(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        this.threshold = MinecraftProtocolHelper.readVarInt(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeVarInt(byteBuf, threshold);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        try {
            // 移除现有的压缩处理器
            removeExistingCompressionHandlers(ctx.pipeline());

            if (threshold > 0) {
                // 添加压缩解码器（接收方向）
                ctx.pipeline().addAfter("frame-decoder", "compression-decoder",
                        new CompressionDecoder(threshold));

                // 添加压缩编码器（发送方向）
                ctx.pipeline().addBefore("packet-encoder", "compression-encoder",
                        new CompressionEncoder(threshold));

                log.debug("已启用双向压缩，阈值: {}", threshold);
            } else {
                log.debug("禁用压缩");
            }

        } catch (Exception e) {
            log.error("设置压缩处理器失败", e);
        }

        return HandleResult.UNFORWARD;
    }

    private void removeExistingCompressionHandlers(ChannelPipeline pipeline) {
        if (pipeline.get("compression-decoder") != null) {
            pipeline.remove("compression-decoder");
        }
        if (pipeline.get("compression-encoder") != null) {
            pipeline.remove("compression-encoder");
        }
    }

    public int getThreshold() {
        return threshold;
    }
}