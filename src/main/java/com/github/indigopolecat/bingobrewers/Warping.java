package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.events.PacketEvent;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Warping {
    public static volatile ConcurrentHashMap<String, String> accountsToWarp = new ConcurrentHashMap<>();
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

    public static void warp() {
        sendChatMessage("/p warp");
        warpThread.inviteSent = 0; // reset
    }

    public static void abort(boolean ineligible) {
        kickParty = true;
        BingoBrewers bb = new BingoBrewers();
        bb.sendPacket(new ServerboundPartyInfoPacket());


        KryoNetwork.AbortWarpTask abort = new KryoNetwork.AbortWarpTask();
        abort.ign = Minecraft.getMinecraft().thePlayer.getDisplayNameString();
        abort.ineligible = ineligible;
        ServerConnection.sendTCP(abort);
        accountsToWarp.clear();
        partyReady = false;
        waitingOnLocation = true;

        if (warpThread != null) {
            warpThread.stop = true;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) {
        }
    }

    public static ArrayList<String> chatMessageHold = new ArrayList<>();
    public static boolean lookForEndingMessage = false;
    public static void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        if (chatMessageHold.contains(message)) {
            event.setCanceled(true);
        }

    }

    // cancelling these packets is intentional in an effort to ensure other mods can avoid interacting with them since this is not a party they player will likely notice they're in
    @SubscribeEvent
    public void onPacketReceived(PacketEvent.Received event) {
        if (event.getPacket() instanceof S02PacketChat) {
            if (((S02PacketChat) event.getPacket()).getType() == 2) return;
            String message = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
            if (message.equals("-----------------------------------------------------")) {

                event.setCanceled(true);
                lookForEndingMessage = !lookForEndingMessage;
            } else if (message.equals("-----------------------------")) {
                event.setCanceled(true);
                lookForEndingMessage = !lookForEndingMessage;
            } else if (lookForEndingMessage) {
                event.setCanceled(true);
            }
        }
    }

    public static void sendChatMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer == null) {
            // player is probably swapping worlds, abort
            Warping.abort(true);
        } else {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
        }
    }

    public enum WARP_PHASE {
        INVITE,
        WARP,
        KICK
    }
}
