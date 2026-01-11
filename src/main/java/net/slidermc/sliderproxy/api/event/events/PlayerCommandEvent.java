package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家命令事件
 * 在玩家发送命令时触发
 */
public class PlayerCommandEvent extends PlayerEvent {
    private String command;
    private final String originalCommand;

    public PlayerCommandEvent(ProxiedPlayer player, String command) {
        super(player);
        this.command = command;
        this.originalCommand = command;
    }

    /**
     * 获取命令内容（不包含斜杠）
     * @return 命令内容
     */
    public String getCommand() {
        return command;
    }

    /**
     * 设置命令内容
     * @param command 新的命令内容
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * 获取原始命令内容（未被修改的）
     * @return 原始命令内容
     */
    public String getOriginalCommand() {
        return originalCommand;
    }

    /**
     * 检查命令是否被修改过
     * @return 如果命令被修改过则返回 true
     */
    public boolean isCommandModified() {
        return !command.equals(originalCommand);
    }
}
