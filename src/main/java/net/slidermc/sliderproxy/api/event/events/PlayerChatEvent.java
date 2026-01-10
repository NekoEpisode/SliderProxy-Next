package net.slidermc.sliderproxy.api.event.events;

import net.slidermc.sliderproxy.api.player.ProxiedPlayer;

/**
 * 玩家聊天事件
 * 
 * 当玩家发送聊天消息时触发此事件。
 * 
 * 注意：
 * - 可以通过 setCancelled(true) 取消消息发送
 * - 可以通过 setMessage() 修改消息内容
 * - 修改消息内容会导致聊天签名失效，在启用聊天签名验证的服务器上可能导致问题
 * 
 * 使用示例：
 * <pre>
 * EventRegistry.registerListener(new Object() {
 *     {@literal @}EventListener
 *     public void onChat(PlayerChatEvent event) {
 *         // 获取玩家和消息
 *         ProxiedPlayer player = event.getPlayer();
 *         String message = event.getMessage();
 *         
 *         // 取消包含敏感词的消息
 *         if (message.contains("badword")) {
 *             event.setCancelled(true);
 *             player.sendMessage("§c你的消息包含敏感词！");
 *         }
 *         
 *         // 或者修改消息（会导致签名失效）
 *         // event.setMessage(message.replace("badword", "***"));
 *     }
 * });
 * </pre>
 */
public class PlayerChatEvent extends PlayerEvent {
    
    private String message;
    private final String originalMessage;

    public PlayerChatEvent(ProxiedPlayer player, String message) {
        super(player);
        this.message = message;
        this.originalMessage = message;
    }

    /**
     * 获取聊天消息内容
     * @return 消息内容（可能已被修改）
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置聊天消息内容
     * 
     * 注意：修改消息内容会导致聊天签名失效，
     * 在启用 enforce-secure-profile 的服务器上可能导致玩家被踢出。
     * 
     * @param message 新的消息内容
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取原始消息内容（未被修改的）
     * @return 原始消息内容
     */
    public String getOriginalMessage() {
        return originalMessage;
    }

    /**
     * 检查消息是否被修改过
     * @return 如果消息被修改过则返回 true
     */
    public boolean isMessageModified() {
        return !message.equals(originalMessage);
    }
}
