package com.github.indigopolecat.events;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class PacketHandler extends ChannelDuplexHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Packet<?>) {
            @SuppressWarnings("unchecked") // This will always be a Packet<INetHandlerPlayClient>
            Packet<INetHandlerPlayClient> packet = (Packet<INetHandlerPlayClient>) msg;
            PacketEvent.Received event = new PacketEvent.Received(packet);
            MinecraftForge.EVENT_BUS.post(event);

            if (event.isCanceled()) {
                return;
            }
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Packet<?>) {
            @SuppressWarnings("unchecked") // This will always be a Packet<INetHandlerPlayServer>
            Packet<INetHandlerPlayServer> packet = (Packet<INetHandlerPlayServer>) msg;
            PacketEvent.Sent event = new PacketEvent.Sent(packet);
            MinecraftForge.EVENT_BUS.post(event);

            if (event.isCanceled()) {
                return;
            }
        }

        super.write(ctx, msg, promise);
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        event.manager.channel().pipeline().addBefore("packet_handler", "examplemod_packet_handler", this);
    }
}