package net.slidermc.sliderproxy.network.packet.serverbound.play;

import com.mojang.brigadier.suggestion.Suggestion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.command.CommandManager;
import net.slidermc.sliderproxy.api.command.PlayerCommandSource;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.ClientboundCommandSuggestionsPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command Suggestions Request 数据包 (0x0E)
 * 客户端请求命令自动补全建议
 */
public class ServerboundCommandSuggestionPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ServerboundCommandSuggestionPacket.class);
    
    private int transactionId;
    private String text;
    
    public ServerboundCommandSuggestionPacket() {
    }
    
    public ServerboundCommandSuggestionPacket(int transactionId, String text) {
        this.transactionId = transactionId;
        this.text = text;
    }
    
    @Override
    public void read(ByteBuf buf) {
        this.transactionId = MinecraftProtocolHelper.readVarInt(buf);
        this.text = MinecraftProtocolHelper.readString(buf);
    }
    
    @Override
    public void write(ByteBuf buf) {
        MinecraftProtocolHelper.writeVarInt(buf, transactionId);
        MinecraftProtocolHelper.writeString(buf, text);
    }
    
    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByUpstreamChannel(ctx.channel());
        if (player == null) {
            return HandleResult.FORWARD;
        }
        
        // 检查是否是代理命令的补全请求
        String commandText = text.startsWith("/") ? text.substring(1) : text;
        String commandName = commandText.split(" ")[0];
        
        if (CommandManager.getInstance().hasCommand(commandName)) {
            // 是代理命令，由代理处理补全
            PlayerCommandSource source = new PlayerCommandSource(player);
            
            CommandManager.getInstance().getCompletionSuggestions(source, commandText)
                .thenAccept(suggestions -> {
                    // 计算正确的替换范围
                    // Brigadier 返回的范围是相对于命令字符串的
                    // 客户端期望的是相对于整个输入的（包括 /）
                    int offset = text.startsWith("/") ? 1 : 0;
                    int start = suggestions.getRange().getStart() + offset;
                    int length = suggestions.getRange().getLength();
                    
                    // 发送补全建议给客户端
                    ClientboundCommandSuggestionsPacket response = new ClientboundCommandSuggestionsPacket(
                        transactionId,
                        start,
                        length
                    );
                    
                    for (Suggestion suggestion : suggestions.getList()) {
                        response.addSuggestion(suggestion.getText());
                    }
                    
                    player.sendPacket(response);
                    log.debug("已发送代理命令补全建议给玩家 {}: {} 个建议, start={}, length={}", 
                        player.getName(), suggestions.getList().size(), start, length);
                });
            
            return HandleResult.UNFORWARD;
        }
        
        // 不是代理命令，转发给后端服务器
        return HandleResult.FORWARD;
    }
    
    public int getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}
