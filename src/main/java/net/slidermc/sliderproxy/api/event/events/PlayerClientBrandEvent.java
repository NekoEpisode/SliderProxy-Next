package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家客户端品牌事件
 * 在接收到玩家的客户端品牌信息时触发
 * 
 * 注意：此事件不能被取消，因为品牌信息已经接收
 */
public class PlayerClientBrandEvent extends PlayerEvent {
    private String brand;

    public PlayerClientBrandEvent(ProxiedPlayer player, String brand) {
        super(player);
        this.brand = brand;
    }

    /**
     * 获取客户端品牌（如 "vanilla", "forge", "fabric" 等）
     * @return 客户端品牌
     */
    public String getBrand() {
        return brand;
    }

    /**
     * 设置客户端品牌
     * @param brand 新的客户端品牌
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerClientBrandEvent cannot be cancelled - brand information has already been received");
    }
}
