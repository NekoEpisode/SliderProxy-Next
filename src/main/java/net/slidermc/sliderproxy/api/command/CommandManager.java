package net.slidermc.sliderproxy.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.RootCommandNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 命令管理器 - 管理所有命令的注册和执行
 */
public class CommandManager {
    private static final Logger log = LoggerFactory.getLogger(CommandManager.class);
    private static CommandManager instance;
    
    private final CommandDispatcher<CommandSource> dispatcher;
    private final Map<String, Command> commands;
    
    private CommandManager() {
        this.dispatcher = new CommandDispatcher<>();
        this.commands = new HashMap<>();
    }
    
    /**
     * 获取命令管理器实例
     * 
     * @return 命令管理器实例
     */
    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }
    
    /**
     * 注册命令
     * 
     * @param command 要注册的命令
     */
    public void registerCommand(Command command) {
        if (commands.containsKey(command.getName())) {
            log.warn("命令 {} 已经注册，将被覆盖", command.getName());
        }
        
        dispatcher.register(command.getCommandBuilder());
        commands.put(command.getName(), command);
        log.info("已注册命令: {}", command.getName());
    }
    
    /**
     * 注销命令
     * 
     * @param commandName 命令名称
     */
    public void unregisterCommand(String commandName) {
        commands.remove(commandName);
        // 注意: Brigadier不支持直接移除命令，需要重建dispatcher
        log.info("已注销命令: {}", commandName);
    }
    
    /**
     * 执行命令
     * 
     * @param source 命令源
     * @param command 命令字符串
     * @return 命令执行结果
     */
    public int execute(CommandSource source, String command) {
        try {
            return dispatcher.execute(command, source);
        } catch (CommandSyntaxException e) {
            source.sendMessage("§c命令语法错误: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            source.sendMessage("§c命令执行失败: " + e.getMessage());
            log.error("命令执行失败", e);
            return 0;
        }
    }
    
    /**
     * 异步执行命令
     * 
     * @param source 命令源
     * @param command 命令字符串
     * @return 命令执行结果的Future
     */
    public CompletableFuture<Integer> executeAsync(CommandSource source, String command) {
        return CompletableFuture.supplyAsync(() -> execute(source, command));
    }
    
    /**
     * 解析命令（不执行）
     * 
     * @param source 命令源
     * @param command 命令字符串
     * @return 解析结果
     */
    public ParseResults<CommandSource> parse(CommandSource source, String command) {
        return dispatcher.parse(command, source);
    }
    
    /**
     * 获取命令建议
     * 
     * @param source 命令源
     * @param command 命令字符串
     * @return 建议列表
     */
    public CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> getCompletionSuggestions(
            CommandSource source, String command) {
        ParseResults<CommandSource> parseResults = parse(source, command);
        return dispatcher.getCompletionSuggestions(parseResults);
    }
    
    /**
     * 获取Brigadier命令调度器
     * 
     * @return 命令调度器
     */
    public CommandDispatcher<CommandSource> getDispatcher() {
        return dispatcher;
    }
    
    /**
     * 获取根命令节点
     * 
     * @return 根命令节点
     */
    public RootCommandNode<CommandSource> getRootNode() {
        return dispatcher.getRoot();
    }
    
    /**
     * 获取已注册的命令
     * 
     * @param name 命令名称
     * @return 命令对象，如果不存在则返回null
     */
    public Command getCommand(String name) {
        return commands.get(name);
    }
    
    /**
     * 获取所有已注册的命令
     * 
     * @return 命令映射
     */
    public Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }
    
    /**
     * 检查是否存在指定名称的命令
     * 
     * @param name 命令名称
     * @return 是否存在
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }
}
