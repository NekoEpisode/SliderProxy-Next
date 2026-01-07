package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.event.Event;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

public class PlayerEvent extends Event {
    private final ProxiedPlayer player;

    public PlayerEvent(ProxiedPlayer player) {
        this.player = player;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }
}
