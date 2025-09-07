package net.slidermc.sliderproxy.api.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {
    private static final ServerManager instance = new ServerManager();

    private final Map<String, ProxiedServer> proxiedServerMap = new ConcurrentHashMap<>(); // name to server

    public void addServer(ProxiedServer server) {
        proxiedServerMap.put(server.getName(), server);
    }

    public void removeServer(String name) {
        proxiedServerMap.remove(name);
    }

    public ProxiedServer getServer(String name) {
        return proxiedServerMap.get(name);
    }

    public int getServerCount() {
        return proxiedServerMap.size();
    }

    public Collection<ProxiedServer> getAllServers() {
        return proxiedServerMap.values();
    }

    public boolean containsServer(String name) {
        return proxiedServerMap.containsKey(name);
    }

    public static ServerManager getInstance() {
        synchronized (ServerManager.class) {
            return instance;
        }
    }
}
