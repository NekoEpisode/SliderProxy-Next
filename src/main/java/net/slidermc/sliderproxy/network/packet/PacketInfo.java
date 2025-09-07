package net.slidermc.sliderproxy.network.packet;

import net.slidermc.sliderproxy.network.ProtocolState;

public record PacketInfo(int packetId, ProtocolState state, PacketDirection direction,
                         Class<? extends IMinecraftPacket> clazz) {
}
