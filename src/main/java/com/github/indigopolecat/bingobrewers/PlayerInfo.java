package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayList;
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
    public static boolean inSkyblockOrPTL;
    public static boolean registeredToWarp;
    public volatile static boolean readyToNotify = false;
    public volatile static String splashHubNumberForNotification = null;
    public volatile static boolean readyToNotifyDungeon = false;
    public volatile static long lastNotification = 0;
    public volatile static ArrayList<String> partyMembers = new ArrayList<>(); // uuids, leader not included

    public static void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register(PlayerInfo::onWorldJoin);
    }

    private static void onWorldJoin(ClientPacketListener clientPacketListener, PacketSender packetSender, Minecraft minecraft) {
        lastWorldLoad = System.currentTimeMillis();

        if (playerLocation.equalsIgnoreCase("crystal_hollows")) {
            KryoNetwork.SubscribeToCHServer subscribeToCHServer = new KryoNetwork.SubscribeToCHServer();
            subscribedToCurrentCHServer = false;
            subscribeToCHServer.server = currentServer;
            subscribeToCHServer.unsubscribe = true;
            System.out.println("subscribed");
            ServerConnection.SubscribeToCHServer(subscribeToCHServer);

            KryoNetwork.RegisterToWarpServer unregister = new KryoNetwork.RegisterToWarpServer();
            unregister.unregister = true;
            PlayerInfo.registeredToWarp = false;
            unregister.server = PlayerInfo.currentServer;
            ServerConnection.sendTCP(unregister);
        }

        playerLocation = "";
        ServerConnection.waypoints.clear();
        CHWaypoints.filteredWaypoints.clear();
        CHWaypoints.itemCounts.clear();

        if (System.currentTimeMillis() - lastSplashHubUpdate > 3000) {
            inSplashHub = false;
        }
    }

    //TODO(matita): do event system
    /*
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
    }*/

    public static void setReadyToNotify(String hub, boolean dungeonHub) {
        readyToNotify = true;
        splashHubNumberForNotification = hub;
        readyToNotifyDungeon = dungeonHub;
        lastNotification = System.currentTimeMillis();
    }
}
