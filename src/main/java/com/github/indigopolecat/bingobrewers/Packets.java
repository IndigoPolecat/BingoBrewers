package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.*;
import net.minecraft.util.BlockPos;
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
            String message = packet.getChatComponent().getUnformattedText();
            String formattedMessage = packet.getChatComponent().getFormattedText();
            if (message.startsWith("{") && message.endsWith("}")) {

                JsonObject locraw = new JsonParser().parse(message).getAsJsonObject();
                Type type = new TypeToken<HashMap<String, String>>() {
                }.getType();
                HashMap<String, String> locrawMap = new Gson().fromJson(locraw, type);

                PlayerInfo.playerGameType = locrawMap.get("gametype");
                if (PlayerInfo.playerGameType == null) return;
                if (PlayerInfo.playerGameType.equalsIgnoreCase("skyblock")) {
                    PlayerInfo.playerLocation = locrawMap.get("mode");
                    // Check if the scoreboard contains "bingo" and set the onBingo flag once we know if we're on skyblock
                    HudRendering.onBingo = ScoreBoard.isBingo();
                    HudRendering.inSkyblockorPTLobby = true;
                } else if (PlayerInfo.playerGameType.equalsIgnoreCase("prototype")) {
                    HudRendering.inSkyblockorPTLobby = true;
                } else {
                    HudRendering.inSkyblockorPTLobby = false;
                }


                PlayerInfo.currentServer = locrawMap.get("server");
                if (PlayerInfo.currentServer != null) {
                    PlayerInfo.playerHubNumber = PlayerInfo.hubServerMap.get(PlayerInfo.currentServer);

                    // This is checking without "DH" tag that dungeon hubs have, unimportant but commenting for clarity
                    if (PlayerInfo.playerHubNumber != null && ServerConnection.hubList.contains(PlayerInfo.playerHubNumber)) {
                        PlayerInfo.inSplashHub = true;
                        PlayerInfo.lastSplashHubUpdate = System.currentTimeMillis();
                    } else { // basically if the server isn't a hub, then it might be a dungeon hub so we check that
                        PlayerInfo.playerHubNumber = PlayerInfo.dungeonHubServerMap.get(PlayerInfo.currentServer);

                        // DH is a tag added to the hub number so regular hubs and dungeon hubs can be differentiated
                        if (PlayerInfo.playerHubNumber != null && ServerConnection.hubList.contains("DH" + PlayerInfo.playerHubNumber)) {
                            PlayerInfo.inSplashHub = true;
                            PlayerInfo.lastSplashHubUpdate = System.currentTimeMillis();
                        }
                    }
                }
                System.out.println("already subscribed: " + PlayerInfo.subscribedToCurrentCHServer);

                if (PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows") && !PlayerInfo.subscribedToCurrentCHServer) {
                    KryoNetwork.SubscribeToCHServer CHRequest = new KryoNetwork.SubscribeToCHServer();
                    CHRequest.server = PlayerInfo.currentServer;
                    System.out.println(PlayerInfo.day);
                    CHRequest.day = PlayerInfo.day;
                    ServerConnection.SubscribeToCHServer(CHRequest);

                }

            } else if (message.contains("You received") && PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows")) {
                CHChests.addChatMessage(formattedMessage);
            }

        }

        if (event.getPacket() instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange.BlockUpdateData[] blockUpdateData = ((S22PacketMultiBlockChange) event.getPacket()).getChangedBlocks();

            // Remove keys greater than 60 seconds in age
            hardstone.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 60000);

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
