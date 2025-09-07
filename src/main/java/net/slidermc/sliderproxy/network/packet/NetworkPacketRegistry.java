package net.slidermc.sliderproxy.network.packet;

import net.slidermc.sliderproxy.network.ProtocolState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkPacketRegistry {

    private static final NetworkPacketRegistry instance = new NetworkPacketRegistry();

    // Map<方向, Map<协议状态, Map<包ID, 包类>>>
    private final Map<PacketDirection, Map<ProtocolState, Map<Integer, Class<? extends IMinecraftPacket>>>> packetMap = new ConcurrentHashMap<>();

    private NetworkPacketRegistry() {}

    public static NetworkPacketRegistry getInstance() {
        return instance;
    }

    public void registerPacket(PacketDirection direction, ProtocolState state, int packetId, Class<? extends IMinecraftPacket> clazz) {
        packetMap
                .computeIfAbsent(direction, d -> new ConcurrentHashMap<>())
                .computeIfAbsent(state, s -> new ConcurrentHashMap<>())
                .put(packetId, clazz);
    }

    public @Nullable Class<? extends IMinecraftPacket> getPacketClass(PacketDirection direction, ProtocolState state, int packetId) {
        Map<ProtocolState, Map<Integer, Class<? extends IMinecraftPacket>>> stateMap = packetMap.get(direction);
        if (stateMap == null) return null;
        Map<Integer, Class<? extends IMinecraftPacket>> idMap = stateMap.get(state);
        return idMap != null ? idMap.get(packetId) : null;
    }

    public @Nullable IMinecraftPacket createPacket(PacketDirection direction, ProtocolState state, int packetId)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Class<? extends IMinecraftPacket> clazz = getPacketClass(direction, state, packetId);
        return clazz != null ? clazz.getDeclaredConstructor().newInstance() : null;
    }

    // @Deprecated
    public @Nullable PacketInfo getPacketInfo(Class<? extends IMinecraftPacket> clazz) {
        for (Map.Entry<PacketDirection, Map<ProtocolState, Map<Integer, Class<? extends IMinecraftPacket>>>> dirEntry : packetMap.entrySet()) {
            PacketDirection direction = dirEntry.getKey();
            for (Map.Entry<ProtocolState, Map<Integer, Class<? extends IMinecraftPacket>>> stateEntry : dirEntry.getValue().entrySet()) {
                ProtocolState state = stateEntry.getKey();
                for (Map.Entry<Integer, Class<? extends IMinecraftPacket>> idEntry : stateEntry.getValue().entrySet()) {
                    if (idEntry.getValue().equals(clazz)) {
                        return new PacketInfo(idEntry.getKey(), state, direction, clazz);
                    }
                }
            }
        }
        return null;
    }

    public @Nullable PacketInfo getPacketInfo(PacketDirection direction, ProtocolState state, Class<? extends IMinecraftPacket> clazz) {
        Map<ProtocolState, Map<Integer, Class<? extends IMinecraftPacket>>> stateMap = packetMap.get(direction);
        if (stateMap == null) return null;

        Map<Integer, Class<? extends IMinecraftPacket>> idMap = stateMap.get(state);
        if (idMap == null) return null;

        for (Map.Entry<Integer, Class<? extends IMinecraftPacket>> entry : idMap.entrySet()) {
            if (entry.getValue().equals(clazz)) {
                return new PacketInfo(entry.getKey(), state, direction, clazz);
            }
        }
        return null;
    }
}
