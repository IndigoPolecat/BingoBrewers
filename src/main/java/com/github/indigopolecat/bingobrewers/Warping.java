package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Warping {
    public static ConcurrentHashMap<String, String> accountsToWarp = new ConcurrentHashMap<>();
    public static boolean invite;
    public static String server;
    // when the party invite was sent out
    public static long inviteSent;
    public static boolean requestLiveParty = false;
    public static long lastPartyUpdate;
    public static boolean waitingOnLocation;
    public static boolean partyReady;
    public static PARTY_PACKET_INTENT intent;
    public static boolean PARTY_EMPTY_KICK = false;

    public static boolean kickParty;
    public static int ticksSinceLastKick = 8;
    public static int TICKS_BETWEEN_KICKS = 8;

    public static void warp() {
        requestLiveParty = false;
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/p warp");
    }

    public static void abort(boolean ineligible) {
        kickParty = true;
        BingoBrewers bb = new BingoBrewers();
        bb.sendPacket(new ServerboundPartyInfoPacket());
        intent = PARTY_PACKET_INTENT.VERIFY_MEMBERS_KICKED;

        KryoNetwork.AbortWarpTask abort = new KryoNetwork.AbortWarpTask();
        abort.ign = Minecraft.getMinecraft().thePlayer.getDisplayNameString();
        abort.ineligible = ineligible;
        ServerConnection.sendTCP(abort);
        accountsToWarp.clear();
        requestLiveParty = false;
        partyReady = false;
        waitingOnLocation = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) {
            if (requestLiveParty && lastPartyUpdate + 2000 < System.currentTimeMillis()) {
                if (intent != PARTY_PACKET_INTENT.VERIFY_MEMBERS_KICKED) {
                    lastPartyUpdate = System.currentTimeMillis();
                    BingoBrewers bb = new BingoBrewers();
                    bb.sendPacket(new ServerboundPartyInfoPacket());
                    intent = PARTY_PACKET_INTENT.CHECK_MEMBERS_JOINED;
                }
            }
            if (kickParty && !PARTY_EMPTY_KICK) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/p disband");
            } else if (kickParty && ticksSinceLastKick >= TICKS_BETWEEN_KICKS && PARTY_EMPTY_KICK) {
                ticksSinceLastKick = 0;
                String accountToKickUUID = accountsToWarp.keySet().toArray(new String[0])[0];
                String accountToKick = accountsToWarp.get(accountToKickUUID);
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/p kick " + accountToKick);
                accountsToWarp.remove(accountToKickUUID);
                if (accountsToWarp.isEmpty()) {
                    kickParty = false;
                    ticksSinceLastKick = TICKS_BETWEEN_KICKS;
                }
            } else if (kickParty) {
                ticksSinceLastKick++;
            }
            if (invite && !accountsToWarp.isEmpty()) {
                StringBuilder inviteCommand = new StringBuilder("/p invite");
                for (String ign : accountsToWarp.values()) {
                    inviteCommand.append(" ").append(ign);
                }
                Minecraft.getMinecraft().thePlayer.sendChatMessage(inviteCommand.toString());
            }
        }
    }

    public enum PARTY_PACKET_INTENT {
        VERIFY_MEMBERS_KICKED,
        CHECK_MEMBERS_JOINED,
        VERIFY_PARTY_EMPTY
    }
}
