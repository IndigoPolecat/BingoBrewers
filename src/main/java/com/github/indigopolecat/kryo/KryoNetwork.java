package com.github.indigopolecat.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class KryoNetwork {
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(EncryptedString.class);
        kryo.register(byte[].class);
        kryo.register(ServerPublicKey.class);
        kryo.register(Authentication.class);
        kryo.register(ClientSymmetricKey.class);
        kryo.register(ConnectionIGN.class);
        kryo.register(SplashNotification.class);
        kryo.register(ArrayList.class);
        kryo.register(PlayerCount.class);
        kryo.register(PlayerCountBroadcast.class);
        kryo.register(HashMap.class);
        kryo.register(ClientRequestLowestBINPrices.class);
        kryo.register(ServerSendLowestBINPrices.class);
        kryo.register(ClientSendCHItems.class);
        kryo.register(ServerSendCHItems.class);
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
        kryo.register(ServerSummary.class);
        kryo.register(ServersSummary.class);
        kryo.register(UpdateServers.class);
        kryo.register(RequestLiveUpdatesForServerInfo.class);
        kryo.register(WarningBannerInfo.class);
        kryo.register(ClientReceiveServerConstantValues.class);
        kryo.register(JoinAlert.class);
        kryo.register(WarperInfo.class);
        kryo.register(RequestQueuePosition.class);
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ServerPublicKey {
        public String public_key;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ClientSymmetricKey {
        public String symmetric_key;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class Authentication {
        public EncryptedString AuthID;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ConnectionIGN {
        public EncryptedString IGN;
        public EncryptedString version;
        public EncryptedString uuid;
        public int connections;
        public HashMap<Object, Object> accountInformation = new HashMap<>(); // for future Misc. purposes
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class EncryptedString {
        public String string;
        public byte[] iv;
    }
    
    @NoArgsConstructor @AllArgsConstructor @ToString
    public static class SplashNotification {
        public long timestamp;
        
        public String hub;
        public String serverID;
        public boolean isPrivate;
        public boolean dungeonHub;
        
        public String splasher;
        public boolean splasherRealIGN;
        
        public String partyHost;
        
        public ArrayList<String> note;
        
        public String location;
        public String splash;
        public boolean remove;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class PlayerCount {
        public String splashID;
        public int playerCount;
        public String hub;
        public String serverID;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class PlayerCountBroadcast {
        public int playerCount;
        public String serverID;
    }
    
    // Request the lbin of any item on ah/bz by item id
    // If they don't exist, they won't be included in the response
    @NoArgsConstructor @AllArgsConstructor
    public static class ClientRequestLowestBINPrices {
        public ArrayList<String> items;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ServerSendLowestBINPrices {
        public HashMap<String, Integer> lbinMap;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ClientSendCHItems {
        public ArrayList<CHChestItem> items = new ArrayList<>();
        public int x;
        public int y;
        public int z;
        public String server;
        public int day;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class CHChestItem {
        public String name;
        public String count;
        public Integer numberColor;
        public Integer itemColor;
    }
    
    // TODO: combine with warp register into one packet sent every time you join a lobby
    @NoArgsConstructor @AllArgsConstructor
    public static class SubscribeToCHServer {
        public String server;
        public int day;
        public boolean unsubscribe;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ServerSendCHItems {
        public ArrayList<ChestInfo> chestMap = new ArrayList<>();
        public String server; // used to confirm that the server is correct
        public int day; // server's last known day
        public Long lastReceivedDayInfo = Long.MAX_VALUE;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ChestInfo {
        public int x;
        public int y;
        public int z;
        public ArrayList<CHChestItem> items = new ArrayList<>();
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class RequestWarpToServer {
        public String server;
        public String serverType; // Crystal Hollows, Dwarven Mines, etc.
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class BackgroundWarpTask {
        public String server; // confirm
        public HashMap<String, String> accountsToWarp = new HashMap<>();
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class RegisterToWarpServer {
        public String server;
        public boolean unregister = false;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class DoneWithWarpTask {
        public boolean successful = true;
        public ArrayList<String> ignsWarped = new ArrayList<>();
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class CancelWarpRequest {
        public String server;
    }
    
    // tell a warper to abort a warp
    // client can also send it to the server to indicate it cannot perform a warp to the server at all
    @NoArgsConstructor @AllArgsConstructor
    public static class AbortWarpTask {
        public EncryptedString ign;
        public boolean ineligible;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class QueuePosition {
        public int positionInWarpQueue;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ServersSummary {
        public HashMap<String, ServerSummary> serverInfo = new HashMap<>();
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class UpdateServers {
        public HashMap<String, Long> serversAndLastUpdatedTime = new HashMap<>();
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class RequestLiveUpdatesForServerInfo {
        public boolean unrequest;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class WarningBannerInfo {
        public EncryptedString text;
        public Integer textColor = 0xFFFFFF;
        public Integer backgroundColor = 0x000000;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class ClientReceiveServerConstantValues {
        public HashMap<String, Object> constants = new HashMap<>();
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class JoinAlert {
        public String joinAlertChat;
        public String joinAlertTitle;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class WarperInfo {
        public EncryptedString ign;
    }
    
    @NoArgsConstructor @AllArgsConstructor
    public static class RequestQueuePosition {
        public String server;
    }
}