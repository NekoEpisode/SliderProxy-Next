package net.slidermc.sliderproxy.network.packet.clientbound.play.commands;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.ByteBuf;
import net.slidermc.sliderproxy.api.command.CommandSource;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 命令节点数据
 * 表示命令树中的一个节点
 */
public class CommandNodeData {
    // 节点类型标志
    public static final byte NODE_TYPE_ROOT = 0;
    public static final byte NODE_TYPE_LITERAL = 1;
    public static final byte NODE_TYPE_ARGUMENT = 2;
    
    // 其他标志
    public static final byte FLAG_EXECUTABLE = 0x04;
    public static final byte FLAG_HAS_REDIRECT = 0x08;
    public static final byte FLAG_HAS_SUGGESTIONS = 0x10;
    public static final byte FLAG_IS_RESTRICTED = 0x20;
    
    private byte flags;
    private List<Integer> children;
    private Integer redirectNode;
    private String name;
    private ArgumentTypeData argumentType;
    private String suggestionsType;
    
    public CommandNodeData() {
        this.children = new ArrayList<>();
    }
    
    /**
     * 从Brigadier命令节点创建CommandNodeData
     */
    public static CommandNodeData fromBrigadierNode(
            CommandNode<CommandSource> node,
            Map<CommandNode<CommandSource>, Integer> nodeIndices) {
        
        CommandNodeData data = new CommandNodeData();
        
        // 设置节点类型
        byte flags = 0;
        if (node instanceof RootCommandNode) {
            flags |= NODE_TYPE_ROOT;
        } else if (node instanceof LiteralCommandNode) {
            flags |= NODE_TYPE_LITERAL;
        } else if (node instanceof ArgumentCommandNode) {
            flags |= NODE_TYPE_ARGUMENT;
        }
        
        // 设置可执行标志
        if (node.getCommand() != null) {
            flags |= FLAG_EXECUTABLE;
        }
        
        // 设置重定向标志
        if (node.getRedirect() != null) {
            flags |= FLAG_HAS_REDIRECT;
        }
        
        // 设置受限标志（需要权限）
        if (node.getRequirement() != null) {
            flags |= FLAG_IS_RESTRICTED;
        }
        
        data.flags = flags;
        
        // 添加子节点索引
        for (CommandNode<CommandSource> child : node.getChildren()) {
            Integer childIndex = nodeIndices.get(child);
            if (childIndex != null) {
                data.children.add(childIndex);
            }
        }
        
        // 设置重定向节点
        if (node.getRedirect() != null) {
            data.redirectNode = nodeIndices.get(node.getRedirect());
        }
        
        // 设置节点名称
        if (node instanceof LiteralCommandNode) {
            data.name = ((LiteralCommandNode<?>) node).getLiteral();
        } else if (node instanceof ArgumentCommandNode) {
            data.name = ((ArgumentCommandNode<?, ?>) node).getName();
            
            // 设置参数类型
            ArgumentCommandNode<?, ?> argNode = (ArgumentCommandNode<?, ?>) node;
            data.argumentType = ArgumentTypeData.fromBrigadierType(argNode.getType());
            
            // 设置建议类型
            if (argNode.getCustomSuggestions() != null) {
                flags |= FLAG_HAS_SUGGESTIONS;
                data.flags = flags;
                data.suggestionsType = "minecraft:ask_server";
            }
        }
        
        return data;
    }
    
    public void read(ByteBuf buf) {
        flags = buf.readByte();
        
        // 读取子节点
        int childCount = MinecraftProtocolHelper.readVarInt(buf);
        if (childCount < 0) {
            throw new IllegalArgumentException("Invalid child count: " + childCount + ", flags: " + flags);
        }
        children = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            children.add(MinecraftProtocolHelper.readVarInt(buf));
        }
        
        // 读取重定向节点
        if ((flags & FLAG_HAS_REDIRECT) != 0) {
            redirectNode = MinecraftProtocolHelper.readVarInt(buf);
        }
        
        // 读取节点名称
        byte nodeType = (byte) (flags & 0x03);
        if (nodeType == NODE_TYPE_LITERAL || nodeType == NODE_TYPE_ARGUMENT) {
            name = MinecraftProtocolHelper.readString(buf);
        }
        
        // 读取参数类型
        if (nodeType == NODE_TYPE_ARGUMENT) {
            argumentType = new ArgumentTypeData();
            argumentType.read(buf);
        }
        
        // 读取建议类型
        if ((flags & FLAG_HAS_SUGGESTIONS) != 0) {
            suggestionsType = MinecraftProtocolHelper.readString(buf);
        }
    }
    
    public void write(ByteBuf buf) {
        buf.writeByte(flags);
        
        // 写入子节点
        MinecraftProtocolHelper.writeVarInt(buf, children.size());
        for (Integer child : children) {
            MinecraftProtocolHelper.writeVarInt(buf, child);
        }
        
        // 写入重定向节点
        if ((flags & FLAG_HAS_REDIRECT) != 0 && redirectNode != null) {
            MinecraftProtocolHelper.writeVarInt(buf, redirectNode);
        }
        
        // 写入节点名称
        byte nodeType = (byte) (flags & 0x03);
        if (nodeType == NODE_TYPE_LITERAL || nodeType == NODE_TYPE_ARGUMENT) {
            MinecraftProtocolHelper.writeString(buf, name != null ? name : "");
        }
        
        // 写入参数类型
        if (nodeType == NODE_TYPE_ARGUMENT && argumentType != null) {
            argumentType.write(buf);
        }
        
        // 写入建议类型
        if ((flags & FLAG_HAS_SUGGESTIONS) != 0 && suggestionsType != null) {
            MinecraftProtocolHelper.writeString(buf, suggestionsType);
        }
    }
    
    // Getters and Setters
    
    public byte getFlags() {
        return flags;
    }
    
    public void setFlags(byte flags) {
        this.flags = flags;
    }
    
    public List<Integer> getChildren() {
        return children;
    }
    
    public void setChildren(List<Integer> children) {
        this.children = children;
    }
    
    @Nullable
    public Integer getRedirectNode() {
        return redirectNode;
    }
    
    public void setRedirectNode(Integer redirectNode) {
        this.redirectNode = redirectNode;
    }
    
    @Nullable
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Nullable
    public ArgumentTypeData getArgumentType() {
        return argumentType;
    }
    
    public void setArgumentType(ArgumentTypeData argumentType) {
        this.argumentType = argumentType;
    }
    
    @Nullable
    public String getSuggestionsType() {
        return suggestionsType;
    }
    
    public void setSuggestionsType(String suggestionsType) {
        this.suggestionsType = suggestionsType;
    }
    
    public byte getNodeType() {
        return (byte) (flags & 0x03);
    }
    
    public boolean isExecutable() {
        return (flags & FLAG_EXECUTABLE) != 0;
    }
    
    public boolean hasRedirect() {
        return (flags & FLAG_HAS_REDIRECT) != 0;
    }
    
    public boolean hasSuggestions() {
        return (flags & FLAG_HAS_SUGGESTIONS) != 0;
    }
    
    public boolean isRestricted() {
        return (flags & FLAG_IS_RESTRICTED) != 0;
    }
}
