package com.github.indigopolecat.bingobrewers.network;

import com.esotericsoftware.kryonet.Connection;
import com.github.indigopolecat.bingobrewers.*;
import com.github.indigopolecat.bingobrewers.gui.SplashInfoHud;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import com.github.indigopolecat.kryo.KryoNetwork.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.client.Minecraft;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.github.indigopolecat.bingobrewers.BingoBrewers.version;
import static com.github.indigopolecat.bingobrewers.network.ServerConnection.*;

public class PacketProcessing {
    private static ServerConnection CLIENT_INSTANCE;

    public static void processPacket(Connection connection, Object packet) {
        if (packet instanceof ServerPublicKey) {
            ServerPublicKey serverPublicKey = (ServerPublicKey) packet;
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
                //joinTitle = new TitleHud("Server Public Key Outdated", 0xFF5555, 10000, true);
                joinChat = "\n§a§kmm §rA Bingo Brewers update is required due to outdated encryption keys. §kmm\n";
                getClient().close();
                getClient().removeListener(CLIENT_INSTANCE);
                reconnect = true; // by setting this to true, the client will assume it is already reconnecting and won't try to
                return;
            }

        } else if (packet instanceof Authentication) {
            Authentication authentication = (Authentication) packet;
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
                e.printStackTrace();
                return;
            }

            sendTCP(authentication);

            ign = mc.getUser().getName();
            uuid = mc.getUser().getProfileId().toString();

            ConnectionIGN accountInfo = new ConnectionIGN();
            accountInfo.IGN = encryptString(ign);
            accountInfo.uuid = encryptString(uuid);
            accountInfo.version = encryptString("v0.4");
            accountInfo.connections = connectionsThisSession;

