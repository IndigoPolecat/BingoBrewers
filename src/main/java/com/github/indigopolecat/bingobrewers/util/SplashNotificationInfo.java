package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.Hud.SplashInfoHud;
import com.github.indigopolecat.kryo.KryoNetwork;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class SplashNotificationInfo {
    public String id;
    public long timestamp;
    public String hubNumber = "";
    public String lobbyPlayerCount = "";
    public boolean dungeonHub = false;
    public String splasherIGN = "";
    public String bingoPartyJoinCommand = "No Party";
    public String location = "";
    public ArrayList<String> splasherNotes = new ArrayList<>();
    public boolean example = false;

    public static final String DUNGEON_HUB = "Dungeon Hub";
    public static final String HUB = "Hub";
    public static final String SPLASHER = "Splasher";
    public static final String PARTY = "Party";
    public static final String LOCATION = "Location";
    public static final String NOTE = "Note";


    public SplashNotificationInfo(KryoNetwork.SplashNotification notificationInfo, boolean sendNotif) {
        if (notificationInfo == null || notificationInfo.timestamp == 0 || notificationInfo.hub == null) return;
        this.id = notificationInfo.splash;
        this.timestamp = notificationInfo.timestamp;
        this.hubNumber = notificationInfo.hub;
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
        this.splasherNotes.add("This is an example splash");
        this.timestamp = 0;
    }

}
