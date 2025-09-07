package net.slidermc.sliderproxy.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;

import java.util.List;

/**
 * Minecraft 协议帧解码器：
 * 基于 VarInt Length 将 TCP 流切分为完整的 Minecraft 包。
 */
public class FrameDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex(); // 标记当前位置，用于回滚

        // 尝试读取 VarInt (Length)
        int length = MinecraftProtocolHelper.readVarInt(in);
        if (length == -1) {
            // VarInt 还没读全，回滚
            in.resetReaderIndex();
            return;
        }

        // 检查剩余可读字节是否够这个包
        if (in.readableBytes() < length) {
            // 数据不完整，回滚
            in.resetReaderIndex();
            return;
        }

        // 把完整的包切出来
        ByteBuf frame = in.readBytes(length);
        out.add(frame); // 交给下一个 handler
    }
}
