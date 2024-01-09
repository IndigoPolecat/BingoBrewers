package com.github.indigopolecat.events;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public final class PacketListener {
    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // Create a new instance of PacketHandler every single time because netty does not allow duplicates
        event.manager.channel().pipeline().addBefore("packet_handler", "examplemod_packet_handler", new PacketHandler());
    }
}