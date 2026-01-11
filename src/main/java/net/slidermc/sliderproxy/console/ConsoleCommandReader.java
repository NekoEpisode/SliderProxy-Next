package net.slidermc.sliderproxy.console;

import net.slidermc.sliderproxy.api.command.CommandManager;
import net.slidermc.sliderproxy.api.command.ConsoleCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 控制台命令读取器
 * 监听控制台输入并执行命令
 */
public class ConsoleCommandReader implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ConsoleCommandReader.class);
    
    private volatile boolean running = true;
    private Thread readerThread;
    
    /**
     * 启动控制台命令读取器
     */
    public void start() {
        readerThread = new Thread(this, "Console-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
        log.info("控制台命令读取器已启动");
    }
    
    /**
     * 停止控制台命令读取器
     */
    public void stop() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
        }
    }
    
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            
            // 显示初始提示符
            printPrompt();
            
            while (running) {
                try {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    
                    line = line.trim();
                    if (line.isEmpty()) {
                        printPrompt();
                        continue;
                    }
                    
                    // 处理命令
                    processCommand(line);
                    
                    // 命令执行后显示新的提示符
                    printPrompt();
                    
                } catch (IOException e) {
                    if (running) {
                        log.error("读取控制台输入时出错", e);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            log.error("初始化控制台读取器时出错", e);
        }
    }
    
    /**
     * 打印命令提示符
     */
    private void printPrompt() {
        System.out.print("> ");
        System.out.flush();
    }
    
    /**
     * 处理控制台命令
     */
    private void processCommand(String input) {
        // 移除开头的 / （如果有）
        String command = input.startsWith("/") ? input.substring(1) : input;
        
        // 执行命令
        ConsoleCommandSource source = ConsoleCommandSource.getInstance();
        
        try {
            int result = CommandManager.getInstance().execute(source, command);
            if (result == 0) {
                // 命令未找到或执行失败
                String commandName = command.split(" ")[0];
                if (!CommandManager.getInstance().hasCommand(commandName)) {
                    log.warn("未知命令: {}. 输入 'sliderproxy help' 查看可用命令", commandName);
                }
            }
        } catch (Exception e) {
            log.error("执行命令时出错: {}", command, e);
        }
    }
}
