package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.github.indigopolecat.bingobrewers.hud.SplashHud;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.*;
import com.github.indigopolecat.kryo.ServerSummary;
import com.mojang.authlib.exceptions.AuthenticationException;
import lombok.*;
import net.minecraft.client.Minecraft;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;

public class ServerConnection extends Listener implements Runnable {
    // The server sends it's public key to the client, which checks it based on this. If they don't match the connection is refused.
    // This would require all users to update in the event of the key being changed but simpler than CA.
    public static final String SERVER_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqgPvRC780jqwtXV4/39jjZvlXSnXRGEpD63y3Iptq8YO9sZic7Qno+vHKeoW50Ct5XWmNk13JjUwUdXmWBN4186FUo/b0Z+AtpLNVrkvk7dwkJQgAHa56fok52NK9QN8mTy+Saw1flmX4rdz7TflXpOwPzIYMYC33gqWe4/hMniuU7m+D/07fgzu5Ua5yFz27sNwrbqNuJOr1ReDScLykIazILHzfTa7RFAZn+4nWM3vdtdysKo1YSYQ++05uMR1S51ABtPkJdNLKzEf0sC6H2q1JPOcIAz/9EX2doWHROTfWoYifi0HDHEu+c0Cc20SfhfmY5NjofmLEc0XmuyqewIDAQAB";
    public static int waitTime = 0;
    public static boolean reconnect; // controls the loop for reconnecting the client
    public static int connectionsThisSession = 0; // easy visual indicator server side if connections are struggling
    public static CopyOnWriteArrayList<CHWaypoints> waypoints = new CopyOnWriteArrayList<>();
    // if new ch items are added, they will be in this list
    public static ArrayList<String> newMiscCHItems = new ArrayList<>();
    public static String joinChat;
    public static ArrayList<String> CHItemOrder = new ArrayList<>();
    public static ConcurrentHashMap<String, ServerSummary> serverSummaries = new ConcurrentHashMap<>();
    public static String ign = "";
    public static String uuid = "";
    @Getter(onMethod_ = @Synchronized)
    @Setter(onMethod_ = @Synchronized)
    private static SecretKey symmetricKey;
    public static ConcurrentLinkedDeque<CrystalHollowsItemTotal> filteredItems = new ConcurrentLinkedDeque<>();

    @Override
    public void run() {
        Client client = new Client(16384, 16384);
        BingoBrewers.setClient(client);
        if (BingoBrewers.getClient() == null) {
            Log.warn("Client is null");
        }
        try {
            connection();
        } catch (Exception e) {
            Log.info("catch reconnect");
            Log.error("Server Connection Error: " + e.getMessage(), e);
            if (!reconnect) {
                reconnect();
            }
        }
    }

