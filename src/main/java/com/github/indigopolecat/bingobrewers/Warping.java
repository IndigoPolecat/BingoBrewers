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

public class Warping {
    public static volatile ConcurrentHashMap<String, String> accountsToWarp = new ConcurrentHashMap<>();
    public static volatile ConcurrentHashMap<String, String> accountsToKick = new ConcurrentHashMap<>();
    public static volatile ConcurrentHashMap<String, String> accountsKicked = new ConcurrentHashMap<>();
    public static String server;
    // when the party invite was sent out
    public static long lastPartyUpdate;
    public static boolean waitingOnLocation;
    public static boolean partyReady;
    public static WARP_PHASE PHASE;
    public static boolean PARTY_EMPTY_KICK = false;

    public static volatile boolean kickParty;
    public static int ticksSinceLastKick = 8;
    public static int TICKS_BETWEEN_KICKS = 8;
    public static boolean expectingPartyInfoMessage = false;

    public static BackgroundWarpThread warpThread;
    public boolean delayPartyInvites;

    public static void warp() {
        // todo: come back to this and consider immediately kicking instead of waiting for the chat message
        System.out.println("warping");
        sendChatMessage("/p warp");
        timeOfInvite = 0; // reset
        warpThread.warpAttempts += 1;

        PHASE = WARP_PHASE.VERIFICATION;
    }

    public static void abort(boolean ineligible) {
        System.out.println("aborting warp");
        kickParty = true;
        BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());


        KryoNetwork.AbortWarpTask abort = new KryoNetwork.AbortWarpTask();
        abort.ign = Minecraft.getMinecraft().thePlayer.getDisplayNameString();
        abort.ineligible = ineligible;
        ServerConnection.sendTCP(abort);
        accountsToWarp.clear();
        partyReady = false;
        waitingOnLocation = true;

        if (warpThread != null) {
            warpThread.end();
        }
    }

    public static long lastMessageSent;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) {
            if (warpThread != null && warpThread.stop) {
                warpThread = null;
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

    public static Pattern partyInvitePattern = Pattern.compile("-----------------------------------------------------\n(\\[\\w+\\+*])?\\s*([\\w_]+) has invited you to join their party!\nYou have 60 seconds to accept. Click here to join!\n-----------------------------------------------------");
    public static ArrayList<String> partyInvites = new ArrayList<>(); // this variable is used for whichever comes first, BB server packet with warper info, or HP party invite
    public static String warperIGN;
    public static long timeOfInvite = 0; // this variable is used for whichever comes first, BB server packet with warper info, or HP party invite

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();

        Matcher partyInviteMatcher = partyInvitePattern.matcher(message);
        if (partyInviteMatcher.find()) {
            partyInvites.add(partyInviteMatcher.group(2));
            timeOfInvite = System.currentTimeMillis();

            if (partyInvites.contains(warperIGN) && System.currentTimeMillis() - timeOfInvite < 5000 ) {
                // we have already received the packet telling us the ign and we can safely join
                sendChatMessage("/p accept " + warperIGN);
                partyInvites = new ArrayList<>();
                warperIGN = null;
                timeOfInvite = 0;
            } else if (System.currentTimeMillis() - timeOfInvite > 5000) {
                warperIGN = null;
                partyInvites = new ArrayList<>();
                timeOfInvite = 0;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageMatcher createMatcher(JsonObject object, String key) {
        System.out.println(key);
        JsonArray jsonArray = object.getAsJsonArray(key);
        System.out.println("json Array: " + jsonArray);
        List<String> list = StreamSupport.stream(jsonArray.spliterator(), false)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
        System.out.println(Arrays.toString(list.toArray()));
        return new MessageMatcher(list);
    }


    // cancelling these packets is intentional in an effort to ensure other mods can avoid interacting with them since this is not a party they player will likely notice they're in
    @SubscribeEvent
    public void onPacketReceived(PacketEvent.Received event) {
        if (event.getPacket() instanceof S02PacketChat) {
            if (((S02PacketChat) event.getPacket()).getType() == 2) return;
            String message = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
            if (PHASE == null) return;
            if (message.equals("§9§m-----------------------------------------------------§r")) {

                //event.setCanceled(true);
                lookForEndingMessage = !lookForEndingMessage;
            } else if (message.equals(warpMessageTrigger)) {
                //event.setCanceled(true);
                lookForEndingMessage = !lookForEndingMessage;

                if (lookForEndingMessage) {
                    PHASE = WARP_PHASE.KICK;
                    kickParty = true;
                    PARTY_EMPTY_KICK = false;

                    warpThread.conclusion.ignsWarped.clear();
                } else {
                    System.out.println(accountsToWarp.toString());
                    if (!accountsToWarp.isEmpty()) {
                        warpThread.warpTime = System.currentTimeMillis() + 5250;
                    }

                }
            } else if (lookForEndingMessage) {
                Matcher playerWarpMatcher = playerWarpPattern.matcher(message);
                if (playerWarpMatcher.find()) {
                    if (playerWarpMatcher.group(1).equalsIgnoreCase("✮") || playerWarpMatcher.group(1).equalsIgnoreCase("✔") || warpThread.warpAttempts > 1) {
                        for (Entry<String, String> player : accountsToWarp.entrySet()) {
                            if (player.getValue().equals(playerWarpMatcher.group(3))) {
                                System.out.println("removing " + player.getValue() + " from warp accounts");
                                warpThread.conclusion.ignsWarped.add(playerWarpMatcher.group(3));
                                accountsToKick.put(player.getKey(), player.getValue());
                                accountsToWarp.remove(player.getKey());
                            }
                        }
                    } else {
                        warpThread.conclusion.successful = false;
                    }
                }

                HashMap<String, String> partyMesageGroups = new HashMap<>();

                if (PARTY_JOIN.match(message, partyMesageGroups)) {
                    for (Entry<String, String> account : accountsToWarp.entrySet()) {
                        if (account.getValue().equalsIgnoreCase(partyMesageGroups.get("1"))) {
                            PlayerInfo.partyMembers.add(account.getKey());
                        }
                    }
                    PlayerInfo.partyMembers.add(partyMesageGroups.get("1"));
                    if (PlayerInfo.partyMembers.containsAll(accountsToWarp.values()) && accountsToWarp.values().containsAll(PlayerInfo.partyMembers)) {
                        // if the party has all members
                        warpThread.warpTime = System.currentTimeMillis();
                    }
                } else if (PARTY_LEAVE.match(message, partyMesageGroups)) {
                    for (Entry<String, String> account : accountsToWarp.entrySet()) {
                        if (account.getValue().equalsIgnoreCase(partyMesageGroups.get("1"))) {
                            PlayerInfo.partyMembers.remove(account.getKey());
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<String> whitelistedMessages = new ArrayList<>();
    public static ArrayList<String> messageQueue = new ArrayList<>();

    public static void sendChatMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer == null) {
            // player is probably swapping worlds, abort
            Warping.abort(true);
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
}
