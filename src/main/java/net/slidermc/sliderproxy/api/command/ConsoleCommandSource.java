package net.slidermc.sliderproxy.api.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制台命令源实现
 */
public class ConsoleCommandSource implements CommandSource {
    private static final Logger log = LoggerFactory.getLogger(ConsoleCommandSource.class);
    private static final ConsoleCommandSource INSTANCE = new ConsoleCommandSource();
    
    private ConsoleCommandSource() {
    }
    
    public static ConsoleCommandSource getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void sendMessage(String message) {
        log.info(message);
    }
    
    @Override
    public void sendMessage(Component component) {
        String text = PlainTextComponentSerializer.plainText().serialize(component);
        log.info(text);
    }
    
    @Override
    public String getName() {
        return "CONSOLE";
    }
    
    @Override
    public int getPermissionLevel() {
        return 4; // 控制台拥有最高权限
    }
    
    @Override
    @Nullable
    public ProxiedPlayer asPlayer() {
        return null;
    }
}
