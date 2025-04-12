package com.github.indigopolecat.events;

import com.github.indigopolecat.bingobrewers.*;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import ibxm.Player;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.*;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.github.indigopolecat.events.PacketEvent;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Packets {

    // Fires event when an inventory packet is sent with a slot number greater than the slot count of the window.
    int slotCount = -1;
    boolean alreadyFired = false;

    // The key is the time in milliseconds the value was added plus a random 8 digit unique identifier
    public static HashMap<String, Long> hardstone = new HashMap<>();

    @SubscribeEvent
    public void onPacketReceived(PacketEvent.Received event) {
        if (event.getPacket() instanceof S38PacketPlayerListItem) {
            if (System.currentTimeMillis() - PlayerInfo.lastSplashHubUpdate > 120000) {
                PlayerInfo.inSplashHub = false;
            }
            if (!PlayerInfo.inSplashHub) return;
            S38PacketPlayerListItem packet = (S38PacketPlayerListItem) event.getPacket();
            if (packet.getAction() != S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME) return;
            for (S38PacketPlayerListItem.AddPlayerData data : packet.getEntries()) {
                if (!data.getDisplayName().getUnformattedText().contains("Players")) return;
                Pattern playerCount = Pattern.compile("Players \\(([0-9]+)\\)");
                Matcher playerCountMatcher = playerCount.matcher(data.getDisplayName().getUnformattedText());
                if (playerCountMatcher.find()) {
                    PlayerInfo playerInfo = new PlayerInfo();
                    playerInfo.setPlayerCount(Integer.parseInt(playerCountMatcher.group(1)));
                }


            }
        }

        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            if (packet.getType() == 2) return;
            String message = packet.getChatComponent().getUnformattedText();
            String formattedMessage = packet.getChatComponent().getFormattedText();

            if ((CHChests.addMessages || formattedMessage.matches(CHChests.signalLootChatMessage)) && PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows")) {
                if (BingoBrewersConfig.crystalHollowsWaypointsToggle) {
                    CHChests.addChatMessage(formattedMessage);
                }
                if (formattedMessage.matches(CHChests.signalLootChatMessageEnd)) {
                    CHChests.parseChat();
                    CHChests.addMessages = false;
                    CHChests.expectingHardstoneLoot = false;
                    return;
                }
            }

        }

        if (event.getPacket() instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange.BlockUpdateData[] blockUpdateData = ((S22PacketMultiBlockChange) event.getPacket()).getChangedBlocks();

            // Remove keys greater than 60 seconds in age
            hardstone.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 60000);
            if (!BingoBrewersConfig.crystalHollowsWaypointsToggle) return;
            for (int i = 0; i < blockUpdateData.length; i++) {
                BlockPos coords = blockUpdateData[i].getPos();
                // get old block
                Block block = Minecraft.getMinecraft().theWorld.getBlockState(coords).getBlock();
                // ignore if the old block is air or water because we are looking for stone blocks (or anything else)
                if (block.toString().contains("air") || block.toString().contains("water") || block.toString().contains("chest") || block.toString().equals(blockUpdateData[i].getBlockState().getBlock().toString())) continue;
                String key = coords.toString();
                hardstone.put(key, System.currentTimeMillis());
            }
        }

        if (event.getPacket() instanceof S23PacketBlockChange) {
            if (!BingoBrewersConfig.crystalHollowsWaypointsToggle) return;
            if (Minecraft.getMinecraft().theWorld == null) {
                System.out.println("Error: World is null!");
                return;
            }

            // coordinates of the block that changed
            BlockPos coords = ((S23PacketBlockChange) event.getPacket()).getBlockPosition();
            // old block
            Block block = Minecraft.getMinecraft().theWorld.getBlockState(coords).getBlock();
            // new block
            String newBlockStr = ((S23PacketBlockChange) event.getPacket()).getBlockState().getBlock().toString();

            if (block.toString().contains("air") || block.toString().contains("water") || block.toString().contains("chest") || block.toString().contains(newBlockStr)) return;
            String key = coords.toString();
            hardstone.put(key, System.currentTimeMillis());


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

}
