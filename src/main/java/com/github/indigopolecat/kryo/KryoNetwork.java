package com.github.indigopolecat.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import java.util.ArrayList;
import java.util.HashMap;
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
        kryo.register(requestItemsForServer.class);
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
        public ArrayList<String> items = new ArrayList<>();
        public int x;
        public int y;
        public int z;
        public String server;
    }

    public static class requestItemsForServer {
        public String server;
        public int day;
    }

    public static class receiveCHItems {
        public HashMap<String, ArrayList<String>> chestMap;
        public String server; // used to confirm that the server is correct
        public int day; // server's last known day
        public Long lastReceivedDayInfo;
    }
}