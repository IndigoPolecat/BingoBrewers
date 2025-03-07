package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.PlayerInfo;
import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.github.indigopolecat.bingobrewers.Hud.SplashInfoHud.activeSplashes;

public class SplashUtils {
    public static void setPlayerCount(int playercount, String splashID) {
        // this is run if the player is either in the server ID specified in the splash, or in the same lobby as the splasher IGN
        // This check is only run when the tablist playercount is updated, definetily not optimal but should be reasonably accurate
        int currentCount = PlayerInfo.playerCount;
        PlayerInfo.playerCount = playercount;
        // If the player count has changed
        if (currentCount != playercount) {
            KryoNetwork.PlayerCount count = new KryoNetwork.PlayerCount();
            count.playerCount = playercount;
            if (PlayerInfo.playerHubNumber == null) {
                System.out.println("Player hub number is null");
                return;
            }
            count.hub = PlayerInfo.playerHubNumber;
            count.serverID = PlayerInfo.currentServer;
            count.splashID = splashID;

            ServerConnection serverConnection = new ServerConnection();
            serverConnection.sendPlayerCount(count);
        }
    }

    public static void setReadyToNotify(String hub, boolean dungeonHub) {
        PlayerInfo.readyToNotify = true;
        PlayerInfo.splashHubNumberForNotification = hub;
        PlayerInfo.readyToNotifyDungeon = dungeonHub;
        PlayerInfo.lastNotification = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        World world = event.world;
        if (event.entity instanceof EntityPlayer) {
            if (activeSplashes.isEmpty()) return;
            PlayerInfo.currentRenderedPlayerEntities.clear(); // super inefficient but I'm pretty sure forge doesn't have an equivalant event for despawning an entity so this it is.
            for (EntityPlayer player : world.playerEntities) {
                System.out.println(player.getName());
                PlayerInfo.currentRenderedPlayerEntities.add(player.getName());
            }
        }
    }
}
