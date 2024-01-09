package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.github.indigopolecat.events.PacketEvent;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;


public class doneLoading {
    // Fires event when an inventory packet is sent with a slot number greater than the slot count of the window.
    int slotCount = -1 ;
    boolean alreadyFired = false;
    @SubscribeEvent
    public void onPacketReceived(PacketEvent.Received event) {
        if (event.getPacket() instanceof S2DPacketOpenWindow) {
            S2DPacketOpenWindow packet = (S2DPacketOpenWindow) event.getPacket();
            slotCount = packet.getSlotCount();
            alreadyFired = false;

        } else if (event.getPacket() instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.getPacket();
            // slot # in inventory of the packet
            int slot = packet.func_149173_d();
            if (slot > slotCount && !alreadyFired) {
                alreadyFired = true;
                new Thread(() -> {
                    try {
                        // wait 100ms to make sure the inventory is loaded
                        Thread.sleep(100);
                        MinecraftForge.EVENT_BUS.post(new InventoryLoadingDoneEvent());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                }

            }
        }


    public static class InventoryLoadingDoneEvent extends Event {
    }
}
