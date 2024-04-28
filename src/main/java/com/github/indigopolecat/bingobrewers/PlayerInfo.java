package com.github.indigopolecat.bingobrewers;


import com.esotericsoftware.kryonet.Server;
import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.HashMap;

public class PlayerInfo {
    public static String playerLocation = "";
    public static String playerGameType = "";
    public static String playerHubNumber = null;
    public static long lastWorldLoad = -1;
    public static long lastPositionUpdate = -1;
    private static boolean newLoad = false;
    public static boolean inSplashHub;
    public static long lastSplashHubUpdate = -1;
    public static int playerCount;
    public static String currentServer = "";
    public static String currentNetwork = "";
    public static HashMap<String, String> hubServerMap = new HashMap<>();
    public static HashMap<String, String> dungeonHubServerMap = new HashMap<>();
    public static int tickCounter = 0;
    public static int day;
    public static boolean subscribedToCurrentCHServer;
    public volatile static boolean readyToNotify = false;
    public volatile static String splashHubNumberForNotification = null;
    public volatile static boolean readyToNotifyDungeon = false;
    public volatile static long lastNotification = 0;

    @SubscribeEvent
    public void onWorldJoin(WorldEvent event) {
        if (event instanceof WorldEvent.Load) {
            // for some reason this packet is sent before you load the server, so we have a timer on client tick below
            lastWorldLoad = System.currentTimeMillis();
            if (playerLocation.equalsIgnoreCase("crystal_hollows")) {
                KryoNetwork.SubscribeToCHServer subscribeToCHServer = new KryoNetwork.SubscribeToCHServer();
                subscribedToCurrentCHServer = false;
                subscribeToCHServer.server = currentServer;
                subscribeToCHServer.unsubscribe = true;
                ServerConnection.SubscribeToCHServer(subscribeToCHServer);
            }
            playerLocation = "";
            newLoad = true;
            ServerConnection.waypoints.clear();
            CHWaypoints.filteredWaypoints.clear();
            ServerConnection.filteredItems.clear();
            CHWaypoints.itemCounts.clear();
            if (System.currentTimeMillis() - lastSplashHubUpdate > 3000) {
                inSplashHub = false;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // /locraw 2s after you join the server and every 20s after
            tickCounter++;
            if (lastWorldLoad == -1 || tickCounter % 20 != 0) return;
            tickCounter = 0;
            // Temporarily set newLoad to true when we want to update our locraw (updating is likely not necessary but it ensures we are working with the most recent data and does little harm)
            if (System.currentTimeMillis() - lastPositionUpdate > 30000) newLoad = true;
            if (System.currentTimeMillis() - lastWorldLoad > 2000 || System.currentTimeMillis() - lastPositionUpdate > 30000) {
                if (BingoBrewers.onHypixel && newLoad) {
                    EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                    if (player != null) {
                        player.sendChatMessage("/locraw");
                        ChatTextUtil.cancelLocRawMessage = true;

                        lastPositionUpdate = System.currentTimeMillis();
                        newLoad = false;

                        World world = Minecraft.getMinecraft().theWorld;
                        long worldTime = world.getWorldTime();
                        System.out.println("worldTime: " + worldTime);
                        day = (int) (worldTime / 24000);
                        System.out.println("days: " + day);
                    }
                }
            }
            ServerData serverData = Minecraft.getMinecraft().getCurrentServerData();
            if (serverData != null) {
                currentNetwork = serverData.serverIP;
                if (currentNetwork != null) {
                    String[] serverDomain = currentNetwork.split("\\.");
                    for (String domain : serverDomain) {
                        if (domain.equalsIgnoreCase("hypixel")) {
                            BingoBrewers.onHypixel = true;
                            break;
                        } else {
                            BingoBrewers.onHypixel = false;
                        }
                    }
                }
            }
            // Expire notification after 5s
            if (System.currentTimeMillis() - lastNotification > 5000) readyToNotify = false;
            if (readyToNotify && Minecraft.getMinecraft().thePlayer != null) {
                readyToNotify = false;
                ServerConnection serverConnection = new ServerConnection();
                serverConnection.notification(splashHubNumberForNotification, readyToNotifyDungeon);
            }
        }
    }


    public void setPlayerCount(int playercount) {
        int currentCount = playerCount;
        PlayerInfo.playerCount = playercount;
        // If the player count has changed
        if (currentCount != playercount) {
            KryoNetwork.PlayerCount count = new KryoNetwork.PlayerCount();
            count.playerCount = playercount;
            count.IGN = Minecraft.getMinecraft().thePlayer.getName();
            if (playerHubNumber == null) {
                System.out.println("Player hub number is null");
                return;
            }
            count.server = playerHubNumber;
            ServerConnection serverConnection = new ServerConnection();
            serverConnection.sendPlayerCount(count);
        }
    }

    public static void setReadyToNotify(String hub, boolean dungeonHub) {
        readyToNotify = true;
        splashHubNumberForNotification = hub;
        readyToNotifyDungeon = dungeonHub;
        lastNotification = System.currentTimeMillis();
    }
}
