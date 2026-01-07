package net.slidermc.sliderproxy.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import java.util.List;

/**
 * AES/CFB8 解密处理器
 */
public class CipherDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final Cipher cipher;
    private byte[] inputBuffer = new byte[0];
    private byte[] outputBuffer = new byte[0];

    public CipherDecoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        
        // 确保输入缓冲区足够大
        if (inputBuffer.length < readableBytes) {
            inputBuffer = new byte[readableBytes];
        }
        
        // 读取加密数据
        in.readBytes(inputBuffer, 0, readableBytes);
        
        // 计算解密后的输出大小
        int outputSize = cipher.getOutputSize(readableBytes);
        if (outputBuffer.length < outputSize) {
            outputBuffer = new byte[outputSize];
        }
        
        // 解密数据
        int decryptedLength;
        try {
            decryptedLength = cipher.update(inputBuffer, 0, readableBytes, outputBuffer);
        } catch (ShortBufferException e) {
            throw new RuntimeException("Cipher output buffer too small", e);
        }
        
        // 创建输出 ByteBuf
        ByteBuf decrypted = ctx.alloc().heapBuffer(decryptedLength);
        decrypted.writeBytes(outputBuffer, 0, decryptedLength);
        out.add(decrypted);
    }
}
