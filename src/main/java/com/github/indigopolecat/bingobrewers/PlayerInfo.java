package com.github.indigopolecat.bingobrewers;


import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;

public class PlayerInfo {
    public static String playerLocation = "";
    public static String playerGameType = "";
    public static String playerHubNumber = "";
    public static long lastWorldLoad = -1;
    public static long lastPositionUpdate = -1;
    private static boolean newLoad = false;
    public static boolean inSplashHub;
    public static long lastSplashHubUpdate = -1;
    public static int playerCount;
    public static String currentServer = "";
    public static HashMap<String, String> hubServerMap = new HashMap<>();
    public static HashMap<String, String> dungeonHubServerMap = new HashMap<>();

    @SubscribeEvent
    public void onWorldJoin(WorldEvent event) {
        if (event instanceof WorldEvent.Load) {
            // for some reason this packet is sent before you load the server, so we have a timer on client tick below
            lastWorldLoad = System.currentTimeMillis();
            playerLocation = "";
            newLoad = true;
            if (System.currentTimeMillis() - lastSplashHubUpdate > 3000) {
                inSplashHub = false;
                System.out.println("false");
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // /locraw 2s after you join the server
            if (lastWorldLoad != -1 && System.currentTimeMillis() - lastWorldLoad > 2000) {
                if (!newLoad) return;
                EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                if (player != null) {
                    player.sendChatMessage("/locraw");
                    // unused, for rerunning the command occassionally if there are bugs
                    lastPositionUpdate = System.currentTimeMillis();
                    newLoad = false;
                }
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
            count.server = playerHubNumber;
            ServerConnection serverConnection = new ServerConnection();
            serverConnection.sendPlayerCount(count);
        }
    }
}
