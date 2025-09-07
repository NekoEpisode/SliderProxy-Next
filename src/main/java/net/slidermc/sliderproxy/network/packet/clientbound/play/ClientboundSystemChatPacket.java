package net.slidermc.sliderproxy.network.packet.clientbound.play;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.text.Component;
import net.slidermc.sliderproxy.network.AdventureNBTHelper;
import net.slidermc.sliderproxy.network.packet.HandleResult;
import net.slidermc.sliderproxy.network.packet.IMinecraftPacket;

public class ClientboundSystemChatPacket implements IMinecraftPacket {
    private Component component;
    private boolean overlay;

    public ClientboundSystemChatPacket() {}

    public ClientboundSystemChatPacket(Component component, boolean overlay) {
        this.component = component;
        this.overlay = overlay;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        // 读取NBT数据（直到最后一个字节之前）
        int readableBytes = byteBuf.readableBytes();
        if (readableBytes > 1) {
            ByteBuf nbtBuf = byteBuf.readSlice(readableBytes - 1);
            this.component = AdventureNBTHelper.readComponent(nbtBuf);
        } else {
            this.component = Component.empty();
        }

        // 读取最后一个字节的overlay标志
        this.overlay = byteBuf.readBoolean();
    }

    @Override
    public void write(ByteBuf byteBuf) {
        AdventureNBTHelper.writeComponent(byteBuf, component);
        byteBuf.writeBoolean(this.overlay);
    }

    @Override
    public HandleResult handle(ChannelHandlerContext ctx) {
        System.out.println("Received message: " + component);
        return HandleResult.FORWARD;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }
}