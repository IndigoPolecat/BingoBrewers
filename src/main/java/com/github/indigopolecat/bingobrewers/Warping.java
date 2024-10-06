package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.MessageMatcher;
import com.github.indigopolecat.events.PacketEvent;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.indigopolecat.bingobrewers.ServerConnection.encryptString;

public class Warping {
    public static volatile ConcurrentHashMap<String, String> accountsToWarp = new ConcurrentHashMap<>();
    public static volatile ConcurrentHashMap<String, String> accountsToKick = new ConcurrentHashMap<>();
    public static volatile ConcurrentHashMap<String, String> accountsKicked = new ConcurrentHashMap<>();
    public static String server;
    // when the party invite was sent out
    public static long lastPartyUpdate;
    public static boolean waitingOnLocation;
    public static WARP_PHASE PHASE;
    public static boolean PARTY_EMPTY_KICK = false;
    public static volatile boolean kickParty;
    public static BackgroundWarpThread warpThread;
    public static String requestedWarp = "";

    public static void warp() {
        // todo: come back to this and consider immediately kicking instead of waiting for the chat message
        System.out.println("warping");
        sendChatMessage("/p warp");
        timeOfInvite = 0; // reset
        warpThread.warpTime = Long.MAX_VALUE;

        PHASE = WARP_PHASE.VERIFICATION;
    }

    public static void abort(boolean ineligible) {
        System.out.println("aborting warp");
        kickParty = true;
        BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());


        KryoNetwork.AbortWarpTask abort = new KryoNetwork.AbortWarpTask();
        abort.ign = encryptString(ServerConnection.ign);
        abort.ineligible = ineligible;
        ServerConnection.sendTCP(abort);
        accountsToWarp.clear();
        waitingOnLocation = true;

