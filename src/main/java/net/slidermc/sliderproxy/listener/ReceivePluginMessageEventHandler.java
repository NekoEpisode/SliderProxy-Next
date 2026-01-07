package net.slidermc.sliderproxy.listener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.key.Key;
import net.slidermc.sliderproxy.api.event.EventListener;
import net.slidermc.sliderproxy.api.event.EventPriority;
import net.slidermc.sliderproxy.api.event.events.ReceivePluginMessageEvent;
import net.slidermc.sliderproxy.network.MinecraftProtocolHelper;
import net.slidermc.sliderproxy.utils.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReceivePluginMessageEventHandler {
    private static final Logger log = LoggerFactory.getLogger(ReceivePluginMessageEventHandler.class);

    @EventListener(priority = EventPriority.LOWEST)
    public void onPluginMessage(ReceivePluginMessageEvent event) {
        Key identifier = event.getIdentifier();
        log.debug("收到PluginMessage: {}", identifier);
        if (identifier.equals(KeyUtils.MINECRAFT_BRAND) && event.getFrom() == ReceivePluginMessageEvent.From.DOWNSTREAM && !event.isCancelled()) {
            log.debug("PluginMessage/Brand包来自下游服务器，正在修改");
            ByteBuf buf = Unpooled.buffer();
            try {
                buf.writeBytes(event.getData());
                String brand = MinecraftProtocolHelper.readString(buf);
                brand = "SliderProxy -> " + brand;

                ByteBuf newBuf = Unpooled.buffer();
                MinecraftProtocolHelper.writeString(newBuf, brand);

                byte[] newData = new byte[newBuf.readableBytes()];
                newBuf.readBytes(newData);
                event.setData(newData);

                newBuf.release();
                log.debug("已修改brand为: {}", brand);
            } catch (Exception e) {
                log.error("处理Brand PluginMessage失败", e);
            } finally {
                buf.release();
            }
        }
    }
}
