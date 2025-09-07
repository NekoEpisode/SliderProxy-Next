package net.slidermc.sliderproxy.api.server;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxiedServer {
    private final InetSocketAddress address;
    private final String name;
    private final List<ProxiedPlayer> connectedPlayers = new CopyOnWriteArrayList<>();

    public ProxiedServer(InetSocketAddress address, String name) {
        this.address = address;
        this.name = name;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public List<ProxiedPlayer> getConnectedPlayers() {
        return connectedPlayers;
    }
}
