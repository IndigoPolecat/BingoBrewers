package com.github.indigopolecat.bingobrewers;

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
import com.github.indigopolecat.kryo.KryoNetwork.SplashNotification;
import com.github.indigopolecat.kryo.ServerSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.esotericsoftware.minlog.Log.*;
import static com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud.filteredItems;
import static com.github.indigopolecat.bingobrewers.PacketProcessing.*;
import static java.lang.String.join;
import static java.lang.String.valueOf;

public class ServerConnection extends Listener implements Runnable {

    // The server sends it's public key to the client, which checks it based on this. If they don't match the connection is refused.
    // This would require all users to update in the event of the key being changed.
    public static final String SERVER_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqgPvRC780jqwtXV4/39jjZvlXSnXRGEpD63y3Iptq8YO9sZic7Qno+vHKeoW50Ct5XWmNk13JjUwUdXmWBN4186FUo/b0Z+AtpLNVrkvk7dwkJQgAHa56fok52NK9QN8mTy+Saw1flmX4rdz7TflXpOwPzIYMYC33gqWe4/hMniuU7m+D/07fgzu5Ua5yFz27sNwrbqNuJOr1ReDScLykIazILHzfTa7RFAZn+4nWM3vdtdysKo1YSYQ++05uMR1S51ABtPkJdNLKzEf0sC6H2q1JPOcIAz/9EX2doWHROTfWoYifi0HDHEu+c0Cc20SfhfmY5NjofmLEc0XmuyqewIDAQAB";

