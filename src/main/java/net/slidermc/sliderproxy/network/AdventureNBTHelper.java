package net.slidermc.sliderproxy.network;

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NBT-Component转换器
 * 支持Minecraft 1.20.3+的NBT格式和JSON格式
 */
public class AdventureNBTHelper {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final Gson GSON_PARSER = new Gson();

    /**
     * 从ByteBuf读取Component（自动处理NBT格式）
     */
    public static Component readComponent(ByteBuf buf) {
        CompoundBinaryTag nbt = NBTProtocolHelper.readCompound(buf);
        if (nbt == null) {
            return Component.empty();
        }
        JsonElement json = nbtToJson(nbt);
        return GSON.deserialize(GSON_PARSER.toJson(json));
    }

    /**
     * 将Component写入ByteBuf（使用NBT格式）
     */
    public static void writeComponent(ByteBuf buf, Component component) {
        String jsonStr = GSON.serialize(component);
        JsonElement json = GSON_PARSER.fromJson(jsonStr, JsonElement.class);
        BinaryTag nbt = jsonToNbt(json);
        NBTProtocolHelper.writeNetworkNBT(buf, nbt);
    }

    /**
     * NBT → JSON 完整转换
     */
    private static JsonElement nbtToJson(BinaryTag tag) {
        if (tag == null) {
            return JsonNull.INSTANCE;
        }

        switch (tag.type().id()) {
            case 1: // BYTE
                return new JsonPrimitive(((ByteBinaryTag) tag).value());
            case 2: // SHORT
                return new JsonPrimitive(((ShortBinaryTag) tag).value());
            case 3: // INT
                return new JsonPrimitive(((IntBinaryTag) tag).value());
            case 4: // LONG
                return new JsonPrimitive(((LongBinaryTag) tag).value());
            case 5: // FLOAT
                return new JsonPrimitive(((FloatBinaryTag) tag).value());
            case 6: // DOUBLE
                return new JsonPrimitive(((DoubleBinaryTag) tag).value());
            case 7: // BYTE_ARRAY
                byte[] byteArray = ((ByteArrayBinaryTag) tag).value();
                JsonArray jsonByteArray = new JsonArray(byteArray.length);
                for (byte b : byteArray) {
                    jsonByteArray.add(new JsonPrimitive(b));
                }
                return jsonByteArray;
            case 8: // STRING
                return new JsonPrimitive(((StringBinaryTag) tag).value());
            case 9: // LIST
                ListBinaryTag list = (ListBinaryTag) tag;
                JsonArray jsonArray = new JsonArray(list.size());
                for (BinaryTag item : list) {
                    jsonArray.add(nbtToJson(item));
                }
                return jsonArray;
            case 10: // COMPOUND
                CompoundBinaryTag compound = (CompoundBinaryTag) tag;
                JsonObject jsonObject = new JsonObject();
                for (String key : compound.keySet()) {
                    // 特殊处理空key（用于列表中的文本组件）
                    jsonObject.add(key.isEmpty() ? "text" : key, nbtToJson(compound.get(key)));
                }
                return jsonObject;
            case 11: // INT_ARRAY
                int[] intArray = ((IntArrayBinaryTag) tag).value();
                JsonArray jsonIntArray = new JsonArray(intArray.length);
                for (int i : intArray) {
                    jsonIntArray.add(new JsonPrimitive(i));
                }
                return jsonIntArray;
            case 12: // LONG_ARRAY
                long[] longArray = ((LongArrayBinaryTag) tag).value();
                JsonArray jsonLongArray = new JsonArray(longArray.length);
                for (long l : longArray) {
                    jsonLongArray.add(new JsonPrimitive(l));
                }
                return jsonLongArray;
            default: // END和其他未知类型
                return JsonNull.INSTANCE;
        }
    }

    /**
     * JSON → NBT 完整转换
     */
    private static BinaryTag jsonToNbt(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return EndBinaryTag.endBinaryTag();
        }

        if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                if (number instanceof Byte) {
                    return ByteBinaryTag.byteBinaryTag((Byte) number);
                } else if (number instanceof Short) {
                    return ShortBinaryTag.shortBinaryTag((Short) number);
                } else if (number instanceof Integer) {
                    return IntBinaryTag.intBinaryTag((Integer) number);
                } else if (number instanceof Long) {
                    return LongBinaryTag.longBinaryTag((Long) number);
                } else if (number instanceof Float) {
                    return FloatBinaryTag.floatBinaryTag((Float) number);
                } else if (number instanceof Double) {
                    return DoubleBinaryTag.doubleBinaryTag((Double) number);
                } else if (number instanceof LazilyParsedNumber) {
                    return IntBinaryTag.intBinaryTag(number.intValue());
                }
            } else if (primitive.isString()) {
                return StringBinaryTag.stringBinaryTag(primitive.getAsString());
            } else if (primitive.isBoolean()) {
                return ByteBinaryTag.byteBinaryTag((byte) (primitive.getAsBoolean() ? 1 : 0));
            }
        } else if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                builder.put(entry.getKey(), jsonToNbt(entry.getValue()));
            }
            return builder.build();
        } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            if (jsonArray.isEmpty()) {
                return ListBinaryTag.empty();
            }

            List<BinaryTag> tagItems = new ArrayList<>(jsonArray.size());
            BinaryTagType<? extends BinaryTag> listType = null;

            for (JsonElement jsonEl : jsonArray) {
                BinaryTag tag = jsonToNbt(jsonEl);
                tagItems.add(tag);

                if (listType == null) {
                    listType = tag.type();
                } else if (listType != tag.type()) {
                    listType = BinaryTagTypes.COMPOUND;
                }
            }

            if (listType == BinaryTagTypes.BYTE) {
                byte[] bytes = new byte[jsonArray.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = jsonArray.get(i).getAsNumber().byteValue();
                }
                return ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
            } else if (listType == BinaryTagTypes.INT) {
                int[] ints = new int[jsonArray.size()];
                for (int i = 0; i < ints.length; i++) {
                    ints[i] = jsonArray.get(i).getAsNumber().intValue();
                }
                return IntArrayBinaryTag.intArrayBinaryTag(ints);
            } else if (listType == BinaryTagTypes.LONG) {
                long[] longs = new long[jsonArray.size()];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = jsonArray.get(i).getAsNumber().longValue();
                }
                return LongArrayBinaryTag.longArrayBinaryTag(longs);
            }

            if (listType == BinaryTagTypes.COMPOUND) {
                for (int i = 0; i < tagItems.size(); i++) {
                    BinaryTag tag = tagItems.get(i);
                    if (tag.type() != BinaryTagTypes.COMPOUND) {
                        tagItems.set(i, CompoundBinaryTag.builder().put("", tag).build());
                    }
                }
            }

            if (listType != null) {
                return ListBinaryTag.listBinaryTag(listType, tagItems);
            }
        }

        return EndBinaryTag.endBinaryTag();
    }
}