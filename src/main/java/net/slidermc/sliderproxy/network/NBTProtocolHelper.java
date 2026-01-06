package net.slidermc.sliderproxy.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.kyori.adventure.nbt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 基于adventure-nbt的NBT序列化工具类
 * 处理Minecraft网络NBT格式（1.20.2+）
 */
public class NBTProtocolHelper {
    private static final Logger log = LoggerFactory.getLogger(NBTProtocolHelper.class);

    /**
     * 从ByteBuf读取网络NBT（1.20.2+格式）
     * 网络NBT没有根标签名，直接是TAG内容
     * 支持字符串和复合标签两种格式
     */
    public static BinaryTag readNetworkNBT(ByteBuf buf) {
        if (buf.readableBytes() == 0) {
            return CompoundBinaryTag.empty();
        }

        // 标记当前位置以便回退
        buf.markReaderIndex();

        try {
            // 先尝试读取标签类型
            byte tagType = buf.readByte();
            buf.resetReaderIndex(); // 重置读取位置

            if (tagType == BinaryTagTypes.STRING.id()) {
                // 处理字符串标签
                return readStringTag(buf);
            } else if (tagType == BinaryTagTypes.COMPOUND.id()) {
                // 处理复合标签
                return readCompoundTag(buf);
            } else {
                log.warn("Unsupported NBT tag type: {}", tagType);
                return CompoundBinaryTag.empty();
            }
        } catch (Exception e) {
            log.error("Failed to determine NBT tag type", e);
            return CompoundBinaryTag.empty();
        }
    }

    /**
     * 读取字符串标签
     */
    private static StringBinaryTag readStringTag(ByteBuf buf) {
        try {
            buf.readByte();

            int length = buf.readUnsignedShort();
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);

            String value = new String(bytes, StandardCharsets.UTF_8);
            return StringBinaryTag.stringBinaryTag(value);
        } catch (Exception e) {
            log.error("Failed to read string tag", e);
            return StringBinaryTag.stringBinaryTag("");
        }
    }

    /**
     * 读取复合标签
     */
    private static CompoundBinaryTag readCompoundTag(ByteBuf buf) {
        try {
            ByteBufInputStream input = new ByteBufInputStream(buf);
            return BinaryTagIO.reader().readNameless((InputStream) input);
        } catch (IOException e) {
            log.error("Failed to read compound tag", e);
            return CompoundBinaryTag.empty();
        }
    }

    /**
     * 写入网络NBT到ByteBuf（1.20.2+格式）
     * 网络NBT没有根标签名，直接写入TAG内容
     */
    public static void writeNetworkNBT(ByteBuf buf, BinaryTag nbt) {
        try {
            ByteBufOutputStream output = new ByteBufOutputStream(buf);

            if (nbt instanceof CompoundBinaryTag) {
                BinaryTagIO.writer().writeNameless((CompoundBinaryTag) nbt, (OutputStream) output);
            } else if (nbt instanceof StringBinaryTag stringTag) {
                // 写入字符串标签
                buf.writeByte(BinaryTagTypes.STRING.id());
                byte[] bytes = stringTag.value().getBytes(StandardCharsets.UTF_8);
                buf.writeShort(bytes.length);
                buf.writeBytes(bytes);
            } else {
                log.warn("Unsupported NBT type for writing: {}", nbt.type());
                // 写入空Compound作为fallback
                writeEmptyCompound(buf);
            }
        } catch (IOException e) {
            log.error("Failed to write network NBT", e);
            // 写入空Compound作为fallback
            writeEmptyCompound(buf);
        }
    }

    /**
     * 读取CompoundBinaryTag（网络格式）
     * 支持字符串和复合标签两种格式
     */
    public static CompoundBinaryTag readCompound(ByteBuf buf) {
        BinaryTag tag = readNetworkNBT(buf);

        if (tag instanceof CompoundBinaryTag compound) {
            return compound;
        } else if (tag instanceof StringBinaryTag stringTag) {
            // 将字符串标签转换为复合标签
            return CompoundBinaryTag.builder()
                    .putString("text", stringTag.value())
                    .build();
        }

        log.warn("Expected CompoundBinaryTag or StringBinaryTag, got {}", tag != null ? tag.type() : "null");
        return CompoundBinaryTag.empty();
    }

    /**
     * 写入CompoundBinaryTag（网络格式）
     */
    public static void writeCompound(ByteBuf buf, CompoundBinaryTag compound) {
        writeNetworkNBT(buf, compound);
    }

    /**
     * 创建简单的文本Component对应的NBT
     */
    public static CompoundBinaryTag createTextComponent(String text) {
        return CompoundBinaryTag.builder()
                .putString("text", text)
                .build();
    }

    /**
     * 从NBT中提取文本内容
     */
    public static String extractText(CompoundBinaryTag nbt) {
        return nbt.getString("text");
    }

    /**
     * 检查NBT是否包含文本组件
     */
    public static boolean isTextComponent(CompoundBinaryTag nbt) {
        return nbt.get("text") != null;
    }

    /**
     * 写入空的Compound Tag
     */
    private static void writeEmptyCompound(ByteBuf buf) {
        try {
            ByteBufOutputStream output = new ByteBufOutputStream(buf);
            BinaryTagIO.writer().write(CompoundBinaryTag.empty(), (OutputStream) output);
        } catch (IOException e) {
            // 手动写入空Compound
            buf.writeByte(BinaryTagTypes.COMPOUND.id());
            buf.writeByte(0); // 名称长度 (空字符串)
            buf.writeByte(0); // 结束标记
        }
    }

    /**
     * 读取NBT并转换为调试字符串
     */
    public static String nbtToString(ByteBuf buf) {
        try {
            BinaryTag tag = readNetworkNBT(buf);
            return tag.toString();
        } catch (Exception e) {
            return "Invalid NBT data";
        }
    }

    /**
     * 合并两个Compound tags
     */
    public static CompoundBinaryTag mergeCompounds(CompoundBinaryTag first, CompoundBinaryTag second) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

        for (String key : first.keySet()) {
            builder.put(key, Objects.requireNonNull(first.get(key)));
        }

        for (String key : second.keySet()) {
            builder.put(key, Objects.requireNonNull(second.get(key)));
        }

        return builder.build();
    }
}