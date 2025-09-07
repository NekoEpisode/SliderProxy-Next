package net.slidermc.sliderproxy.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;

import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class CompressionDecoder extends ByteToMessageDecoder {
    private final int threshold;
    private final Inflater inflater;

    public CompressionDecoder(int threshold) {
        this.threshold = threshold;
        this.inflater = new Inflater();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!in.isReadable()) return;

        // 标记读取位置，以防解析失败需要回退
        in.markReaderIndex();

        try {
            int uncompressedLength = MinecraftProtocolHelper.readVarInt(in);

            if (uncompressedLength == 0) {
                // 未压缩的包 - 直接输出剩余数据
                ByteBuf uncompressed = in.readRetainedSlice(in.readableBytes());
                out.add(uncompressed);
            } else {
                // 压缩的包 - 解压缩
                byte[] compressedData = new byte[in.readableBytes()];
                in.readBytes(compressedData);

                byte[] uncompressedData = new byte[uncompressedLength];
                inflater.setInput(compressedData);

                int resultLength = inflater.inflate(uncompressedData);
                if (resultLength != uncompressedLength) {
                    throw new DataFormatException("解压缩长度不匹配: 期望 " + uncompressedLength + ", 实际 " + resultLength);
                }
                inflater.reset();

                out.add(Unpooled.wrappedBuffer(uncompressedData));
            }
        } catch (Exception e) {
            // 解析失败，回退到原始位置
            in.resetReaderIndex();
            throw e;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        inflater.end();
    }

    public int getThreshold() {
        return threshold;
    }
}