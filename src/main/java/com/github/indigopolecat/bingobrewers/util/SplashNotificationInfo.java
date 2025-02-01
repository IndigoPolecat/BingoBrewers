package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.Hud.SplashInfoHud;
import com.github.indigopolecat.bingobrewers.Hud.TitleHud;
import com.github.indigopolecat.bingobrewers.PlayerInfo;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import static com.github.indigopolecat.bingobrewers.ServerConnection.setActiveHud;

public class SplashNotificationInfo {
    public String id;
    public long timestamp;
    public String hubNumber = "";
    public String serverID = "";
    public String lobbyPlayerCount = "";
    public boolean dungeonHub = false;
    public String splasherIGN = "";
    public String bingoPartyJoinCommand = "No Party";
    public String location = "";
    public ArrayList<String> splasherNotes = new ArrayList<>();
    public boolean example = false;

    public static final String DUNGEON_HUB = "Dungeon Hub";
    public static final String HUB = "Hub";
    public static final String PLAYER_COUNT = "Players";
    public static final String SPLASHER = "Splasher";
    public static final String PARTY = "Party";
    public static final String LOCATION = "Location";
    public static final String NOTE = "Note";


    public SplashNotificationInfo(KryoNetwork.SplashNotification notificationInfo, boolean sendNotif) {
        if (notificationInfo == null || notificationInfo.timestamp == 0 || notificationInfo.hub == null) return;
        this.id = notificationInfo.splash;
        this.timestamp = notificationInfo.timestamp;
        this.hubNumber = notificationInfo.hub;
        if (notificationInfo.serverID != null && !notificationInfo.serverID.isEmpty()) {
            serverID = notificationInfo.serverID;
        }
        this.dungeonHub = notificationInfo.dungeonHub;
        this.splasherNotes = notificationInfo.note;
        this.splasherIGN = notificationInfo.splasher;
        this.location = notificationInfo.location;
        if (notificationInfo.partyHost.equals("No Party")) {
            this.bingoPartyJoinCommand = notificationInfo.partyHost;
        } else {
            this.bingoPartyJoinCommand = "/p join " + notificationInfo.partyHost;
        }
    }

    public SplashNotificationInfo(boolean example) {
        if (!example) return;

        this.example = true;
        this.hubNumber = "14";
        this.splasherIGN = "indigo_polecat";
        this.bingoPartyJoinCommand = "/p join BingoParty";
        this.location = "Bea House";
        this.splasherNotes.add("This is an example splash with a long example note, which is intended to wrap to a new line.");
        this.timestamp = 0;
    }

    // This is called onTickEvent in PlayerInfo when the player is not null, only called once per notification
    public static synchronized void notification(String hub, boolean dungeonHub) {
        if (!BingoBrewersConfig.splashNotificationsEnabled) return;
        if(!PlayerInfo.onBingo) return; // non-profile bingo splashes setting was here
        if(!PlayerInfo.inSkyblockOrPTL && !BingoBrewersConfig.splashNotificationsOutsideSkyblock) return;
        if(!BingoBrewers.onHypixel) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (!dungeonHub) {
            if (hub.equalsIgnoreCase("Unknown Hub")) {
                hub = "Unknown Hub";
            } else {
                hub = "Hub " + hub;
            }
            TitleHud titleHud = new TitleHud("Splash in " + hub, BingoBrewersConfig.alertTextColor.getRgba(), 4000, false);
            setActiveHud(titleHud);
        } else {
            if (hub.equalsIgnoreCase("Unknown Hub")) {
                hub = "Unknown Dungeon Hub";
            } else {
                hub = "Dungeon Hub " + hub;
            }
            TitleHud titleHud = new TitleHud("Splash in " + hub, BingoBrewersConfig.alertTextColor.getRgba(), 4000, false);
            setActiveHud(titleHud);
        }

        player.playSound("bingobrewers:splash_notification", BingoBrewersConfig.splashNotificationVolume/100f, 1.0f);
    }

}
