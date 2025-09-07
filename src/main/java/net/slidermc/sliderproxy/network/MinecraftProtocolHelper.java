package net.slidermc.sliderproxy.network;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MinecraftProtocolHelper {

    /**
     * 读取 Minecraft VarInt
     */
    public static int readVarInt(ByteBuf buf) {
        int numRead = 0;
        int result = 0;
        while (buf.isReadable()) {
            byte read = buf.readByte();
            int value = read & 0b01111111;
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) throw new RuntimeException("VarInt too big");

            if ((read & 0b10000000) == 0) return result;
        }
        return -1; // 数据不完整
    }

    /**
     * 写 VarInt
     */
    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & 0xFFFFFF80) != 0L) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value & 0x7F);
    }

    /**
     * 读取 Minecraft VarLong
     */
    public static long readVarLong(ByteBuf buf) {
        int numRead = 0;
        long result = 0;
        while (buf.isReadable()) {
            byte read = buf.readByte();
            long value = read & 0b01111111L;
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 10) throw new RuntimeException("VarLong too big");

            if ((read & 0b10000000) == 0) return result;
        }
        return -1;
    }

    /**
     * 写 VarLong
     */
    public static void writeVarLong(ByteBuf buf, long value) {
        while ((value & 0xFFFFFFFFFFFFFF80L) != 0L) {
            buf.writeByte(((int)value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte((int)value & 0x7F);
    }

    /**
     * 读取长度前缀的 UTF-8 字符串
     */
    public static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        if (length < 0) throw new RuntimeException("Invalid string length: " + length);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 写长度前缀 UTF-8 字符串
     */
    public static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /**
     * 读取 UUID (16 字节)
     */
    public static UUID readUUID(ByteBuf buf) {
        long most = buf.readLong();
        long least = buf.readLong();
        return new UUID(most, least);
    }

    /**
     * 写 UUID
     */
    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * 读取 Boolean (1 byte)
     */
    public static boolean readBoolean(ByteBuf buf) {
        return buf.readByte() != 0;
    }

    /**
     * 写 Boolean (1 byte)
     */
    public static void writeBoolean(ByteBuf buf, boolean value) {
        buf.writeByte(value ? 1 : 0);
    }

    /**
     * 读取 Position (8 bytes)
     * x: 26-bit signed, y: 12-bit signed, z: 26-bit signed
     */
    public static int[] readPosition(ByteBuf buf) {
        long val = buf.readLong();
        int x = (int) (val >> 38);
        int y = (int) (val & 0xFFF);
        int z = (int) ((val << 26 >> 38));
        return new int[]{x, y, z};
    }

    /**
     * 写 Position
     */
    public static void writePosition(ByteBuf buf, int x, int y, int z) {
        long val = (((long)x & 0x3FFFFFF) << 38) | (((long)z & 0x3FFFFFF) << 12) | ((long)y & 0xFFF);
        buf.writeLong(val);
    }

    /**
     * 读取 Angle (1 byte)
     */
    public static float readAngle(ByteBuf buf) {
        return (buf.readUnsignedByte() * 360f) / 256f;
    }

    /**
     * 写 Angle (1 byte)
     */
    public static void writeAngle(ByteBuf buf, float degrees) {
        buf.writeByte((int)(degrees * 256f / 360f) & 0xFF);
    }

    /**
     * 读取固定长度 ByteArray
     */
    public static byte[] readByteArray(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    /**
     * 写固定长度 ByteArray
     */
    public static void writeByteArray(ByteBuf buf, byte[] bytes) {
        buf.writeBytes(bytes);
    }

    /**
     * 获取 VarInt 编码后的字节长度
     */
    public static int getVarIntSize(int value) {
        if ((value & 0xFFFFFF80) == 0) return 1;
        if ((value & 0xFFFFC000) == 0) return 2;
        if ((value & 0xFFE00000) == 0) return 3;
        if ((value & 0xF0000000) == 0) return 4;
        return 5;
    }
}