        if (warpThread != null) {
            warpThread.stop = true;
            warpThread.notify();
        }
    }

    public static void resetWarpThread() {
        warpThread = null;
        kickParty = false;
        accountsToWarp.clear();
        accountsToKick.clear();
        accountsKicked.clear();
        PHASE = null;
        PARTY_EMPTY_KICK = false;
    }

    public static long lastMessageSent;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) {

            if (warpThread != null && System.currentTimeMillis() - warpThread.executionTimeBegan > 15000) {
                System.out.println("15s disband");
                sendChatMessage("/p disband");
                System.out.println("ending");
                if (warpThread != null) {
                    warpThread.stop = true;
                    warpThread.resume();
                    System.out.println("thread ended");
                } else {
                    System.out.println ("warp thread is already null");
                }
                PHASE = null;
            }

            if (!messageQueue.isEmpty() && System.currentTimeMillis() - lastMessageSent > 100 && Minecraft.getMinecraft().thePlayer != null) {
                for (String message : messageQueue) {
                    if (message.startsWith("/p") && warpThread != null && !whitelistedMessages.contains(message)) continue;

                    Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
                    messageQueue.remove(message);
                    break;
                }
            }
        }
    }

    public static ArrayList<String> chatMessageHold = new ArrayList<>();
    public static boolean lookForEndingMessage = false;
    public static String warpMessageTrigger = "§9§m-----------------------------§r";
    public static Pattern playerWarpPattern = Pattern.compile("([✮✔✖])\\s+(\\[\\w+\\+*])?\\s*([\\w_]+)\\s+([\\w\\s]+)");
    public static String warpingTooFastError = "Please wait 5 seconds between SkyBlock warps!"; // this is currently not translated
    public static Pattern partyInvitePattern = Pattern.compile("-----------------------------------------------------\n(\\[\\w+\\+*])?\\s*([\\w_]+) has invited you to join their party!\nYou have 60 seconds to accept. Click here to join!\n-----------------------------------------------------");
    public static ArrayList<String> partyInvites = new ArrayList<>(); // this variable is used for whichever comes first, BB server packet with warper info, or HP party invite
    public static String warperIGN;
    public static long timeOfInvite = 0; // this variable is used for whichever comes first, BB server packet with warper info, or HP party invite

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (requestedWarp.isEmpty() || event.type == 2) return;

        String message = event.message.getUnformattedText();

        HashMap<String, String> partyMessageGroups = new HashMap<>();
        Pattern accountPattern = Pattern.compile("(§.)?(\\[\\w+\\+*])?\\s*([\\w_]+)");
        System.out.println(message);
        System.out.println(INVITED_TO_PARTY.match(message, partyMessageGroups));

        if (INVITED_TO_PARTY.match(message, partyMessageGroups)) {

            Matcher accountMatcher = accountPattern.matcher(partyMessageGroups.get("0"));
            String ign = "";
            if (accountMatcher.find()) {
                ign = accountMatcher.group(3);
            }
            System.out.println("ign = " + ign);
            partyInvites.add(ign);

            if (partyInvites.contains(warperIGN) && System.currentTimeMillis() - timeOfInvite < 5000 ) {
                System.out.println("joining");
                // we have already received the packet telling us the ign and we can safely join
                sendChatMessage("/p accept " + warperIGN);
                partyInvites = new ArrayList<>();
                warperIGN = null;
                timeOfInvite = 0;
            } else if (System.currentTimeMillis() - timeOfInvite > 5000) {
                warperIGN = null;
                partyInvites = new ArrayList<>();
                timeOfInvite = 0;
            } else {
                timeOfInvite = System.currentTimeMillis();
            }

        }
    }

    // Everything up to the packet receiving function is adapted code from the Dungeon Guide Mod, including the contents of dungeon_guide_party_languages.json


    private static MessageMatcher NOT_IN_PARTY;
    private static MessageMatcher PARTY_CHANNEL;
    private static MessageMatcher TRANSFER_LEFT;
    private static MessageMatcher ALL_INVITE_ON;
    private static MessageMatcher ALL_INVITE_OFF;
    private static MessageMatcher PARTY_JOIN;
    private static MessageMatcher PARTY_LEAVE;
    private static MessageMatcher INVITED;
    private static MessageMatcher INVITE_PERM;
    private static MessageMatcher TRANSFER;
    private static MessageMatcher PROMOTE_LEADER;
    private static MessageMatcher PROMOTE_MODERATOR;
    private static MessageMatcher MEMBER;
    private static MessageMatcher ACCEPT_INVITE_LEADER;
    private static MessageMatcher ACCEPT_INVITE_MEMBERS;
    private static MessageMatcher INVITED_TO_PARTY;

    public static void createPartyMessageMatchers() {
        try {
            JsonObject jsonObject = new Gson().fromJson(IOUtils.toString(Objects.requireNonNull(BingoBrewers.class.getResourceAsStream("/assets/bingobrewers/dungeon_guide_party_languages.json")), StandardCharsets.UTF_8), JsonObject.class);
            NOT_IN_PARTY = createMatcher(jsonObject, "not_in_party");
            PARTY_CHANNEL = createMatcher(jsonObject, "party_channel");
            ALL_INVITE_ON = createMatcher(jsonObject, "all_invite_on");
            ALL_INVITE_OFF = createMatcher(jsonObject, "all_invite_off");
            PARTY_JOIN = createMatcher(jsonObject, "party_join");
            PARTY_LEAVE = createMatcher(jsonObject, "party_leave");
            INVITED = createMatcher(jsonObject, "invited");
            INVITE_PERM = createMatcher(jsonObject, "invite_perm");
            TRANSFER = createMatcher(jsonObject, "transfer");
            TRANSFER_LEFT = createMatcher(jsonObject, "transfer_left");
            PROMOTE_LEADER = createMatcher(jsonObject, "promote_leader");
            PROMOTE_MODERATOR = createMatcher(jsonObject, "promote_moderator");
            MEMBER = createMatcher(jsonObject, "member");
            ACCEPT_INVITE_LEADER = createMatcher(jsonObject, "accept_invite_leader");
            ACCEPT_INVITE_MEMBERS = createMatcher(jsonObject, "accept_invite_members");
            INVITED_TO_PARTY = createMatcher(jsonObject, "invited_to_party");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageMatcher createMatcher(JsonObject object, String key) {
        JsonArray jsonArray = object.getAsJsonArray(key);
        List<String> list = StreamSupport.stream(jsonArray.spliterator(), false)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
        return new MessageMatcher(list);
    }


    // cancelling these packets is intentional in an effort to ensure other mods can avoid interacting with them since this is not a party they player will likely notice they're in
    @SubscribeEvent
    public void onPacketReceived(PacketEvent.Received event) {
        if (event.getPacket() instanceof S02PacketChat) {
            if (((S02PacketChat) event.getPacket()).getType() == 2) return;
            String message = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
            String unformatted = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();

            if (warpThread == null) return;

            // This message is not currently translated
            if (unformatted.equals(warpingTooFastError) && System.currentTimeMillis() - warpThread.executionTimeBegan > 12000) {
                PHASE = WARP_PHASE.KICK;
                kickParty = true;
                PARTY_EMPTY_KICK = false;
            }

            if (message.equals("§9§m-----------------------------------------------------§r")) {

                //event.setCanceled(true);
                lookForEndingMessage = !lookForEndingMessage;
            } else if (message.equals(warpMessageTrigger)) {
                //event.setCanceled(true);
                lookForEndingMessage = !lookForEndingMessage;

                if (lookForEndingMessage) {


                    warpThread.conclusion.ignsWarped.clear();
                } else {
                    System.out.println(accountsToWarp.toString());
                    warpThread.warpAttempts += 1;

                }
            } else if (lookForEndingMessage) {
                Matcher playerWarpMatcher = playerWarpPattern.matcher(unformatted);
                if (playerWarpMatcher.find()) {
                    System.out.println("group1: " + playerWarpMatcher.group(1));
                    if (playerWarpMatcher.group(1).equalsIgnoreCase("✮") || playerWarpMatcher.group(1).equalsIgnoreCase("✔") || (warpThread != null && warpThread.warpAttempts >= 1)) {
                        for (Entry<String, String> player : accountsToWarp.entrySet()) {
                            if (player.getValue().equals(playerWarpMatcher.group(3))) {
                                System.out.println("removing " + player.getValue() + " from warp accounts");
                                warpThread.conclusion.ignsWarped.add(playerWarpMatcher.group(3));
                                accountsToKick.put(player.getKey(), player.getValue());
                                accountsToWarp.remove(player.getKey());
                            }
                        }
                    } else {
                        if (warpThread != null) {
                            warpThread.conclusion.successful = false;
                            // todo: check for error reason and potentially cancel
                            System.out.println("5250 til warp");
                            warpThread.warpTime = System.currentTimeMillis() + 5250;
                        }
                    }
                }

                HashMap<String, String> partyMessageGroups = new HashMap<>();
                Pattern accountPattern = Pattern.compile("(§.)?(\\[\\w+\\+*])?\\s*([\\w_]+)");

                if (PARTY_JOIN.match(message, partyMessageGroups)) {
                    Matcher accountMatcher = accountPattern.matcher(partyMessageGroups.get("1"));
                    String ign = "";
                    if (accountMatcher.find()) {
                        ign = accountMatcher.group(3);
                    }

                    System.out.println("parsed account: " + ign);
                    System.out.println("accounts to warp: " + accountsToWarp.toString());

                    boolean warpAccount = false;
                    for (Entry<String, String> account : accountsToWarp.entrySet()) {
                        if (Objects.equals(account.getValue(), ign)) {
                            PlayerInfo.partyMembers.add(account.getKey());
                            warpAccount = true;
                        }
                    }
                    if (PlayerInfo.partyMembers.containsAll(accountsToWarp.values()) && accountsToWarp.values().containsAll(PlayerInfo.partyMembers)) {
                        // if the party has all members
                        warpThread.warpTime = System.currentTimeMillis();
                        warpThread.chatWarpOverride = true;
                    }
                    if (!warpAccount) {
                        // an account that isn't supposed to be in the party has joined
                        System.out.println("Aborting because an unknown account has joined");
                        abort(true);
                    }

                } else if (INVITED.match(message, partyMessageGroups)) {
                    Matcher accountMatcher = accountPattern.matcher(partyMessageGroups.get("1"));
                    String ign = "";
                    if (accountMatcher.find()) {
                        ign = accountMatcher.group(3);
                    }

                    System.out.println("accounts to warp: " + accountsToWarp.toString());
                    System.out.println("parsed account: " + ign);

                    boolean warpAccount = false;
                    for (Entry<String, String> account : accountsToWarp.entrySet()) {
                        if (Objects.equals(account.getValue(), ign)) {
                            warpAccount = true;
                            break;
                        }
                    }
                    if (!warpAccount) {
                        // an account that isn't supposed to be in the party has been invited
                        System.out.println("Aborting because an unknown account has joined");
                        abort(true);
                    }
                } else if (PARTY_LEAVE.match(message, partyMessageGroups)) {
                    Matcher accountMatcher = accountPattern.matcher(partyMessageGroups.get("1"));
                    String ign = "";
                    if (accountMatcher.find()) {
                        ign = accountMatcher.group(3);
                    }

                    for (Entry<String, String> account : accountsToWarp.entrySet()) {
                        if (Objects.equals(account.getValue(), ign) ) {
                            PlayerInfo.partyMembers.remove(account.getKey());
                        }
                    }
                } else if (NOT_IN_PARTY.match(message, partyMessageGroups)) {
                    PlayerInfo.partyMembers.clear();
                }
            }
        }
    }

    public static ArrayList<String> whitelistedMessages = new ArrayList<>();
    public static ArrayList<String> messageQueue = new ArrayList<>();

    public static void sendChatMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer == null) {
            // player is probably swapping worlds, abort
            if (!accountsToWarp.isEmpty()) abort(true);
        } else {
            whitelistedMessages.add(message);
            Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
        }
    }

    public enum WARP_PHASE {
        INVITE,
        WARP,
        VERIFICATION,
        KICK
    }

    @SubscribeEvent
    public void serverDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        messageQueue.clear(); // clear on disconnect
        if (PlayerInfo.currentNetwork.equalsIgnoreCase("hypixel")) {

            KryoNetwork.RegisterToWarpServer unregister = new KryoNetwork.RegisterToWarpServer();
            unregister.unregister = true;
            PlayerInfo.registeredToWarp = false;
            unregister.server = PlayerInfo.currentServer;
            ServerConnection.sendTCP(unregister);

            PlayerInfo.currentServer = "";
        }

        PlayerInfo.currentNetwork = null;
    }

    public static void requestWarp(String server) {
        KryoNetwork.RequestWarpToServer request = new KryoNetwork.RequestWarpToServer();
        request.server = server;
        request.serverType = "Crystal Hollows";

        requestedWarp = server;

        ServerConnection.sendTCP(request);
    }
}
