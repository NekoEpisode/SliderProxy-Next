package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerConfigurationCompleteEvent extends PlayerEvent{
    public PlayerConfigurationCompleteEvent(ProxiedPlayer player) {
        super(player);
    }
}
