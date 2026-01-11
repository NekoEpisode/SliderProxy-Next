package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.api.player.data.ClientInformation;
import net.slidermc.sliderproxy.api.utils.UnsignedByte;

/**
 * 玩家设置改变事件
 * 在玩家的客户端设置发生改变时触发
 * 
 * 注意：此事件不能被取消，因为设置已经更新
 */
public class PlayerSettingsChangedEvent extends PlayerEvent {
    private final ClientInformation clientInformation;

    public PlayerSettingsChangedEvent(ProxiedPlayer player, ClientInformation clientInformation) {
        super(player);
        this.clientInformation = clientInformation;
    }

    /**
     * 获取客户端信息
     * @return 客户端信息对象
     */
    public ClientInformation getClientInformation() {
        return clientInformation;
    }

    /**
     * 获取客户端语言设置
     * @return 语言代码（如 "zh_cn", "en_us"）
     */
    public String getLocale() {
        return clientInformation.getLocale();
    }

    /**
     * 获取视距设置
     * @return 视距（区块数）
     */
    public byte getViewDistance() {
        return clientInformation.getViewDistance();
    }

    /**
     * 获取聊天模式
     * @return 聊天模式
     */
    public ClientInformation.ChatMode getChatMode() {
        return clientInformation.getChatMode();
    }

    /**
     * 检查是否启用聊天颜色
     * @return 如果启用则返回 true
     */
    public boolean isChatColors() {
        return clientInformation.isChatColors();
    }

    /**
     * 获取显示的皮肤部位
     * @return 皮肤部位标志位
     */
    public UnsignedByte getDisplayedSkinParts() {
        return clientInformation.getDisplayedSkinParts();
    }

    /**
     * 获取主手设置
     * @return 主手类型
     */
    public ClientInformation.MainHandType getMainHandType() {
        return clientInformation.getMainHandType();
    }

    /**
     * 检查是否启用文本过滤
     * @return 如果启用则返回 true
     */
    public boolean isEnableTextFiltering() {
        return clientInformation.isEnableTextFiltering();
    }

    /**
     * 检查是否允许在服务器列表中显示
     * @return 如果允许则返回 true
     */
    public boolean isAllowServerListings() {
        return clientInformation.isAllowServerListings();
    }

    /**
     * 获取粒子状态设置
     * @return 粒子状态
     */
    public ClientInformation.ParticleStatus getParticleStatus() {
        return clientInformation.getParticleStatus();
    }

    @Override
    public void setCancelled(boolean cancelled) {
        throw new UnsupportedOperationException("PlayerSettingsChangedEvent cannot be cancelled - settings have already been updated");
    }
}
