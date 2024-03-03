package com.github.indigopolecat.bingobrewers;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.ConnectionIgn;
import com.github.indigopolecat.kryo.KryoNetwork.SplashNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.esotericsoftware.minlog.Log.*;
import static java.lang.String.valueOf;

public class ServerConnection extends Listener implements Runnable {

    public static final String DUNGEON_HUB = "Dungeon Hub";
    public static final String HUB = "Hub";
    public static final String SPLASHER = "Splasher";
    public static final String PARTY = "Party";
    public static final String LOCATION = "Location";
    public static final String NOTE = "Note";

    // The Hud renderer checks this every time it renders
    public static ArrayList<HashMap<String, ArrayList<String>>> mapList = new ArrayList<>();
    public static ArrayList<String> keyOrder = new ArrayList<>();
    int waitTime = 0;
    boolean repeat;
    public static ArrayList<String> hubList = new ArrayList<>();
    long originalTime = -1;


    @Override
    public void run() {
        Client client1 = new Client();
        setClient(client1);
        if (BingoBrewers.client == null) {
            LoggerUtil.LOGGER.info("Client is null");
        }
        try {
            connection();
        } catch (Exception e) {
            LoggerUtil.LOGGER.info("Server Connection Error: " + e.getMessage());
            reconnect();
        }

    }

