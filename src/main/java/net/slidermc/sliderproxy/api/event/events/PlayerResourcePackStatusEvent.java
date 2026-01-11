package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

import java.util.UUID;

/**
 * 玩家资源包状态事件
 * 在玩家响应资源包请求时触发
 */
public class PlayerResourcePackStatusEvent extends PlayerEvent {
    private final UUID packId;
    private final Status status;

    public PlayerResourcePackStatusEvent(ProxiedPlayer player, UUID packId, Status status) {
        super(player);
        this.packId = packId;
        this.status = status;
    }

    /**
     * 获取资源包 ID
     * @return 资源包 UUID
     */
    public UUID getPackId() {
        return packId;
    }

    /**
     * 获取资源包状态
     * @return 资源包状态
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 资源包状态枚举
     */
    public enum Status {
        /** 成功加载 */
        SUCCESSFULLY_LOADED,
        /** 已拒绝 */
        DECLINED,
        /** 下载失败 */
        FAILED_DOWNLOAD,
        /** 已接受 */
        ACCEPTED,
        /** 已下载 */
        DOWNLOADED,
        /** 无效的 URL */
        INVALID_URL,
        /** 加载失败 */
        FAILED_RELOAD,
        /** 已丢弃 */
        DISCARDED
    }
}