    public static int waitTime = 0;
    public static boolean reconnect; // controls the loop for reconnecting the client
    public static long originalTime = -1;
    public static CopyOnWriteArrayList<CHWaypoints> waypoints = new CopyOnWriteArrayList<>();
    // if new ch items are added, they will be in this list
    public static ArrayList<String> newMiscCHItems = new ArrayList<>();
    public static TitleHud joinTitle;
    public static String joinChat;
    public static ArrayList<String> CHItemOrder = new ArrayList<>();
    public static ConcurrentHashMap<String, ServerSummary> serverSummaries = new ConcurrentHashMap<>();
    public static String ign = "";
    public static String uuid = "";
    private static SecretKey symmetricKey;

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
            System.out.println("catch reconnect");
            LoggerUtil.LOGGER.info("Server Connection Error: " + e.getMessage());
            if (!reconnect) {
                reconnect();
            }
        }

    }

    private void connection() throws Exception {
        Log.set(LEVEL_ERROR);
        KryoNetwork.register(BingoBrewers.client);

        BingoBrewers.client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                processPacket(connection, object);
            }


            @Override
            public void disconnected(Connection connection) {
                System.out.println("disconnected");
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
    }

    public static void setSplashHudItems() {
        SplashHud.keyOrder.clear(); // clear the list so it doesn't keep adding the same keys every time you reconnect
        SplashHud.keyOrder.add(SplashHud.HUB);
        if (BingoBrewersConfig.showSplasher) SplashHud.keyOrder.add(SplashHud.SPLASHER);
        if (BingoBrewersConfig.showParty) SplashHud.keyOrder.add(SplashHud.PARTY);
        if (BingoBrewersConfig.showLocation) SplashHud.keyOrder.add(SplashHud.LOCATION);
        if (BingoBrewersConfig.showNote) SplashHud.keyOrder.add(SplashHud.NOTE);
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

    public static synchronized void setActiveHud(TitleHud activeTitle) {
        BingoBrewers.activeTitle = activeTitle;
    }

    public static SecretKey getSymmetricKey() {return symmetricKey;}
    public static void setSymmetricKey(SecretKey key) {symmetricKey = key;}

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
            hubInfo.add(SplashHud.HUB);
            SplashHud.hubList.add(hub);
        } else {
            hubInfo.add(SplashHud.DUNGEON_HUB);
            // Identify a hub as a dungeonhub to avoid mixing up regular hubs and dhubs
            SplashHud.hubList.add("DH" + hub);
        }
        hubInfo.add(": " + hub);
        splashInfo.put(SplashHud.HUB, hubInfo);

        ArrayList<String> splasherInfo = new ArrayList<>();
        splasherInfo.add(SplashHud.SPLASHER);
        splasherInfo.add(": " + splasher);
        splashInfo.put(SplashHud.SPLASHER, splasherInfo);

        ArrayList<String> partyInfo = new ArrayList<>();
        partyInfo.add("Bingo Party");
        partyInfo.add(": " + partyHost);
        splashInfo.put(SplashHud.PARTY, partyInfo);

        ArrayList<String> locationInfo = new ArrayList<>();
        locationInfo.add(SplashHud.LOCATION);
        locationInfo.add(": " + location);
        splashInfo.put(SplashHud.LOCATION, locationInfo);

        ArrayList<String> noteInfo = new ArrayList<>();
        noteInfo.add(SplashHud.NOTE);
        if (note == null || note.isEmpty()) {
            noteInfo.add(": No Note");
        } else {
            noteInfo.add(": ");
            noteInfo.addAll(note);
        }
        splashInfo.put(SplashHud.NOTE, noteInfo);

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

        SplashHud.mapList.add(splashInfo);
        if (sendNotif) {
            PlayerInfo.setReadyToNotify(hub, notif.dungeonHub);
        }
    }

    // This is called onTickEvent in PlayerInfo when the player is not null
    public synchronized void notification(String hub, boolean dungeonHub) {
        if (!BingoBrewersConfig.splashNotificationsEnabled) return;
        if(!SplashHud.onBingo) return; // non-profile bingo splashes setting was here
        if(!SplashHud.inSkyblockorPTLobbyorLimbo && !BingoBrewersConfig.splashNotificationsOutsideSkyblock) return;
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

    public static synchronized void sendTCP(Object packet) {
        Client client = getClient();

        if (client == null) {
            LoggerUtil.LOGGER.info("Client is null");
            return;
        }
        client.sendTCP(packet);
    }




    public void reconnect() {
        BingoBrewers.client.close();
        BingoBrewers.client.removeListener(this);
        if (waitTime == 0) {
            waitTime = (int) (5000 * Math.random() + 1000);
        }
        System.out.println("Disconnected from server.");
        reconnect = true;
        while (reconnect) {
            System.out.println("Reconnecting");
            try {
                BingoBrewers.client = new Client(16384, 16384);
                connection();
                reconnect = false;
            } catch (Exception e) {
                e.printStackTrace();
                LoggerUtil.LOGGER.info("Server Connection Error: " + e.getMessage());
                BingoBrewers.client.close();
                BingoBrewers.client.removeListener(this);

                System.out.println("Failed to reconnect, retrying in " + waitTime + " milliseconds.");
                try {
                    System.out.println("Sleeping for " + waitTime + "ms");
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

    public static PublicKey loadPublicKeyFromBase64(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = null;
        try {

            keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey generateAESKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize); // keySize can be 128, 192, or 256 bits
        return keyGenerator.generateKey();
    }

    public static String encodeKeyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String encryptObjectPublicKey(SecretKey obj, PublicKey publicKey) {
        // Get the byte array of the serialized object
        byte[] objectBytes = obj.getEncoded();

        // Encrypt the byte array using RSA
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(objectBytes);

            // Encode the encrypted bytes to a Base64 string
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateIV() {
        byte[] iv = new byte[16]; // AES block size is 16 bytes
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    public static EncryptedString encryptString(String obj) {
        byte[] iv = generateIV();
        SecretKey aesKey = symmetricKey;

        // Get the byte array of the string object
        byte[] objectBytes = obj.getBytes();

        Cipher cipher = null;
        try {

            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(objectBytes);
            EncryptedString encryptedString = new EncryptedString();
            encryptedString.string = Base64.getEncoder().encodeToString(encryptedBytes);
            encryptedString.iv = iv;
            return encryptedString;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptString(EncryptedString encryptedString) {
        SecretKey aesKey = symmetricKey;

        try {

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(encryptedString.iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
            String encryptedData = encryptedString.string;
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }


}
