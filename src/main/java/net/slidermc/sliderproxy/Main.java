package net.slidermc.sliderproxy;

import net.slidermc.sliderproxy.network.ProtocolState;
import net.slidermc.sliderproxy.network.packet.NetworkPacketRegistry;
import net.slidermc.sliderproxy.network.packet.PacketDirection;
import net.slidermc.sliderproxy.network.packet.clientbound.ClientboundPluginMessagePacket;
import net.slidermc.sliderproxy.network.packet.clientbound.configuration.ClientboundFinishConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.configuration.ClientboundKeepAliveConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundSetCompressionPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundKeepAlivePlayPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundStartConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundPongResponsePacket;
import net.slidermc.sliderproxy.network.packet.clientbound.status.ClientboundStatusResponsePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.ServerboundPluginMessagePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.configuration.ServerboundFinishConfigurationAckPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.configuration.ServerboundKeepAliveConfigurationPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.handshake.ServerboundHandshakePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundHelloPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.login.ServerboundLoginAcknowledgePacket;
import net.slidermc.sliderproxy.network.packet.serverbound.play.ServerboundKeepAlivePlayPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.status.ServerboundPingRequestPacket;
import net.slidermc.sliderproxy.network.packet.serverbound.status.ServerboundStatusRequestPacket;
import net.slidermc.sliderproxy.translate.TranslateManager;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        registerClientboundPackets();
        registerServerboundPackets();

        initLanguages(); // 初始化多语言

        SliderProxyServer server = new SliderProxyServer(new InetSocketAddress("0.0.0.0", 25565));
        server.run();
    }

    public static void initLanguages() {
        TranslateManager.loadFromResource("zh_cn", "lang/zh_cn.json");
    }

    private static void registerClientboundPackets() {
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.STATUS, 0x00, ClientboundStatusResponsePacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.STATUS, 0x01, ClientboundPongResponsePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x02, ClientboundLoginSuccessPacket.class);
        // NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x00, ClientboundDisconnectPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.LOGIN, 0x03, ClientboundSetCompressionPacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x04, ClientboundKeepAliveConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x03, ClientboundFinishConfigurationPacket.class);
        // NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x02, ClientboundDisconnectPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.CONFIGURATION, 0x01, ClientboundPluginMessagePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x6F, ClientboundStartConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x26, ClientboundKeepAlivePlayPacket.class);
        // NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x1C, ClientboundDisconnectPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.CLIENTBOUND, ProtocolState.PLAY, 0x18, ClientboundPluginMessagePacket.class);
    }

    private static void registerServerboundPackets() {
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.HANDSHAKE, 0x00, ServerboundHandshakePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.STATUS, 0x00, ServerboundStatusRequestPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.STATUS, 0x01, ServerboundPingRequestPacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.LOGIN, 0x00, ServerboundHelloPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.LOGIN, 0x03, ServerboundLoginAcknowledgePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x04, ServerboundKeepAliveConfigurationPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x03, ServerboundFinishConfigurationAckPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.CONFIGURATION, 0x02, ServerboundPluginMessagePacket.class);

        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x1B, ServerboundKeepAlivePlayPacket.class);
        NetworkPacketRegistry.getInstance().registerPacket(PacketDirection.SERVERBOUND, ProtocolState.PLAY, 0x15, ServerboundPluginMessagePacket.class);
    }
}
