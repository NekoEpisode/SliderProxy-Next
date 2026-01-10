package net.slidermc.sliderproxy.network.packet.clientbound.play;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.slidermc.sliderproxy.api.command.CommandManager;
import net.slidermc.sliderproxy.api.command.CommandSource;
import net.slidermc.sliderproxy.api.player.PlayerManager;
import net.slidermc.sliderproxy.api.player.ProxiedPlayer;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;
import net.slidermc.sliderproxy.network.packet.clientbound.play.commands.ArgumentTypeData;
import net.slidermc.sliderproxy.network.packet.clientbound.play.commands.CommandNodeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Commands 数据包 (0x10)
 * 发送服务器的命令树结构给客户端
 */
public class ClientboundCommandsPacket implements IMinecraftPacket {
    private static final Logger log = LoggerFactory.getLogger(ClientboundCommandsPacket.class);
    
    private List<CommandNodeData> nodes;
    private int rootIndex;
    
    public ClientboundCommandsPacket() {
        this.nodes = new ArrayList<>();
        this.rootIndex = 0;
    }
    
    public ClientboundCommandsPacket(RootCommandNode<CommandSource> rootNode) {
        this.nodes = new ArrayList<>();
        this.rootIndex = 0;
        buildNodeList(rootNode);
    }
    
    /**
     * 从根节点构建节点列表
     */
    private void buildNodeList(RootCommandNode<CommandSource> rootNode) {
        Map<CommandNode<CommandSource>, Integer> nodeIndices = new HashMap<>();
        List<CommandNode<CommandSource>> nodeList = new ArrayList<>();
        
        // 使用广度优先搜索遍历命令树
        Queue<CommandNode<CommandSource>> queue = new LinkedList<>();
        queue.add(rootNode);
        nodeIndices.put(rootNode, 0);
        nodeList.add(rootNode);
        
        while (!queue.isEmpty()) {
            CommandNode<CommandSource> node = queue.poll();
            
            for (CommandNode<CommandSource> child : node.getChildren()) {
                if (!nodeIndices.containsKey(child)) {
                    nodeIndices.put(child, nodeList.size());
                    nodeList.add(child);
                    queue.add(child);
                }
            }
        }
        
        // 转换为CommandNodeData
        for (CommandNode<CommandSource> node : nodeList) {
            CommandNodeData nodeData = CommandNodeData.fromBrigadierNode(node, nodeIndices);
            nodes.add(nodeData);
        }
        
        this.rootIndex = 0; // 根节点总是第一个
    }
    
    @Override
    public void read(ByteBuf buf) {
        int nodeCount = MinecraftProtocolHelper.readVarInt(buf);
        nodes = new ArrayList<>(nodeCount);
        
        for (int i = 0; i < nodeCount; i++) {
            CommandNodeData nodeData = new CommandNodeData();
            nodeData.read(buf);
            nodes.add(nodeData);
        }
        
        rootIndex = MinecraftProtocolHelper.readVarInt(buf);
    }
    
    @Override
    public void write(ByteBuf buf) {
        MinecraftProtocolHelper.writeVarInt(buf, nodes.size());
        
        for (CommandNodeData node : nodes) {
            node.write(buf);
        }
        
        MinecraftProtocolHelper.writeVarInt(buf, rootIndex);
    }
    
    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        // 拦截后端的命令包，合并代理命令后再发送给客户端
        ProxiedPlayer player = PlayerManager.getInstance().getPlayerByDownstreamChannel(ctx.channel());
        if (player == null) {
            return HandleResult.FORWARD;
        }
        
