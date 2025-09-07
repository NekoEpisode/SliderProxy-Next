package net.slidermc.sliderproxy.api.player;

import io.netty.channel.Channel;
import net.slidermc.sliderproxy.network.connection.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家管理器 - 管理所有连接的玩家
 */
public class PlayerManager {
    private static final PlayerManager instance = new PlayerManager();

    private final Map<UUID, ProxiedPlayer> playersByUuid = new ConcurrentHashMap<>();
    private final Map<String, ProxiedPlayer> playersByName = new ConcurrentHashMap<>();
    private final Map<Channel, ProxiedPlayer> playersByUpstreamChannel = new ConcurrentHashMap<>();
    private final Map<Channel, ProxiedPlayer> playersByDownstreamChannel = new ConcurrentHashMap<>();

    /**
     * 注册新玩家
     */
    public void registerPlayer(@NotNull ProxiedPlayer player) {
        UUID uuid = player.getGameProfile().uuid();
        String name = player.getName().toLowerCase();

        playersByUuid.put(uuid, player);
        playersByName.put(name, player);
        playersByUpstreamChannel.put(player.getPlayerConnection().getUpstreamChannel(), player);

        Channel downstreamChannel = player.getPlayerConnection().getDownstreamChannel();
        if (downstreamChannel != null) {
            playersByDownstreamChannel.put(downstreamChannel, player);
        }
    }

    /**
     * 注销玩家
     */
    public void unregisterPlayer(@NotNull ProxiedPlayer player) {
        UUID uuid = player.getGameProfile().uuid();
        String name = player.getName().toLowerCase();

        playersByUuid.remove(uuid);
        playersByName.remove(name);
        playersByUpstreamChannel.remove(player.getPlayerConnection().getUpstreamChannel());

        Channel downstreamChannel = player.getPlayerConnection().getDownstreamChannel();
        if (downstreamChannel != null) {
            playersByDownstreamChannel.remove(downstreamChannel);
        }
    }

    /**
     * 通过UUID获取玩家
     */
    @Nullable
    public ProxiedPlayer getPlayerByUuid(@NotNull UUID uuid) {
        return playersByUuid.get(uuid);
    }

    /**
     * 通过用户名获取玩家
     */
    @Nullable
    public ProxiedPlayer getPlayerByName(@NotNull String name) {
        return playersByName.get(name.toLowerCase());
    }

    /**
     * 通过上游Channel获取玩家
     */
    @Nullable
    public ProxiedPlayer getPlayerByUpstreamChannel(@NotNull Channel channel) {
        return playersByUpstreamChannel.get(channel);
    }

    /**
     * 通过下游Channel获取玩家
     */
    @Nullable
    public ProxiedPlayer getPlayerByDownstreamChannel(@NotNull Channel channel) {
        return playersByDownstreamChannel.get(channel);
    }

    /**
     * 通过PlayerConnection获取玩家
     */
    @Nullable
    public ProxiedPlayer getPlayerByConnection(@NotNull PlayerConnection connection) {
        return playersByUpstreamChannel.get(connection.getUpstreamChannel());
    }

    /**
     * 更新玩家的下游Channel
     */
    public void updateDownstreamChannel(@NotNull ProxiedPlayer player, @Nullable Channel newDownstreamChannel) {
        // 移除旧的下游Channel映射
        Channel oldDownstreamChannel = player.getPlayerConnection().getDownstreamChannel();
        if (oldDownstreamChannel != null) {
            playersByDownstreamChannel.remove(oldDownstreamChannel);
        }

        // 更新新的下游Channel映射
        if (newDownstreamChannel != null) {
            playersByDownstreamChannel.put(newDownstreamChannel, player);
        }
    }

    /**
     * 获取所有在线玩家
     */
    @NotNull
    public Collection<ProxiedPlayer> getAllPlayers() {
        return Collections.unmodifiableCollection(playersByUuid.values());
    }

    /**
     * 获取在线玩家数量
     */
    public int getPlayerCount() {
        return playersByUuid.size();
    }

    /**
     * 检查玩家是否在线
     */
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return playersByUuid.containsKey(uuid);
    }

    /**
     * 检查玩家是否在线
     */
    public boolean isPlayerOnline(@NotNull String name) {
        return playersByName.containsKey(name.toLowerCase());
    }

    /**
     * 广播消息给所有玩家
     */
    public void broadcastMessage(@NotNull String message) {
        for (ProxiedPlayer player : getAllPlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * 根据条件过滤玩家
     */
    @NotNull
    public List<ProxiedPlayer> getPlayersByFilter(@NotNull java.util.function.Predicate<ProxiedPlayer> filter) {
        List<ProxiedPlayer> result = new ArrayList<>();
        for (ProxiedPlayer player : getAllPlayers()) {
            if (filter.test(player)) {
                result.add(player);
            }
        }
        return result;
    }

    /**
     * 清空所有玩家数据（服务器关闭时使用）
     */
    public void clearAllPlayers() {
        playersByUuid.clear();
        playersByName.clear();
        playersByUpstreamChannel.clear();
        playersByDownstreamChannel.clear();
    }

    public static PlayerManager getInstance() {
        synchronized (instance) {
            return instance;
        }
    }
}