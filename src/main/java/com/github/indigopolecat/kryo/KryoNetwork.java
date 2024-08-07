package com.github.indigopolecat.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class KryoNetwork {

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(ConnectionIgn.class);
        kryo.register(SplashNotification.class);
        kryo.register(ArrayList.class);
        kryo.register(PlayerCount.class);
        kryo.register(PlayerCountBroadcast.class);
        kryo.register(HashMap.class);
        kryo.register(receiveConstantsOnStartup.class);
        kryo.register(requestLbin.class);
        kryo.register(sendLbin.class);
        kryo.register(sendCHItems.class);
        kryo.register(receiveCHItems.class);
        kryo.register(SubscribeToCHServer.class);
        kryo.register(ChestInfo.class);
        kryo.register(CHChestItem.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(RequestWarpToServer.class);
        kryo.register(BackgroundWarpTask.class);
        kryo.register(RegisterToWarpServer.class);
        kryo.register(DoneWithWarpTask.class);
        kryo.register(CancelWarpRequest.class);
        kryo.register(AbortWarpTask.class);
        kryo.register(QueuePosition.class);
        kryo.register(ServersSummary.class);
        kryo.register(UpdateServers.class);
        kryo.register(RequestLiveUpdatesForServerInfo.class);
        kryo.register(ServerSummary.class);
        kryo.register(WarningBannerInfo.class);
        kryo.register(ReceiveConstantsOnStartupModern.class);
        kryo.register(JoinAlert.class);
        kryo.register(WarperInfo.class);
        kryo.register(PollQueuePosition.class);
    }

    public static class ConnectionIgn {
        public String hello;
    }

    public static class SplashNotification {
        public String message;
        public String splasher;
        public String partyHost;
        public List<String> note;
        public String location;
        public String splash;
        public boolean dungeonHub;
    }

    public static class PlayerCount {
        public int playerCount;
        public String IGN;
        public String server;
    }

    public static class PlayerCountBroadcast {
        public HashMap<String, String> playerCounts;
    }

    public static class receiveConstantsOnStartup {
        public HashMap<Integer, Integer> bingoRankCosts;
        public int POINTS_PER_BINGO;
        public int POINTS_PER_BINGO_COMMUNITIES;
        public ArrayList<String> newCHChestItems = new ArrayList<>();
        public String chItemRegex;
        public String joinAlertTitle;
        public String joinAlertChat;
        public LinkedHashSet<String> CHItemOrder = new LinkedHashSet<>();

    }


    // Request the lbin of any item on ah/bz by item id
    // If they don't exist, they won't be included in the response
    public static class requestLbin {
        public ArrayList<String> items;
    }

    public static class sendLbin {
        public HashMap<String, Integer> lbinMap;
    }

    public static class sendCHItems {
        public ArrayList<CHChestItem> items = new ArrayList<>();
        public int x;
        public int y;
        public int z;
        public String server;
        public int day;
    }

    public static class CHChestItem {
        public String name;
        public String count;
        public Integer numberColor;
        public Integer itemColor;
    }

    public static class SubscribeToCHServer {
        public String server;
        public int day;
        public boolean unsubscribe;
    }

    public static class receiveCHItems {
        public ArrayList<ChestInfo> chestMap;
        public String server; // used to confirm that the server is correct
        public int day; // server's last known day
        public Long lastReceivedDayInfo = Long.MAX_VALUE;
    }
    public static class ChestInfo {
        public int x;
        public int y;
        public int z;
        public ArrayList<CHChestItem> items = new ArrayList<>();
    }

    public static class RequestWarpToServer {
        public String server;
        public String serverType; // Crystal Hollows, Dwarven Mines, etc.
    }

    public static class BackgroundWarpTask {
        public String server; // confirm
        public HashMap<String, String> accountsToWarp;
    }

    public static class RegisterToWarpServer {
        public String server;
        public boolean unregister = false;
    }

    public static class DoneWithWarpTask {
        public boolean successful = true;
        public ArrayList<String> ignsWarped = new ArrayList<>();
    }

    public static class CancelWarpRequest {
        public String server;
    }

    public static class AbortWarpTask {
        public String ign;
        public boolean ineligible;
    }

    public static class QueuePosition {
        public int positionInWarpQueue;
    }

    public static class ServersSummary {
        public HashMap<String, ServerSummary> serverInfo = new HashMap<>();
    }

    public static class UpdateServers {
        public HashMap<String, Long> serversAndLastUpdatedTime = new HashMap<>();
    }

    public static class RequestLiveUpdatesForServerInfo {
        public boolean unrequest;
    }
    public static class WarningBannerInfo {
        public String text;
        public Integer textColor = 0xFFFFFF;
        public Integer backgroundColor = 0x000000;
    }
    public static class ReceiveConstantsOnStartupModern {
        public HashMap<String, Object> constants = new HashMap<>();
    }

    public static class JoinAlert {
        public String joinAlertChat;
        public String joinAlertTitle;
    }
    public static class WarperInfo {
        public String ign;
    }
    public static class PollQueuePosition {
        public String server;
    }
}