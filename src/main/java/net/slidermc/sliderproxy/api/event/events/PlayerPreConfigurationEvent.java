package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerPreConfigurationEvent extends PlayerEvent{
    public PlayerPreConfigurationEvent(ProxiedPlayer player) {
        super(player);
    }
}
