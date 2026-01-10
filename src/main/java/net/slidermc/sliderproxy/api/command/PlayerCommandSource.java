package net.slidermc.sliderproxy.api.command;

import net.kyori.adventure.text.Component;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家命令源实现
 */
public class PlayerCommandSource implements CommandSource {
    private final ProxiedPlayer player;
    
    public PlayerCommandSource(ProxiedPlayer player) {
        this.player = player;
    }
    
    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }
    
    @Override
    public void sendMessage(Component component) {
        player.sendMessage(component);
    }
    
    @Override
    public String getName() {
        return player.getName();
    }
    
    @Override
    public int getPermissionLevel() {
        // TODO: 实现权限系统
        return 0;
    }
    
    @Override
    @Nullable
    public ProxiedPlayer asPlayer() {
        return player;
    }
    
    public ProxiedPlayer getPlayer() {
        return player;
    }
}
