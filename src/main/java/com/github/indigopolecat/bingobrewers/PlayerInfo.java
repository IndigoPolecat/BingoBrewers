package com.github.indigopolecat.bingobrewers;


import cc.polyfrost.oneconfig.libs.checker.units.qual.A;
import com.esotericsoftware.kryonet.Server;
import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import com.github.indigopolecat.bingobrewers.Hud.TitleHud;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
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
    public static boolean inParty;
    public static boolean registeredToWarp;
    public volatile static boolean readyToNotify = false;
    public volatile static String splashHubNumberForNotification = null;
    public volatile static boolean readyToNotifyDungeon = false;
    public volatile static long lastNotification = 0;
    public volatile static ArrayList<String> partyMembers = new ArrayList<>(); // uuids, leader not included

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

                KryoNetwork.RegisterToWarpServer unregister = new KryoNetwork.RegisterToWarpServer();
                unregister.unregister = true;
                unregister.server = PlayerInfo.currentServer;
                ServerConnection.sendTCP(unregister);
            }
            playerLocation = "";
            ServerConnection.waypoints.clear();
            CHWaypoints.filteredWaypoints.clear();
            CrystalHollowsHud.filteredItems.clear();
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

            if (System.currentTimeMillis() - lastWorldLoad > 2000 || System.currentTimeMillis() - lastPositionUpdate > 30000) {
                if (BingoBrewers.onHypixel) {
                    World world = Minecraft.getMinecraft().theWorld;
                    if (world != null) {
                        long worldTime = world.getWorldTime();
                        day = (int) (worldTime / 24000);
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

            if (ServerConnection.joinTitle != null && Minecraft.getMinecraft().thePlayer != null) {
                BingoBrewers.activeTitle = new TitleHud(ServerConnection.joinTitle);
                ServerConnection.joinTitle = null;
            }
            if (ServerConnection.joinChat != null && Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(ServerConnection.joinChat));
                ServerConnection.joinChat = null;
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
