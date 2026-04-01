package com.github.indigopolecat.events;

import com.github.indigopolecat.bingobrewers.*;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class HypixelPackets {
    public static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if (System.currentTimeMillis() > subscribeToCHServerTime) {
                if (BingoBrewersConfig.getConfig().crystalHollowsWaypointsToggle) {
                    Level level = Minecraft.getInstance().level;
                    if (level == null) return;
                    long worldTime = level.getGameTime();
                    PlayerInfo.day = (int) (worldTime / 24000000);

                    if (PlayerInfo.currentServer == null) return;
                    KryoNetwork.SubscribeToCHServer CHRequest = new KryoNetwork.SubscribeToCHServer();
                    CHRequest.server = PlayerInfo.currentServer;
                    CHRequest.day = PlayerInfo.day;
                    ServerConnection.SubscribeToCHServer(CHRequest);
                }
                subscribeToCHServerTime = Long.MAX_VALUE;
            }


            if (System.currentTimeMillis() - BingoBrewers.lastPacketSentAt > 2500 && BingoBrewers.waitingForPacketResponse) {
                BingoBrewers.packetHold.addFirst(BingoBrewers.lastPacketSent);
                BingoBrewers.waitingForPacketResponse = false;
            }

            if (BingoBrewers.packetHold.isEmpty()) return;

            if (System.currentTimeMillis() - BingoBrewers.lastPacketSentAt > 2500) {
                HypixelPacket packet = BingoBrewers.packetHold.getFirst();
                BingoBrewers.INSTANCE.sendPacket(packet);
                BingoBrewers.packetHold.removeIf(hypixelPacket -> packet.getClass() == hypixelPacket.getClass());
            }
        });
    }

    public static void onPingPacket(ClientboundPingPacket packet) {
        if (BingoBrewers.lastPacketSent instanceof ServerboundPingPacket) {
            BingoBrewers.waitingForPacketResponse = false;
        }
        System.out.println("PING");
    }

    public static void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
        if (BingoBrewers.lastPacketSent instanceof ServerboundPartyInfoPacket) {
            BingoBrewers.waitingForPacketResponse = false;
            System.out.println("no longer waiting");
        }

        System.out.println("party packet");
        PlayerInfo.inParty = packet.isInParty();
        Map<UUID, ClientboundPartyInfoPacket.PartyMember> party = packet.getMemberMap();
        ArrayList<String> uuids = new ArrayList<>();

        for (UUID uuid : party.keySet()) {
            if (party.get(uuid).getRole().equals(ClientboundPartyInfoPacket.PartyRole.LEADER)) continue;
            else uuids.add(uuid.toString());
        }
        PlayerInfo.partyMembers = uuids;
    }

    public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
        if (BingoBrewers.lastPacketSent instanceof ServerboundPlayerInfoPacket) {
            BingoBrewers.waitingForPacketResponse = false;
        }
    }

    public static long checkScoreboardForBingoTime = Long.MAX_VALUE; // This variable is not
    public static long subscribeToCHServerTime = Long.MAX_VALUE;

    public static void onLocationEvent(ClientboundLocationPacket packet) {
        Log.info("Location Packet: " + packet.toString());
        checkScoreboardForBingoTime = System.currentTimeMillis() + 1500;
        if (packet.getServerType().isEmpty()) return;
        PlayerInfo.playerGameType = packet.getServerType().get().getName();
        if (PlayerInfo.playerGameType.equalsIgnoreCase("skyblock")) {
            if (packet.getMode().isEmpty()) return;
            PlayerInfo.playerLocation = packet.getMode().get();
            PlayerInfo.inSkyblockOrPTL = true;
        } else {
            PlayerInfo.inSkyblockOrPTL = PlayerInfo.playerGameType.equalsIgnoreCase("prototype") || PlayerInfo.playerGameType.equalsIgnoreCase("limbo");
        }

        PlayerInfo.currentServer = packet.getServerName();
        if (PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows") && !PlayerInfo.subscribedToCurrentCHServer) {
            subscribeToCHServerTime = System.currentTimeMillis() + 2000;
        }
    }
}