    public void processPacket(Connection connection, Object packet) {
        System.out.println("abcdefg" + packet);
        if (packet == null) return; //Never process null packets
        if (packet instanceof ServerPublicKey serverPublicKey) {
            String public_key = serverPublicKey.public_key;
            SecretKey symmetricKey;

            if (public_key.equals(SERVER_PUBLIC_KEY)) {
                try {
                    symmetricKey = generateAESKey(256);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                setSymmetricKey(symmetricKey);

                ClientSymmetricKey key = new ClientSymmetricKey();
                key.symmetric_key = encryptObjectPublicKey(symmetricKey, loadPublicKeyFromBase64(public_key));
                sendTCP(key);
            } else {
                //joinTitle = new TitleHud("Server Public Key Outdated", 0xFF5555, 10000, true);                    //TODO: mhhh, will need to check https://github.com/IndigoPolecat/BingoBrewers/blob/0.4dev/src/main/java/com/github/indigopolecat/bingobrewers/ServerConnection.java
                joinChat = "\n§a§kmm §rA Bingo Brewers update is required due to outdated encryption keys. §kmm\n"; //TODO: add this to the chat/show a toast/do something
                BingoBrewers.getClient().close();
                BingoBrewers.getClient().removeListener(this);
                reconnect = true; // by setting this to true, the client will assume it is already reconnecting and won't try to
            }

        } else if (packet instanceof Authentication authentication) {
            String serverAuthID = decryptString(authentication.AuthID);

            String clientAuthID = UUID.randomUUID().toString().replaceAll("-", "");
            authentication.AuthID = encryptString(clientAuthID);

            Minecraft mc = Minecraft.getInstance();
            try {
                String serverHash = serverAuthID.substring(0, serverAuthID.length() / 2 - 1) + clientAuthID.substring(clientAuthID.length() / 2);
                // This is sending your session info to Mojang's servers as if you were joining a server,
                // this is used on the Bingo Brewers server to authenticate your IGN like an MC server normally would when you join.
                mc.services().sessionService().joinServer(
                        mc.getUser().getProfileId(), // Gets the modern UUID
                        mc.getUser().getAccessToken(), // Gets the session token
                        serverHash
                );
            } catch (AuthenticationException e) {
                Log.warn("An error occurred while authenticating with the bingobrewers server via mojang auth", e);
                return;
            }

            sendTCP(authentication);

            ign = mc.getUser().getName();
            uuid = mc.getUser().getProfileId().toString();

            ConnectionIGN accountInfo = new ConnectionIGN();
            accountInfo.IGN = encryptString(ign);
            accountInfo.uuid = encryptString(uuid);
            accountInfo.version = encryptString(BingoBrewers.version);
            accountInfo.connections = connectionsThisSession;

            System.out.println("Sending " + ign + "|" + BingoBrewers.version + "|" + uuid);
            sendTCP(accountInfo);
            System.out.println("sent");

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            PlayerInfo.subscribedToCurrentCHServer = false;
            BingoBrewersConfig.getConfig().subscribeToServer();

        } else if (packet instanceof SplashNotification notif) {
            if (notif.hub.isEmpty()) return; // completely ignore splashes without a hub number
            Log.info("Received Splash Notification: " + notif.hub + ", splasher: " + notif.splasher);
            Log.info("notif=" + notif);// Log all fields just in case

            if (notif.remove) {
                SplashHud.removeSplash(notif.splash);
                return;
            }

            if (notif.timestamp + BingoBrewersConfig.getConfig().splashConfig.displayTime < System.currentTimeMillis())
                return; // Skip outdated splashes
            try {
                SplashHud.addSplash(notif);
            } catch (IllegalArgumentException ignored) {
                Log.info("Tried to add to render queue invalid splash (id=" + notif.splash + ")");
            }

        } else if (packet instanceof PlayerCountBroadcast playerCountBroadcast) {
            for (SplashNotificationInfo info : SplashHud.splashes.values()) {
                if (playerCountBroadcast.serverID.equals(info.serverID)) {
                    info.lobbyPlayerCount = String.valueOf(playerCountBroadcast.playerCount);
                }
            }

        } else if (packet instanceof ClientReceiveServerConstantValues request) {
            HashMap<String, Object> constants = request.constants;
            if (constants.get("bingoRankCosts") != null && constants.get("bingoRankCosts") instanceof HashMap) {
                boolean nope = false;
                for (Map.Entry<?, ?> entry : ((HashMap<?, ?>) constants.get("bingoRankCosts")).entrySet()) {
                    if (!(entry.getValue() instanceof Integer) || !(entry.getKey() instanceof Integer)) {
                        nope = true;
                        break;
                    }
                }
                if (!nope) {
                    //noinspection unchecked
                    ChestInventories.rankPriceMap = (HashMap<Integer, Integer>) constants.get("bingoRankCosts");
                }
            }
//            if (constants.get("chItemRegex") != null && constants.get("chItemRegex") instanceof String) {
//                CHChests.regex = (String) constants.get("chItemRegex");
//            }
            //noinspection rawtypes
            if (constants.get("newMiscCHItems") != null && constants.get("newMiscCHItems") instanceof ArrayList list) {
                boolean nope = false;
                for (Object string : list) {
                    if (!(string instanceof String)) {
                        nope = true;
                        break;
                    }
                }
                if (!nope) {
                    //noinspection unchecked
                    newMiscCHItems = (ArrayList<String>) constants.get("newMiscCHItems");
                }
            }
            if (constants.get("joinAlert" + BingoBrewers.version) != null && constants.get("joinAlert" + BingoBrewers.version) instanceof JoinAlert joinAlert) {
                if (joinAlert.joinAlertChat != null) {
                    joinChat = joinAlert.joinAlertChat;
                }
                if (joinAlert.joinAlertTitle != null) {
                    //joinTitle = new TitleHud(joinAlert.joinAlertTitle, 0xFF5555, 10000, true);
                }
            }

            if (constants.get("CHItemOrder") != null && constants.get("CHItemOrder") instanceof LinkedHashSet<?> lhs) {
                boolean nope = false;
                for (Object string : lhs) {
                    if (!(string instanceof String)) {
                        nope = true;
                        break;
                    }
                }
                if (!nope) {
                    //noinspection unchecked
                    CHItemOrder = new ArrayList<>((LinkedHashSet<String>) constants.get("CHItemOrder"));
                }
            }

//            if (constants.get("itemNameRegexGroup") != null && constants.get("itemNameRegexGroup") instanceof Integer) {
//                if (constants.get("itemCountRegexGroup") != null && constants.get("itemCountRegexGroup") instanceof Integer)
//                    if (constants.get("itemNameColorRegexGroup") != null && constants.get("itemNameColorRegexGroup") instanceof Integer) {
//                        CHChests.itemCountRegexGroup = (Integer) constants.get("itemCountRegexGroup");
//                        CHChests.itemNameRegexGroup = (Integer) constants.get("itemNameRegexGroup");
//                        CHChests.itemNameColorRegexGroup = (Integer) constants.get("itemNameColorRegexGroup");
//                    }
//            }
//
//            if (constants.get("signalLootChatMessage") != null && constants.get("signalLootChatMessage") instanceof String) {
//                CHChests.signalLootChatMessage = (String) constants.get("signalLootChatMessage");
//            }
//
//            if (constants.get("signalLootChatMessageEnd") != null && constants.get("signalLootChatMessageEnd") instanceof String) {
//                CHChests.signalLootChatMessageEnd = (String) constants.get("signalLootChatMessageEnd");
//            }
        } else if (packet instanceof ServerSendCHItems CHItems) {
            System.out.println("Received CH Chests for " + CHItems.server);
            ArrayList<ChestInfo> chests = CHItems.chestMap;
            if (CHItems.server.equals(PlayerInfo.currentServer)) {
                if (CHItems.day - 1 > PlayerInfo.day || System.currentTimeMillis() - (CHItems.lastReceivedDayInfo != null ? CHItems.lastReceivedDayInfo : Long.MAX_VALUE) > 25_200_000)
                    return; // ignore if the server is younger than last known, or it's been more than 7 hours since info was received
                for (ChestInfo chest : chests) {
                    CHWaypoints chWaypoints = new CHWaypoints(chest.x, chest.y, chest.z, chest.items);
                    waypoints.add(chWaypoints);

                    for (CHChestItem item : chest.items) {
                        CrystalHollowsItemTotal.sumItems(item);
                    }

                    for (CHWaypoints waypoint : CHWaypoints.filteredWaypoints) {
                        waypoint.filteredExpandedItems.clear();
                    }

//                    BingoBrewersConfig.filterPowder();
//                    BingoBrewersConfig.filterGoblinEggs();
//                    BingoBrewersConfig.filterRoughGemstones();
//                    BingoBrewersConfig.filterJasperGemstones();
//                    BingoBrewersConfig.filterRobotParts();
//                    BingoBrewersConfig.filterPrehistoricEggs();
//                    BingoBrewersConfig.filterPickonimbus();
//                    BingoBrewersConfig.filterMisc();
                    organizeWaypoints();
                }
            }
        }
    }

    private void connection() throws Exception {
        com.esotericsoftware.minlog.Log.set(LEVEL_ERROR);
        KryoNetwork.register(BingoBrewers.getClient());
        BingoBrewers.getClient().addListener(new Listener() {
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

        BingoBrewers.getClient().start();
        System.out.println("Client started, Test Instance: " + BingoBrewersConfig.getConfig().testInstance);

        connectionsThisSession++;

        if (BingoBrewersConfig.getConfig().testInstance) {
            // Note: for those compiling their own version, the test server will rarely be active so keep the boolean as false
            System.out.println("Connecting to test server");
            BingoBrewers.getClient().connect(8000, "bingobrewers.com", 9090, 9191);
        } else {
            BingoBrewers.getClient().connect(8000, "bingobrewers.com", 8080, 7070);
        }
        System.out.println("Connected to server.");
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

    public synchronized void sendPlayerCount(KryoNetwork.PlayerCount count) {
        if (!BingoBrewersConfig.getConfig().splashNotificationsEnabled) return;
        sendTCP(count);
    }

    public static synchronized void SubscribeToCHServer(SubscribeToCHServer server) {
        sendTCP(server);
        if (!server.unsubscribe) {
            ServerConnection.waypoints.clear();
            CHWaypoints.filteredWaypoints.clear();
            //CrystalHollowsHud.filteredItems.clear();
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
        sendTCP(request);
    }

    public static synchronized void sendTCP(Object packet) {
        Client client = BingoBrewers.getClient();

        if (client == null) {
            Log.info("Client is null");
            return;
        }
        client.sendTCP(packet);
    }

    public void reconnect() {
        BingoBrewers.getClient().close();
        //Matita: this seems wrong, the listener is declared in an anonymos class inside the connect method
        BingoBrewers.getClient().removeListener(this);
        float waitTime = 0;

        waitTime = (int) (5000 * Math.random() + 2000);

        Log.warn("Disconnected from server.");
        reconnect = true;
        while (reconnect) {
            Log.info("Reconnecting to Bingo Brewers server.");
            try {
                BingoBrewers.setClient(new Client(16384, 16384));
                //Matita: this seems to always throw an exception:
                /*
                Note: Client#update must be called in a separate thread during connect.
                java.net.SocketTimeoutException: Connected, but timed out during TCP registration.
                 */
                connection(); // there's a built in reconnect method idk that's not used but this works
                reconnect = false;
            } catch (Exception e) {
                Log.info("[Bingo Brewers] Server Connection Error: " + e.getMessage(), e);
                BingoBrewers.getClient().close();
                BingoBrewers.getClient().removeListener(this);

                try {
                    Log.error("Reconnect failed. Trying again in " + waitTime + " milliseconds.");
                    Thread.sleep((int) waitTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                waitTime = Math.min(waitTime * 1.5F, 60000);
                if (waitTime == 60000) {
                    waitTime = 60000 - (int) (5000 * Math.random() + 1000); // slightly vary time
                }
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
