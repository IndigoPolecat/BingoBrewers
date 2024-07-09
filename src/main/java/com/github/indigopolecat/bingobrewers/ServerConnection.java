package com.github.indigopolecat.bingobrewers;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.bingobrewers.Hud.TitleHud;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.*;
import com.github.indigopolecat.kryo.KryoNetwork.ConnectionIgn;
import com.github.indigopolecat.kryo.KryoNetwork.SplashNotification;
import com.github.indigopolecat.kryo.ServerSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.esotericsoftware.minlog.Log.*;
import static com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud.filteredItems;
import static com.github.indigopolecat.bingobrewers.Warping.accountsToWarp;
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
    public static CopyOnWriteArrayList<CHWaypoints> waypoints = new CopyOnWriteArrayList<>();
    // if new ch items are added, they will be in this list
    public static ArrayList<String> newMiscCHItems = new ArrayList<>();
    public static TitleHud joinTitle;
    public static String joinChat;
    public static ArrayList<String> CHItemOrder = new ArrayList<>();
    public static ConcurrentHashMap<String, ServerSummary> serverSummaries = new ConcurrentHashMap<>();

    @Override
    public void run() {
        Client client1 = new Client(16384, 16384);
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
                } else if (object instanceof PlayerCountBroadcast) {
                    PlayerCountBroadcast request = (PlayerCountBroadcast) object;
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
                } else if (object instanceof receiveConstantsOnStartup) {
                    receiveConstantsOnStartup request = (receiveConstantsOnStartup) object;
                    ChestInventories.rankPriceMap = request.bingoRankCosts;
                    CHChests.regex = request.chItemRegex;
                    newMiscCHItems = request.newCHChestItems;
                    if (request.joinAlertChat != null) {
                        joinChat = request.joinAlertChat;
                    }
                    if (request.joinAlertTitle != null) {
                        joinTitle = new TitleHud(request.joinAlertTitle, 0xFF5555, 10000, true);
                    }
                    CHItemOrder = new ArrayList<>(request.CHItemOrder);

                } else if (object instanceof receiveCHItems) {
                    receiveCHItems CHItems = (receiveCHItems) object;
                    System.out.println("Received CH Chests for " + CHItems.server);
                    ArrayList<ChestInfo> chests = CHItems.chestMap;
                    if (CHItems.server.equals(PlayerInfo.currentServer)) {
                        if (CHItems.day - 1 > PlayerInfo.day || System.currentTimeMillis() - (CHItems.lastReceivedDayInfo != null ? CHItems.lastReceivedDayInfo : Long.MAX_VALUE) > 25_200_000) return; // ignore if the server is younger than last known, or it's been more than 7 hours since info was received
                        for (ChestInfo chest : chests) {
                            CHWaypoints chWaypoints = new CHWaypoints(chest.x, chest.y, chest.z, chest.items);
                            waypoints.add(chWaypoints);

                            for (CHChestItem item : chest.items) {
                                CrystalHollowsItemTotal.sumItems(item);
                            }

                            for (CHWaypoints waypoint : CHWaypoints.filteredWaypoints) {
                                waypoint.filteredExpandedItems.clear();
                            }

                            BingoBrewersConfig.filterPowder();
                            BingoBrewersConfig.filterGoblinEggs();
                            BingoBrewersConfig.filterRoughGemstones();
                            BingoBrewersConfig.filterJasperGemstones();
                            BingoBrewersConfig.filterRobotParts();
                            BingoBrewersConfig.filterPrehistoricEggs();
                            BingoBrewersConfig.filterPickonimbus();
                            BingoBrewersConfig.filterMisc();
                            organizeWaypoints();

                        }
                    }
                } else if (object instanceof ServersSummary) {
                    ServersSummary servers = (ServersSummary) object;
                    serverSummaries.putAll(servers.serverInfo);
                    // remove outdated entries
                    for (ServerSummary server : serverSummaries.values()) {
                        if (server.serverType == null) {
                            serverSummaries.remove(server.server);
                        }
                    }
                } else if (object instanceof QueuePosition) {
                    // if you have to wait in the queue, this will give you your current position
                    // gonna leave it for you to implement because I think the permanent value should be stored in the class for rendering the menu
                } else if (object instanceof BackgroundWarpTask) {
                    BackgroundWarpTask warpTask = (BackgroundWarpTask) object;
                    if (warpTask.server.equals(PlayerInfo.currentServer) && !warpTask.accountsToWarp.isEmpty() && accountsToWarp.isEmpty()) {
                        accountsToWarp = new ConcurrentHashMap<>(warpTask.accountsToWarp);
                        Warping.server = warpTask.server;

                        if (accountsToWarp.isEmpty()) {
                            Warping.abort(false);
                            return;
                        }

                        if (Warping.warpThread != null) {
                            Warping.warpThread.end();
                            Warping.warpThread = new BackgroundWarpThread();
                            Thread warpThread = new Thread(Warping.warpThread);
                            warpThread.start();
                        }
                    }
                } else if (object instanceof WarningBannerInfo) {

                } else if (object instanceof AbortWarpTask) {
                    Warping.PARTY_EMPTY_KICK = false;
                    Warping.kickParty = true;
                    accountsToWarp.clear();
                    Warping.partyReady = false;
                    Warping.waitingOnLocation = true;
                    Warping.warpThread.stop = true;
                }
            }


            @Override
            public void disconnected(Connection connection) {
                reconnect();
            }

        });

        BingoBrewers.client.start();
        System.out.println("Client started, Test Instance: " + BingoBrewers.TEST_INSTANCE);
        if (BingoBrewers.TEST_INSTANCE) {
            // Note: for those compiling their own version, the test server will rarely be active so keep the boolean as false
            System.out.println("Connecting to test server");
            BingoBrewers.client.connect(3000, "38.46.216.110", 9090, 9191);
        } else {
            BingoBrewers.client.connect(3000, "38.46.216.110", 8080, 7070);
        }
        System.out.println("Connected to server.");
        // send server player ign and version
        ConnectionIgn response = new ConnectionIgn();
        String ign = Minecraft.getMinecraft().getSession().getUsername();
        String uuid = Minecraft.getMinecraft().getSession().getProfile().getId().toString();
        response.hello = ign + "|v0.3.3|Beta|" + uuid;
        System.out.println("sending " + response.hello);
        BingoBrewers.client.sendTCP(response);
        System.out.println("sent");
        PlayerInfo.subscribedToCurrentCHServer = false;
        // List of all keys that may be used in infopanel, in the order they'll be rendered in an element
        keyOrder.clear(); // clear the list so it doesn't keep adding the same keys every time you reconnect
        keyOrder.add(HUB);
        keyOrder.add(SPLASHER);
        keyOrder.add(PARTY);
        keyOrder.add(LOCATION);
        keyOrder.add(NOTE);
        repeat = false;
    }

    public static void organizeWaypoints() {
        // filter the items into the correct order
        ArrayList<Integer> orderedIndexes = new ArrayList<>();
        for (CrystalHollowsItemTotal total : filteredItems) {
            String item = total.itemName;
            if (orderedIndexes.contains(CHItemOrder.indexOf(item))) continue;
            orderedIndexes.add(CHItemOrder.indexOf(item));
        }
        Collections.sort(orderedIndexes);
        orderedIndexes.removeIf(index -> index == -1);

        ConcurrentLinkedDeque<CrystalHollowsItemTotal> sortedDeque = new ConcurrentLinkedDeque<>(filteredItems);
        filteredItems.clear();
        for (Integer index : orderedIndexes) {
            String item = CHItemOrder.get(index);
            for (CrystalHollowsItemTotal total : sortedDeque) {
                if (total.itemName.equalsIgnoreCase(item)) {
                    filteredItems.add(total);
                }
            }
        }

        for (CHWaypoints waypoint : CHWaypoints.filteredWaypoints) {
            orderedIndexes.clear();
            for (CHChestItem item : waypoint.filteredExpandedItems) {
                String name = item.name;
                orderedIndexes.add(CHItemOrder.indexOf(name));
            }
            Collections.sort(orderedIndexes);
            orderedIndexes.removeIf(index -> index == -1);
            CopyOnWriteArrayList<CHChestItem> sortedItems = new CopyOnWriteArrayList<>(waypoint.filteredExpandedItems);
            waypoint.filteredExpandedItems.clear();
            for (Integer index : orderedIndexes) {
                String item = CHItemOrder.get(index);
                for (CHChestItem chestItem : sortedItems) {
                    if (chestItem.name.equalsIgnoreCase(item)) {
                        waypoint.filteredExpandedItems.add(chestItem);
                    }
                }
            }
        }
    }

    public synchronized void setClient(Client client) {
        BingoBrewers.client = client;
    }

    public static synchronized Client getClient() {
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
        if (hub == null) {
            hub = "Unknown Hub";
        }
        String splasher = notif.splasher;
        String partyHost = notif.partyHost;
        if (!partyHost.equals("No Party")) {
            partyHost = "/p join " + partyHost;
        }
        List<String> note = notif.note;
        // This should always be "Bea House" but is hard coded server side incase it ever needs to change quickly
        String location = notif.location;

        HashMap<String, ArrayList<String>> splashInfo = new HashMap<>();

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
        if(!SplashHud.onBingo) return; // non-profile bingo splashes setting was here
        if(!SplashHud.inSkyblockorPTLobby && !BingoBrewersConfig.splashNotificationsOutsideSkyblock) return;
        if(!BingoBrewers.onHypixel) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (!dungeonHub) {
            if (hub.equalsIgnoreCase("Unknown Hub")) {
                hub = "Unknown Hub";
            } else {
                hub = "Hub " + hub;
            }
            TitleHud titleHud = new TitleHud("Splash in " + hub, BingoBrewersConfig.alertTextColor.getRGB(), 4000, false);
            setActiveHud(titleHud);
        } else {
            if (hub.equalsIgnoreCase("Unknown Hub")) {
                hub = "Unknown Dungeon Hub";
            } else {
                hub = "Dungeon Hub " + hub;
            }
            TitleHud titleHud = new TitleHud("Splash in " + hub, BingoBrewersConfig.alertTextColor.getRGB(), 4000, false);
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

    public static synchronized void SubscribeToCHServer(SubscribeToCHServer server) {
        Client currentClient = getClient();

        if (currentClient == null) {
            LoggerUtil.LOGGER.info("Client is null");
            return;
        }
        currentClient.sendTCP(server);
        if (!server.unsubscribe) {
            ServerConnection.waypoints.clear();
            CHWaypoints.filteredWaypoints.clear();
            CrystalHollowsHud.filteredItems.clear();
            CHWaypoints.itemCounts.clear();
            System.out.println("Subscribing to " + PlayerInfo.currentServer);
            PlayerInfo.subscribedToCurrentCHServer = true;
        } else {
            System.out.println("Unsubscribing from " + PlayerInfo.currentServer);
        }
    }

    public static synchronized void requestLiveUpdates(boolean unrequest) {
        RequestLiveUpdatesForServerInfo request = new RequestLiveUpdatesForServerInfo();
        request.unrequest = unrequest;
        Client client = getClient();

        if (client == null) {
            LoggerUtil.LOGGER.info("Client is null");
            return;
        }
        client.sendTCP(request);
    }

    public static synchronized void sendTCP(Object object) {
        Client client = getClient();

        if (client == null) {
            LoggerUtil.LOGGER.info("Client is null");
            return;
        }
        client.sendTCP(object);
    }




    public void reconnect() {
        BingoBrewers.client.close();
        BingoBrewers.client.removeListener(this);
        if (waitTime == 0) {
            waitTime = (int) (5000 * Math.random());
        }
        System.out.println("Disconnected from server. Reconnecting in " + waitTime + " milliseconds.");
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
                } else {
                    waitTime = 60000;
                }
                System.out.println("Disconnected from server. Reconnecting in " + waitTime + " milliseconds.");
            }
        }
    }

}
