package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家位置更新事件
 * 在玩家位置发生变化时触发
 */
public class PlayerPositionEvent extends PlayerEvent {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public PlayerPositionEvent(ProxiedPlayer player, double x, double y, double z, 
                              float yaw, float pitch, boolean onGround) {
        super(player);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    /**
     * 获取 X 坐标
     * @return X 坐标
     */
    public double getX() {
        return x;
    }

    /**
     * 设置 X 坐标
     * @param x X 坐标
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * 获取 Y 坐标
     * @return Y 坐标
     */
    public double getY() {
        return y;
    }

    /**
     * 设置 Y 坐标
     * @param y Y 坐标
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * 获取 Z 坐标
     * @return Z 坐标
     */
    public double getZ() {
        return z;
    }

    /**
     * 设置 Z 坐标
     * @param z Z 坐标
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * 获取偏航角（Yaw）
     * @return 偏航角
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * 设置偏航角（Yaw）
     * @param yaw 偏航角
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * 获取俯仰角（Pitch）
     * @return 俯仰角
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * 设置俯仰角（Pitch）
     * @param pitch 俯仰角
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * 检查玩家是否在地面上
     * @return 如果在地面上则返回 true
     */
    public boolean isOnGround() {
        return onGround;
    }

    /**
     * 设置玩家是否在地面上
     * @param onGround 是否在地面上
     */
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
}
