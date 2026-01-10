package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Suggestions Response 数据包 (0x0F)
 * 服务器响应命令自动补全建议
 */
public class ClientboundCommandSuggestionsPacket implements IMinecraftPacket {
    private int transactionId;
    private int start;
    private int length;
    private List<Suggestion> suggestions;
    
    public ClientboundCommandSuggestionsPacket() {
        this.suggestions = new ArrayList<>();
    }
    
    public ClientboundCommandSuggestionsPacket(int transactionId, int start, int length) {
        this.transactionId = transactionId;
        this.start = start;
        this.length = length;
        this.suggestions = new ArrayList<>();
    }
    
    @Override
    public void read(ByteBuf buf) {
        this.transactionId = MinecraftProtocolHelper.readVarInt(buf);
        this.start = MinecraftProtocolHelper.readVarInt(buf);
        this.length = MinecraftProtocolHelper.readVarInt(buf);
        
        int count = MinecraftProtocolHelper.readVarInt(buf);
        this.suggestions = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            String match = MinecraftProtocolHelper.readString(buf);
            Component tooltip = null;
            
            if (buf.readBoolean()) {
                String tooltipJson = MinecraftProtocolHelper.readString(buf);
                tooltip = GsonComponentSerializer.gson().deserialize(tooltipJson);
            }
            
            suggestions.add(new Suggestion(match, tooltip));
        }
    }
    
    @Override
    public void write(ByteBuf buf) {
        MinecraftProtocolHelper.writeVarInt(buf, transactionId);
        MinecraftProtocolHelper.writeVarInt(buf, start);
        MinecraftProtocolHelper.writeVarInt(buf, length);
        
        MinecraftProtocolHelper.writeVarInt(buf, suggestions.size());
        
        for (Suggestion suggestion : suggestions) {
            MinecraftProtocolHelper.writeString(buf, suggestion.getMatch());
            
            if (suggestion.getTooltip() != null) {
                buf.writeBoolean(true);
                String tooltipJson = GsonComponentSerializer.gson().serialize(suggestion.getTooltip());
                MinecraftProtocolHelper.writeString(buf, tooltipJson);
            } else {
                buf.writeBoolean(false);
            }
        }
    }
    
    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        return HandleResult.FORWARD;
    }
    
    public void addSuggestion(String match) {
        suggestions.add(new Suggestion(match, null));
    }
    
    public void addSuggestion(String match, Component tooltip) {
        suggestions.add(new Suggestion(match, tooltip));
    }
    
    // Getters and Setters
    
    public int getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
    
    public int getStart() {
        return start;
    }
    
    public void setStart(int start) {
        this.start = start;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public List<Suggestion> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }
    
    /**
     * 命令建议项
     */
    public static class Suggestion {
        private final String match;
        private final Component tooltip;
        
        public Suggestion(String match, @Nullable Component tooltip) {
            this.match = match;
            this.tooltip = tooltip;
        }
        
        public String getMatch() {
            return match;
        }
        
        @Nullable
        public Component getTooltip() {
            return tooltip;
        }
    }
}
