package net.slidermc.sliderproxy.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

/**
 * AES/CFB8 加密处理器
 */
public class CipherEncoder extends MessageToByteEncoder<ByteBuf> {

    private final Cipher cipher;
    private byte[] inputBuffer = new byte[0];
    private byte[] outputBuffer = new byte[0];

    public CipherEncoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int readableBytes = msg.readableBytes();
        
        // 确保输入缓冲区足够大
        if (inputBuffer.length < readableBytes) {
            inputBuffer = new byte[readableBytes];
        }
        
        // 读取明文数据
        msg.readBytes(inputBuffer, 0, readableBytes);
        
        // 计算加密后的输出大小
        int outputSize = cipher.getOutputSize(readableBytes);
        if (outputBuffer.length < outputSize) {
            outputBuffer = new byte[outputSize];
        }
        
        // 加密数据
        int encryptedLength;
        try {
            encryptedLength = cipher.update(inputBuffer, 0, readableBytes, outputBuffer);
        } catch (ShortBufferException e) {
            throw new RuntimeException("Cipher output buffer too small", e);
        }
        
        // 写入加密数据
        out.writeBytes(outputBuffer, 0, encryptedLength);
    }
}
