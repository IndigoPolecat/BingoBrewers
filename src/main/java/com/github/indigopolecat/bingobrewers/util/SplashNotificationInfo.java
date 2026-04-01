package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.bingobrewers.hud.HudManager;
import com.github.indigopolecat.bingobrewers.hud.SplashTitleHud;
import com.github.indigopolecat.kryo.KryoNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SplashNotificationInfo {
    public static final String KEY_COLOR = "§e§l";
    public static final String KEY_RESET = "§r ";
    public static final String DUNGEON_HUB_LABEL = KEY_COLOR + "Dungeon Hub:" + KEY_RESET;
    public static final String HUB_LABEL = KEY_COLOR + "Hub:" + KEY_RESET;
    public static final String PRIVATE_HUB_LABEL = KEY_COLOR + "Private Hub:" + KEY_RESET;
    public static final String PLAYER_COUNT_LABEL = KEY_COLOR + "Players:" + KEY_RESET;
    public static final String SPLASHER_LABEL = KEY_COLOR + "Splasher:" + KEY_RESET;
    public static final String PARTY_LABEL = KEY_COLOR + "Party:" + KEY_RESET;
    public static final String LOCATION_LABEL = KEY_COLOR + "Location:" + KEY_RESET;
    public static final String NOTE_LABEL = KEY_COLOR + "Note:" + KEY_RESET;
    public static final Map<String, SplashNotificationInfo> splashes = Collections.synchronizedMap(new HashMap<>());
    
    public KryoNetwork.SplashNotification lastNotif;
    public String hub = "";
    public String serverID = "";
    public String lobbyPlayerCount = ""; //Matita: Could be an int btw
    public String bingoPartyJoinCommand = "No Party";
    
    public SplashNotificationInfo(KryoNetwork.SplashNotification notification) {
        update(notification, true);
    }
    
    public void update(KryoNetwork.SplashNotification notif) {
        update(notif, false);
    }
    
    public ArrayList<String> getText() {
        ArrayList<String> builder = new ArrayList<>();
        final BingoBrewersConfig config = BingoBrewersConfig.getConfig();
        
        String hubPrefix;
        if(lastNotif.dungeonHub) hubPrefix = DUNGEON_HUB_LABEL;
        else if(lastNotif.isPrivate) hubPrefix = PRIVATE_HUB_LABEL;
        else hubPrefix = HUB_LABEL;
        
        String hubInfo = serverID.isEmpty()? hub : hub + " (" + serverID + ")";
        builder.add(hubPrefix + hubInfo);
        if(config.showPlayerCount) {
            if(ServerConnection.playerCounts.containsKey(serverID)) builder.add(PLAYER_COUNT_LABEL + ServerConnection.playerCounts.get(serverID).second());
            else if(!lobbyPlayerCount.isEmpty()) builder.add(PLAYER_COUNT_LABEL + lobbyPlayerCount);
        }
        if(config.showSplasher) builder.add(SPLASHER_LABEL + lastNotif.splasher);
        if(config.showParty) builder.add(PARTY_LABEL + bingoPartyJoinCommand);
        if(config.showLocation) builder.add(LOCATION_LABEL + lastNotif.location);
        if(config.showNote && !lastNotif.note.isEmpty()) { //Ensure there are notes
            // Add first element on the same line as the key ("Note: ")
            builder.add(NOTE_LABEL + lastNotif.note.getFirst());
            for (int i = 1; i < lastNotif.note.size(); i++) builder.add(lastNotif.note.get(i));
        }
        
        return builder;
    }
    
    private void update(KryoNetwork.SplashNotification notif, boolean initialize) {
        if(notif == null || notif.timestamp == 0 || notif.hub == null) throw new IllegalArgumentException();
        
        if(BingoBrewersConfig.getConfig().splashNotificationsEnabled && (BingoBrewersConfig.getConfig().splashNotificationsOutsideSkyblock || ServerUtils.isBingo())) {
            if(initialize) {
                HudManager.addNewHud(new SplashTitleHud(notif.hub));
            } else {
                if(!serverID.equals(notif.serverID) && !serverID.isEmpty()) {
                    // if the new server ID doesn't match the old, and the old did have a value (i.e. it isn't being set for the first time), then clear the player count and notify
                    lobbyPlayerCount = "";
                    HudManager.addNewHud(new SplashTitleHud(notif.hub));
                } else if((!hub.equals(notif.hub) || lastNotif.dungeonHub != notif.dungeonHub) && serverID.isEmpty()) {
                    // if the hub number changed, and the server ID is empty (if there was a server id and it didn't change when the server ID did, then they're probably already in the right lobby), then clear the player count and notify
                    lobbyPlayerCount = "";
                    HudManager.addNewHud(new SplashTitleHud(notif.hub));
                }
            }
        } else Log.info("Skipped alert for splash " + notif.splash);
        
        serverID = notif.serverID != null && !notif.serverID.isEmpty()? notif.serverID : "";
        hub = notif.isPrivate? "/p join " + notif.hub : notif.hub;
        bingoPartyJoinCommand = notif.partyHost.isEmpty()?"No Party" : "/p join " + notif.partyHost;
        
        lastNotif = notif;
    }
}
