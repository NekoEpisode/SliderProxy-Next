package net.slidermc.sliderproxy.network.packet.serverbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.command.CommandManager;
import net.slidermc.sliderproxy.api.command.PlayerCommandSource;
import net.slidermc.sliderproxy.api.event.EventRegistry;
import net.slidermc.sliderproxy.api.event.events.PlayerCommandEvent;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerboundChatCommandPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundChatCommandPacket.class);
    private String command;

    public ServerboundChatCommandPacket() {}

    public ServerboundChatCommandPacket(String command) {
        this.command = command;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        String command = MinecraftProtocolHelper.readString(byteBuf);
        if (command.length() > 32767) throw new IllegalArgumentException("命令长度过长");
        this.command = command;
    }

    @Override
    public void write(ByteBuf byteBuf) {
        MinecraftProtocolHelper.writeString(byteBuf, command);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player == null) return HandleResult.FORWARD;
        
        // 触发命令事件
        PlayerCommandEvent commandEvent = new PlayerCommandEvent(player, command);
        EventRegistry.callEvent(commandEvent);
        
        // 如果事件被取消，不执行命令也不转发
        if (commandEvent.isCancelled()) {
            log.debug("命令被取消: /{}", command);
            return HandleResult.UNFORWARD;
        }
        
        // 如果命令被修改，更新命令内容
        if (commandEvent.isCommandModified()) {
            this.command = commandEvent.getCommand();
            log.debug("命令被修改为: /{}", command);
        }
        
        // 检查是否是代理注册的命令
        String commandName = command.split(" ")[0];
        if (!CommandManager.getInstance().hasCommand(commandName)) {
            // 不是代理命令，转发给后端服务器
            return HandleResult.FORWARD;
        }
        
        // 是代理命令，执行它
        PlayerCommandSource source = new PlayerCommandSource(player);
        
        try {
            int result = CommandManager.getInstance().execute(source, command);
            log.debug("玩家 {} 执行代理命令: /{} (结果: {})", player.getName(), command, result);
        } catch (Exception e) {
            log.error("执行命令时出错: /{}", command, e);
        }
        
        // 代理命令不转发
        return HandleResult.UNFORWARD;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
