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
        // msg 来自 PacketEncoder，格式为：Packet Length + Packet ID + Data
        // 我们需要提取 Packet ID + Data，然后压缩

        // 读取并丢弃原有的 Packet Length（我们稍后会重新计算）
        int originalPacketLength = MinecraftProtocolHelper.readVarInt(msg);

        // 剩余的是 Packet ID + Data
        int dataLength = msg.readableBytes();

        if (dataLength < threshold) {
            // 未压缩：Data Length = 0
            // 输出：Packet Length + Data Length(0) + Packet ID + Data
            MinecraftProtocolHelper.writeVarInt(out, dataLength + MinecraftProtocolHelper.getVarIntSize(0));
            MinecraftProtocolHelper.writeVarInt(out, 0);
            out.writeBytes(msg);
        } else {
            // 压缩：需要压缩 Packet ID + Data
            byte[] uncompressedData = new byte[dataLength];
            msg.readBytes(uncompressedData);

            deflater.setInput(uncompressedData);
            deflater.finish();

            // 使用重用的缓冲区进行压缩
            int compressedLength = deflater.deflate(buffer);
            deflater.reset();

            // 输出：Packet Length + Data Length + 压缩数据
            int packetLength = MinecraftProtocolHelper.getVarIntSize(dataLength) + compressedLength;
            MinecraftProtocolHelper.writeVarInt(out, packetLength);
            MinecraftProtocolHelper.writeVarInt(out, dataLength);
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