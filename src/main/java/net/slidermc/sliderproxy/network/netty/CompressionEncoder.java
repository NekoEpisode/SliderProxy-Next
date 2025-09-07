package net.slidermc.sliderproxy.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;

import java.util.zip.Deflater;

public class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    private final int threshold;
    private final Deflater deflater;
    private final byte[] buffer = new byte[8192]; // 重用缓冲区

    public CompressionEncoder(int threshold) {
        this.threshold = threshold;
        this.deflater = new Deflater();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int uncompressedLength = msg.readableBytes();

        if (uncompressedLength < threshold) {
            // 不压缩 - 写入 0 表示未压缩
            MinecraftProtocolHelper.writeVarInt(out, 0);
            out.writeBytes(msg);
        } else {
            // 压缩
            byte[] uncompressedData = new byte[uncompressedLength];
            msg.readBytes(uncompressedData);

            deflater.setInput(uncompressedData);
            deflater.finish();

            // 使用重用的缓冲区
            int compressedLength = deflater.deflate(buffer);
            deflater.reset();

            // 写入解压缩后的长度和压缩数据
            MinecraftProtocolHelper.writeVarInt(out, uncompressedLength);
            out.writeBytes(buffer, 0, compressedLength);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        deflater.end();
    }

    public int getThreshold() {
        return threshold;
    }
}