        try {
            // 获取代理的命令树
            RootCommandNode<CommandSource> proxyRoot = CommandManager.getInstance().getRootNode();
            
            // 合并后端命令树和代理命令树
            mergeCommandTrees(proxyRoot);
            
            // 发送合并后的命令树给客户端
            player.sendPacket(this);
            
            log.debug("已向玩家 {} 发送合并后的命令树（共 {} 个节点）", player.getName(), nodes.size());
            
            // 不转发原始包
            return HandleResult.UNFORWARD;
        } catch (Exception e) {
            log.error("合并命令树时出错", e);
            // 出错时转发原始包
            return HandleResult.FORWARD;
        }
    }
    
    /**
     * 合并代理命令树到现有的命令树中
     * 代理命令优先级高于后端命令（会覆盖同名命令）
     */
    private void mergeCommandTrees(RootCommandNode<CommandSource> proxyRoot) {
        if (nodes.isEmpty() || rootIndex < 0 || rootIndex >= nodes.size()) {
            log.warn("后端命令树为空或根节点索引无效，只使用代理命令");
            buildNodeList(proxyRoot);
            return;
        }
        
        // 获取后端的根节点
        CommandNodeData backendRoot = nodes.get(rootIndex);
        
        // 将代理命令添加到后端根节点的子节点中
        for (CommandNode<CommandSource> proxyChild : proxyRoot.getChildren()) {
            String commandName = proxyChild.getName();
            
            // 检查是否已存在同名命令，如果存在则移除（代理命令覆盖后端命令）
            Integer existingIndex = null;
            for (Integer childIndex : backendRoot.getChildren()) {
                if (childIndex < nodes.size()) {
                    CommandNodeData childNode = nodes.get(childIndex);
                    if (commandName.equals(childNode.getName())) {
                        existingIndex = childIndex;
                        break;
                    }
                }
            }
            
            // 移除后端的同名命令
            if (existingIndex != null) {
                backendRoot.getChildren().remove(existingIndex);
                log.debug("代理命令 '{}' 覆盖后端同名命令", commandName);
            }
            
            // 递归添加代理命令及其所有子节点
            int newNodeIndex = addNodeRecursively(proxyChild);
            backendRoot.getChildren().add(newNodeIndex);
            log.debug("已添加代理命令: {}", commandName);
        }
    }
    
    /**
     * 递归添加命令节点及其所有子节点
     * @return 添加的节点在 nodes 列表中的索引
     */
    private int addNodeRecursively(CommandNode<CommandSource> node) {
        // 先为当前节点分配索引
        int currentIndex = nodes.size();
        
        // 创建节点数据（先添加占位）
        CommandNodeData nodeData = new CommandNodeData();
        nodes.add(nodeData);
        
        // 构建子节点索引映射
        Map<CommandNode<CommandSource>, Integer> childIndices = new HashMap<>();
        
        // 递归添加所有子节点
        for (CommandNode<CommandSource> child : node.getChildren()) {
            int childIndex = addNodeRecursively(child);
            childIndices.put(child, childIndex);
        }
        
        // 现在填充节点数据
        fillNodeData(nodeData, node, childIndices);
        
        return currentIndex;
    }
    
    /**
     * 填充节点数据
     */
    private void fillNodeData(CommandNodeData data, CommandNode<CommandSource> node, 
                              Map<CommandNode<CommandSource>, Integer> childIndices) {
        // 设置节点类型
        byte flags = 0;
        if (node instanceof RootCommandNode) {
            flags |= CommandNodeData.NODE_TYPE_ROOT;
        } else if (node instanceof LiteralCommandNode) {
            flags |= CommandNodeData.NODE_TYPE_LITERAL;
        } else if (node instanceof ArgumentCommandNode) {
            flags |= CommandNodeData.NODE_TYPE_ARGUMENT;
        }
        
        // 设置可执行标志
        if (node.getCommand() != null) {
            flags |= CommandNodeData.FLAG_EXECUTABLE;
        }
        
        // 设置重定向标志
        if (node.getRedirect() != null) {
            flags |= CommandNodeData.FLAG_HAS_REDIRECT;
        }
        
        data.setFlags(flags);
        
        // 添加子节点索引
        List<Integer> children = new ArrayList<>();
        for (CommandNode<CommandSource> child : node.getChildren()) {
            Integer childIndex = childIndices.get(child);
            if (childIndex != null) {
                children.add(childIndex);
            }
        }
        data.setChildren(children);
        
        // 设置节点名称
        if (node instanceof LiteralCommandNode) {
            data.setName(((LiteralCommandNode<?>) node).getLiteral());
        } else if (node instanceof ArgumentCommandNode) {
            ArgumentCommandNode<?, ?> argNode = (ArgumentCommandNode<?, ?>) node;
            data.setName(argNode.getName());
            
            // 设置参数类型
            data.setArgumentType(ArgumentTypeData.fromBrigadierType(argNode.getType()));
            
            // 设置建议类型
            if (argNode.getCustomSuggestions() != null) {
                flags |= CommandNodeData.FLAG_HAS_SUGGESTIONS;
                data.setFlags(flags);
                data.setSuggestionsType("minecraft:ask_server");
            }
        }
    }
    
    public List<CommandNodeData> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<CommandNodeData> nodes) {
        this.nodes = nodes;
    }
    
    public int getRootIndex() {
        return rootIndex;
    }
    
    public void setRootIndex(int rootIndex) {
        this.rootIndex = rootIndex;
    }
}
