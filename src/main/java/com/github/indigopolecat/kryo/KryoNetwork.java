package com.github.indigopolecat.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KryoNetwork {

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(ReceivedString.class);
        kryo.register(ResponseString.class);
        kryo.register(SplashNotification.class);
        kryo.register(ArrayList.class);
        kryo.register(PlayerCount.class);
        kryo.register(PlayerCountBroadcast.class);
        kryo.register(HashMap.class);
    }
    public static class ReceivedString {
        public String hello;

    }

    public static class ResponseString {
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
}