package com.github.indigopolecat.bingobrewers;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.github.indigopolecat.events.PacketEvent;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import java.util.HashMap;
import java.util.concurrent.*;


public class Packets {
    // Fires event when an inventory packet is sent with a slot number greater than the slot count of the window.
    int slotCount = -1 ;
    boolean alreadyFired = false;

    // The key is the time in milliseconds the value was added plus a random 8 digit unique identifier
    HashMap<String, Long> hardstone = new HashMap<>();
    @SubscribeEvent
    public void onPacketReceived(PacketEvent.Received event) {
        if (event.getPacket() instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange.BlockUpdateData[] blockUpdateData = ((S22PacketMultiBlockChange) event.getPacket()).getChangedBlocks();

            // Remove keys greater than 60 seconds in age
            /*
            Object[] keys = hardstone.keySet().toArray();
            for (int i = 0; i < hardstone.size(); i++) {
                String key = (String) keys[i];
                // Time key was created (8 is size of random uuid)
                long time = Long.parseLong(key.substring(0, key.length() - 8));
                if (System.currentTimeMillis() - time > 60000) {
                    hardstone.remove(key);
                }
            }*/
            hardstone.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 60000);


            for (int i = 0; i < blockUpdateData.length; i++) {
                BlockPos coords = blockUpdateData[i].getPos();
                Block block = Minecraft.getMinecraft().theWorld.getBlockState(coords).getBlock();
                if (!block.toString().contains("stone")) continue;

                String key = coords.toString();
                hardstone.put(key, System.currentTimeMillis());
            }
        }
        if (event.getPacket() instanceof S23PacketBlockChange) {
            BlockPos coords = ((S23PacketBlockChange) event.getPacket()).getBlockPosition();
            Block block = Minecraft.getMinecraft().theWorld.getBlockState(coords).getBlock();

            /*
            if (block.toString().contains("chest")) {
                Object[] keys = hardstone.keySet().toArray();
                for (int i = 0; i < hardstone.size(); i++) {
                    String key = (String) keys[i];
                    System.out.println("hardstone.get(key): " + hardstone.get(key));
                    if (hardstone.get(key).equals(coords.toString()))  {
                        System.out.println("Adding to blacklist");
                        CHChests.ChestBlacklist.put(System.currentTimeMillis(), coords.toString());
                        hardstone.remove(key);
                    }

                }
            }*/
            if (!block.toString().contains("chest")) return;
            if (!hardstone.containsKey(coords.toString())) return;
            System.out.println("Adding to blacklist " + coords);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<?> future = executorService.submit(() -> {
                while (System.currentTimeMillis() - CHChests.lastMessageTime < 30);
                for (int i = 0; i < CHChests.RecentChatMessages.size(); i++) {
                    String message = CHChests.RecentChatMessages.get(i);
                    CHChests.parseChat(message);
                }
            });
            CHChests.ChestBlacklist.put(System.currentTimeMillis(), coords.toString());

                // Remove old entries
            Object[] keys = CHChests.ChestBlacklist.keySet().toArray();
            for (int i = 0; i < CHChests.ChestBlacklist.size(); i++) {
                Long key = (Long) keys[i];
                if (System.currentTimeMillis() - key > 60000) {
                    CHChests.ChestBlacklist.remove(key);
                }
            }
            try {
                future.get(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        }
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

    public static class RandomString {
        public static String randomString(int size) {
            String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "0123456789"
                    + "abcdefghijklmnopqrstuvxyz";
            StringBuilder sb = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                int index = (int) (AlphaNumericString.length() * Math.random());
                sb.append(AlphaNumericString.charAt(index));
            }
            return sb.toString();
        }
    }
}