    private void connection() throws IOException {
        Log.set(LEVEL_ERROR);
        KryoNetwork.register(BingoBrewers.client);
        BingoBrewers.client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {

                if (object instanceof ConnectionIgn) {
                    ConnectionIgn request = (ConnectionIgn) object;
                    LoggerUtil.LOGGER.info(request.hello);
                } else if (object instanceof SplashNotification) {
                    LoggerUtil.LOGGER.info("Received splash notification");
                    boolean sendNotif = true;
                    SplashNotification notif = (SplashNotification) object;
                    // Remove the previous splash notification with the same ID (if message is edited)
                    for (int i = 0; i < mapList.size(); i++) {
                        HashMap<String, ArrayList<String>> map = mapList.get(i);
                        if (map.get("Splash").get(0).equals(notif.splash)) {
                            ArrayList<String> hubField = map.get(HUB);
                            // Don't send notification if the hub # or hub type (dungeon/normal) hasn't changed
                            try {
                                String hubNumber = hubField.get(1).replaceAll(": (\\d+).*", "$1");
                                if (hubNumber.equals(notif.message) && notif.dungeonHub == hubField.get(0).contains(DUNGEON_HUB)) {
                                    sendNotif = false;
                                    hubList.remove(hubNumber);
                                    hubList.remove("DH" + hubNumber);
                                }

                            } catch (Exception ignored) {

                            }

                            // keep track of the original time the splash was sent, instead of updating each time it's edited
                            originalTime = Long.parseLong(map.get("Time").get(0));

                            mapList.remove(mapList.get(i));
                        }
                    }
                    updateMapList(notif, sendNotif);
                } else if (object instanceof KryoNetwork.PlayerCountBroadcast) {
                    KryoNetwork.PlayerCountBroadcast request = (KryoNetwork.PlayerCountBroadcast) object;
                    for (HashMap<String, ArrayList<String>> map : mapList) {
                        if (map.containsKey(HUB)) {
                            String hub = map.get(HUB).get(1).replaceAll(": (\\d+).*", "$1");
                            if (request.playerCounts.containsKey(hub)) {
                                // If the hub is a dungeon hub, it has a 24 player limit
                                if (map.get(HUB).get(0).equals(DUNGEON_HUB)) {
                                    map.get(HUB).set(1, ": " + hub + " (" + request.playerCounts.get(hub) + "/24)");
                                } else {
                                    map.get(HUB).set(1, ": " + hub + " (" + request.playerCounts.get(hub) + "/80)");
                                }
                            }
                        }
                    }
                }
            }


            @Override
            public void disconnected(Connection connection) {
                reconnect();
            }

        });
        BingoBrewers.client.start();
        if (BingoBrewers.TEST_INSTANCE) {
            // Note: for those compiling their own version, the test server will rarely be active so keep the boolean as false
            LoggerUtil.LOGGER.info("Connecting to test server");
            BingoBrewers.client.connect(3000, "38.46.216.110", 9090, 9191);
        } else {
            BingoBrewers.client.connect(3000, "38.46.216.110", 8080, 7070);
        }
        LoggerUtil.LOGGER.info("Connected to server.");
        // send server player ign and version
        ConnectionIgn response = new ConnectionIgn();
        String ign = Minecraft.getMinecraft().getSession().getUsername();
        response.hello = ign + "|v0.1|Beta";
        LoggerUtil.LOGGER.info("sending " + response.hello);
        BingoBrewers.client.sendTCP(response);
        LoggerUtil.LOGGER.info("sent");
        // List of all keys that may be used in infopanel, in the order they'll be rendered in an element
        keyOrder.clear(); // clear the list so it doesn't keep adding the same keys every time you reconnect
        keyOrder.add(HUB);
        keyOrder.add(SPLASHER);
        keyOrder.add(PARTY);
        keyOrder.add(LOCATION);
        keyOrder.add(NOTE);
        repeat = false;
    }

    public synchronized void setClient(Client client) {
        BingoBrewers.client = client;
    }

    public synchronized Client getClient() {
        return BingoBrewers.client;

    }

    public synchronized void setActiveHud(TitleHud activeTitle) {
        BingoBrewers.activeTitle = activeTitle;
    }

    public synchronized TitleHud getActiveHud() {
        return BingoBrewers.activeTitle;
    }

    public void updateMapList(SplashNotification notif, boolean sendNotif) {
        String hub = notif.message;
        String splasher = notif.splasher;
        String partyHost = notif.partyHost;
        if (!partyHost.equals("No Party")) {
            partyHost = "/p join " + partyHost;
        }
        List<String> note = notif.note;
        // This should always be "Bea House" but is hard coded server side incase it ever needs to change quickly
        String location = notif.location;

        HashMap<String, ArrayList<String>> splashInfo = new HashMap<String, ArrayList<String>>();

        ArrayList<String> hubInfo = new ArrayList<>();
        if (!notif.dungeonHub) {
            hubInfo.add(HUB);
            hubList.add(hub);
        } else {
            hubInfo.add(DUNGEON_HUB);
            // Identify a hub as a dungeonhub to avoid mixing up regular hubs and dhubs
            hubList.add("DH" + hub);
        }
        hubInfo.add(": " + hub);
        splashInfo.put(HUB, hubInfo);

        ArrayList<String> splasherInfo = new ArrayList<>();
        splasherInfo.add(SPLASHER);
        splasherInfo.add(": " + splasher);
        splashInfo.put(SPLASHER, splasherInfo);

        ArrayList<String> partyInfo = new ArrayList<>();
        partyInfo.add("Bingo Party");
        partyInfo.add(": " + partyHost);
        splashInfo.put(PARTY, partyInfo);

        ArrayList<String> locationInfo = new ArrayList<>();
        locationInfo.add(LOCATION);
        locationInfo.add(": " + location);
        splashInfo.put(LOCATION, locationInfo);

        ArrayList<String> noteInfo = new ArrayList<>();
        noteInfo.add(NOTE);
        if (note == null || note.isEmpty()) {
            noteInfo.add(": No Note");
        } else {
            noteInfo.add(": ");
            noteInfo.addAll(note);
        }
        splashInfo.put(NOTE, noteInfo);

        ArrayList<String> timeInfo = new ArrayList<>();
        if (originalTime != -1) {
            timeInfo.add(valueOf(originalTime));
            originalTime = -1;
        } else {
            timeInfo.add(valueOf(System.currentTimeMillis()));
        }
        splashInfo.put("Time", timeInfo);
        ArrayList<String> splashId = new ArrayList<>();
        splashId.add(notif.splash);
        splashInfo.put("Splash", splashId);

        mapList.add(splashInfo);
        if (sendNotif) {
            PlayerInfo.setReadyToNotify(hub, notif.dungeonHub);
        }
    }

    // This is called onTickEvent in PlayerInfo when the player is not null
    public synchronized void notification(String hub, boolean dungeonHub) {
        if (!BingoBrewersConfig.splashNotificationsEnabled) return;
        if(!HudRendering.onBingo && !BingoBrewersConfig.splashNotificationsInBingo) return;
        if(!HudRendering.inSkyblockorPTLobby && !BingoBrewersConfig.splashNotificationsOutsideSkyblock) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (!dungeonHub) {
            TitleHud titleHud = new TitleHud("Splash in Hub " + hub, BingoBrewersConfig.alertTextColor.getRGB(), 4000);
            setActiveHud(titleHud);
        } else {
            TitleHud titleHud = new TitleHud("Splash in Dungeon Hub " + hub, BingoBrewersConfig.alertTextColor.getRGB(), 4000);
            setActiveHud(titleHud);
        }

        player.playSound("bingobrewers:splash_notification", BingoBrewersConfig.splashNotificationVolume/100f, 1.0f);
    }

    public synchronized void sendPlayerCount(KryoNetwork.PlayerCount count) {
        if (!BingoBrewersConfig.splashNotificationsEnabled) return;
        Client currentClient = getClient();
        if (currentClient == null) {
            LoggerUtil.LOGGER.info("Client is null");
            return;
        }
        currentClient.sendUDP(count);
    }

    public void reconnect() {
        BingoBrewers.client.close();
        BingoBrewers.client.removeListener(this);
        if (waitTime == 0) {
            waitTime = (int) (5000 * Math.random());
        }
        LoggerUtil.LOGGER.info("Disconnected from server. Reconnecting in " + waitTime + " milliseconds.");
        repeat = true;
        while (repeat) {
            try {
                BingoBrewers.client = new Client();
                connection();
            } catch (Exception e) {
                LoggerUtil.LOGGER.info("Server Connection Error: " + e.getMessage());
                BingoBrewers.client.close();
                BingoBrewers.client.removeListener(this);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (waitTime < 60000) {
                    waitTime *= 2;
                } else if (waitTime > 60000) {
                    waitTime = 60000;
                }
                LoggerUtil.LOGGER.info("Disconnected from server. Reconnecting in " + waitTime + " milliseconds.");
            }
        }
    }
}