            System.out.println("Sending " + ign + "|" + version + "|" + uuid);
            sendTCP(accountInfo);
            System.out.println("sent");


            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            PlayerInfo.subscribedToCurrentCHServer = false;
            BingoBrewersConfig.SubscribeToServer();


        } else if (packet instanceof SplashNotification) {
            LoggerUtil.LOGGER.info("Received splash notification");

            SplashNotification notif = (SplashNotification) packet;
            if (notif.hub.isEmpty()) return; // completely ignore splashes without a hub number
            System.out.println("Received Splash Notification: " + notif.hub);

            // update the active splashes list if the message is edited
            for (int i = 0; i < SplashInfoHud.activeSplashes.size(); i++) {
                SplashNotificationInfo splashNotificationInfo = SplashInfoHud.activeSplashes.get(i);
                if (splashNotificationInfo.id.equals(notif.splash)) {

                    if (notif.remove) {
                        // remove the notification if the server has parsed the splasher signaling the end of a splash (e.g. "done" at the end of the message)
                        SplashInfoHud.activeSplashes.remove(i);
                        return;
                    }

                    SplashNotificationInfo updatedSplash;

                    if (splashNotificationInfo.hubNumber.equals(notif.hub) && splashNotificationInfo.dungeonHub == notif.dungeonHub) {
                        updatedSplash = new SplashNotificationInfo(notif, false, splashNotificationInfo);
                        updatedSplash.lobbyPlayerCount = splashNotificationInfo.lobbyPlayerCount; // copy over the player count since the hub hasn't changed
                        // add the updated splash in the original location in the arraylist
                    } else {
                        updatedSplash = new SplashNotificationInfo(notif, true, splashNotificationInfo); // if the hub or dungeon hub status changed, resend the notification
                    }

                    SplashInfoHud.activeSplashes.set(i, updatedSplash);
                    System.out.println("Updated Splash Notification: " + updatedSplash.hubNumber);

                    return;
                }
            }

            System.out.println("time to remove: " + (notif.timestamp  + 120_000) + " current time: " + System.currentTimeMillis());
            if (notif.timestamp + 120_000 < System.currentTimeMillis()) return; // if we received it outdated

            SplashInfoHud.activeSplashes.add(new SplashNotificationInfo(notif, true, null));
            System.out.println("Added Splash Notification: " + notif.hub);

            System.out.println("Active Splashes: " + SplashInfoHud.activeSplashes.toString() );

        } else if (packet instanceof PlayerCountBroadcast) {
            PlayerCountBroadcast playerCountBroadcast = (PlayerCountBroadcast) packet;

            for (SplashNotificationInfo info : SplashInfoHud.activeSplashes) {
                if (playerCountBroadcast.serverID.equals(info.serverID)) {
                    info.lobbyPlayerCount = String.valueOf(playerCountBroadcast.playerCount);
                }
            }

        } else if (packet instanceof ClientReceiveServerConstantValues) {
            ClientReceiveServerConstantValues request = (ClientReceiveServerConstantValues) packet;
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
                    ChestInventories.rankPriceMap = (HashMap<Integer, Integer>) constants.get("bingoRankCosts");
                }
            }
            if (constants.get("chItemRegex") != null && constants.get("chItemRegex") instanceof String) {
                CHChests.regex = (String) constants.get("chItemRegex");
            }
            if (constants.get("newMiscCHItems") != null && constants.get("newMiscCHItems") instanceof ArrayList) {
                boolean nope = false;
                for (Object string : (ArrayList<Object>) constants.get("newMiscCHItems")) {
                    if (!(string instanceof String)) {
                        nope = true;
                        break;
                    }
                }
                if (!nope) {
                    newMiscCHItems = (ArrayList<String>) constants.get("newMiscCHItems");
                }
            }
            if (constants.get("joinAlert"+ version) != null && constants.get("joinAlert"+ version) instanceof JoinAlert) {
                JoinAlert joinAlert = (JoinAlert) constants.get("joinAlert"+ version);
                if (joinAlert.joinAlertChat != null) {
                    joinChat = joinAlert.joinAlertChat;
                }
                if (joinAlert.joinAlertTitle != null) {
                    //joinTitle = new TitleHud(joinAlert.joinAlertTitle, 0xFF5555, 10000, true);
                }
            }

            if (constants.get("CHItemOrder") != null && constants.get("CHItemOrder") instanceof LinkedHashSet) {
                boolean nope = false;
                for (Object string : (LinkedHashSet<Object>) constants.get("CHItemOrder")) {
                    if (!(string instanceof String)) {
                        nope = true;
                        break;
                    }
                }
                if (!nope) {
                    CHItemOrder = new ArrayList<>((LinkedHashSet<String>) constants.get("CHItemOrder"));
                }
            }

            if (constants.get("itemNameRegexGroup") != null && constants.get("itemNameRegexGroup") instanceof Integer) {
                if (constants.get("itemCountRegexGroup") != null && constants.get("itemCountRegexGroup") instanceof Integer)
                    if (constants.get("itemNameColorRegexGroup") != null && constants.get("itemNameColorRegexGroup") instanceof Integer) {
                        CHChests.itemCountRegexGroup = (Integer) constants.get("itemCountRegexGroup");
                        CHChests.itemNameRegexGroup = (Integer) constants.get("itemNameRegexGroup");
                        CHChests.itemNameColorRegexGroup = (Integer) constants.get("itemNameColorRegexGroup");
                    }
            }

            if (constants.get("signalLootChatMessage") != null && constants.get("signalLootChatMessage") instanceof String) {
                CHChests.signalLootChatMessage = (String) constants.get("signalLootChatMessage");
            }

            if (constants.get("signalLootChatMessageEnd") != null && constants.get("signalLootChatMessageEnd") instanceof String) {
                CHChests.signalLootChatMessageEnd = (String) constants.get("signalLootChatMessageEnd");
            }


        } else if (packet instanceof ServerSendCHItems) {
            ServerSendCHItems CHItems = (ServerSendCHItems) packet;
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
                    //BingoBrewersConfig.filterJasperGemstones();
                    BingoBrewersConfig.filterRobotParts();
                    BingoBrewersConfig.filterPrehistoricEggs();
                    BingoBrewersConfig.filterPickonimbus();
                    BingoBrewersConfig.filterMisc();
                    organizeWaypoints();

                }
            }
        }
    }

    public static void setClientInstance(ServerConnection instance) {CLIENT_INSTANCE = instance;}
}
