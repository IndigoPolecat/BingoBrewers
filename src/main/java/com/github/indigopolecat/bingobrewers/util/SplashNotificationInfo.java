package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.hud.HudManager;
import com.github.indigopolecat.bingobrewers.hud.SplashTitleHud;
import com.github.indigopolecat.kryo.KryoNetwork.SplashNotification;

import java.util.ArrayList;

public class SplashNotificationInfo {
    public String id;
    public long timestamp;
    public String hub = "";
    public String serverID = "";
    public String lobbyPlayerCount = "";
    public boolean isPrivate;
    public boolean dungeonHub = false;
    public String splasherIGN = "";
    public boolean realIGN;
    public String bingoPartyJoinCommand = "No Party";
    public String location = "";
    public ArrayList<String> splasherNotes = new ArrayList<>();
    public boolean example = false;

    public static final String DUNGEON_HUB = "Dungeon Hub:§r ";
    public static final String HUB = "Hub:§r ";
    public static final String PRIVATE_HUB = "Private Hub:§r ";
    public static final String PLAYER_COUNT = "Players:§r ";
    public static final String SPLASHER = "Splasher:§r ";
    public static final String PARTY = "Party:§r ";
    public static final String LOCATION = "Location:§r ";
    public static final String NOTE = "Note:§r ";
    public static boolean inSplashHub;


    public SplashNotificationInfo(SplashNotification notificationInfo, SplashNotificationInfo oldSplash) {
        if (notificationInfo == null || notificationInfo.timestamp == 0 || notificationInfo.hub == null) return;

        this.id = notificationInfo.splash;
        this.timestamp = notificationInfo.timestamp;
        if (notificationInfo.serverID != null && !notificationInfo.serverID.isEmpty()) {
            serverID = notificationInfo.serverID;
        }
        this.isPrivate = notificationInfo.isPrivate;

        if (this.isPrivate) {
            this.hub = "/p join " + notificationInfo.hub;
        } else {
            this.hub = notificationInfo.hub;
        }

        this.dungeonHub = notificationInfo.dungeonHub;
        this.splasherNotes = notificationInfo.note;
        this.splasherIGN = notificationInfo.splasher;
        this.realIGN = notificationInfo.splasherRealIGN;

        this.location = notificationInfo.location;
        if (notificationInfo.partyHost.isEmpty()) {
            this.bingoPartyJoinCommand = "No Party";
        } else {
            this.bingoPartyJoinCommand = "/p join " + notificationInfo.partyHost;
        }

        if (oldSplash != null) {
            if (!oldSplash.serverID.equals(notificationInfo.serverID) && !oldSplash.serverID.isEmpty()) {
                // if the new server ID doesn't match the old, and the old did have a value (i.e. it isn't being set for the first time), then clear the player count and notify
                this.lobbyPlayerCount = "";
                HudManager.addNewHud(new SplashTitleHud(notificationInfo.hub));
            } else if ((!oldSplash.hub.equals(notificationInfo.hub) || oldSplash.dungeonHub != notificationInfo.dungeonHub) && oldSplash.serverID.isEmpty()) {
                // if the hub number changed, and the server ID is empty (if there was a server id and it didn't change when the server ID did, then they're probably already in the right lobby), then clear the player count and notify
                this.lobbyPlayerCount = "";
                HudManager.addNewHud(new SplashTitleHud(notificationInfo.hub));
            } else {
                this.lobbyPlayerCount = oldSplash.lobbyPlayerCount;
            }
        } else {
            HudManager.addNewHud(new SplashTitleHud(notificationInfo.hub));
        }
    }
}
