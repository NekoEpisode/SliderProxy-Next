package net.slidermc.sliderproxy.api.command;

public interface CommandSender {
    void sendMessage(String message);
    String getName();
}
