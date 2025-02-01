package com.github.indigopolecat.events;

import com.github.indigopolecat.bingobrewers.*;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.bingobrewers.Hud.SplashInfoHud;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import com.github.indigopolecat.kryo.KryoNetwork;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class HypixelPackets {

    public void onHelloEvent(ClientboundHelloPacket packet) {
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

        if (Warping.warpThread != null) {
            Warping.warpThread.resume();
        }

    }

    public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
        if (BingoBrewers.lastPacketSent instanceof ServerboundPlayerInfoPacket) {
            BingoBrewers.waitingForPacketResponse = false;
        }
    }

    public static long checkScoreboardForBingoTime = Long.MAX_VALUE; // This variable is not
    public static long subscribeToCHServerTime = Long.MAX_VALUE;

    public static void onLocationEvent(ClientboundLocationPacket packet) {
        System.out.println("Location Packet: " + packet.toString());

        Warping.requestedWarp = ""; // reset once servers are swapped

        checkScoreboardForBingoTime = System.currentTimeMillis() + 1500;
        if (!packet.getServerType().isPresent()) return;
        PlayerInfo.playerGameType = packet.getServerType().get().getName();
        if (PlayerInfo.playerGameType.equalsIgnoreCase("skyblock")) {
            if (!packet.getMode().isPresent()) return;
            PlayerInfo.playerLocation = packet.getMode().get();
            PlayerInfo.inSkyblockOrPTL = true;
        } else if (PlayerInfo.playerGameType.equalsIgnoreCase("prototype")) {
            PlayerInfo.inSkyblockOrPTL = true;
        } else if (PlayerInfo.playerGameType.equalsIgnoreCase("limbo")) {
            PlayerInfo.inSkyblockOrPTL = true;
        } else {
            PlayerInfo.inSkyblockOrPTL = false;
        }


        PlayerInfo.currentServer = packet.getServerName();
        if (PlayerInfo.currentServer != null && !PlayerInfo.currentServer.isEmpty()) {
            for (SplashNotificationInfo notif : SplashInfoHud.activeSplashes) {

                PlayerInfo.playerHubNumber = PlayerInfo.hubServerMap.get(PlayerInfo.currentServer);
                PlayerInfo.playerHubNumber = PlayerInfo.dungeonHubServerMap.get(PlayerInfo.currentServer);
                if (notif.serverID.equalsIgnoreCase(PlayerInfo.currentServer)) {
                    PlayerInfo.inSplashHub = true;
                    PlayerInfo.lastSplashHubPresenceUpdate = System.currentTimeMillis();
                } else if (PlayerInfo.playerHubNumber.equals(notif.hubNumber)) {
                    PlayerInfo.inSplashHub = true;
                    PlayerInfo.lastSplashHubPresenceUpdate = System.currentTimeMillis();
                }
            }
        }

        if (PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows") && !PlayerInfo.subscribedToCurrentCHServer) {
            subscribeToCHServerTime = System.currentTimeMillis() + 2000;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (System.currentTimeMillis() > checkScoreboardForBingoTime && BingoBrewers.onHypixel && PlayerInfo.playerGameType.equalsIgnoreCase("skyblock")) {
                PlayerInfo.onBingo = ScoreBoard.isBingo();
                checkScoreboardForBingoTime = Long.MAX_VALUE;
            }

            if (System.currentTimeMillis() > subscribeToCHServerTime) {
                if (BingoBrewersConfig.crystalHollowsWaypointsToggle) {
                    // update day
                    World world = Minecraft.getMinecraft().theWorld;
                    long worldTime = world.getWorldTime();
                    PlayerInfo.day = (int) (worldTime / 24000);

                    if (PlayerInfo.currentServer == null) return;
                    KryoNetwork.SubscribeToCHServer CHRequest = new KryoNetwork.SubscribeToCHServer();
                    CHRequest.server = PlayerInfo.currentServer;
                    CHRequest.day = PlayerInfo.day;
                    ServerConnection.SubscribeToCHServer(CHRequest);

                    System.out.println("Registering to warp for " + PlayerInfo.currentServer);
                    KryoNetwork.RegisterToWarpServer register = new KryoNetwork.RegisterToWarpServer();
                    register.unregister = false;
                    PlayerInfo.registeredToWarp = true;
                    register.server = PlayerInfo.currentServer;
                    ServerConnection.sendTCP(register);
                }
                subscribeToCHServerTime = Long.MAX_VALUE;
            }

            if (System.currentTimeMillis() - BingoBrewers.lastPacketSentAt > 2000 && BingoBrewers.waitingForPacketResponse) {
                BingoBrewers.packetHold.add(0, BingoBrewers.lastPacketSent);
                BingoBrewers.waitingForPacketResponse = false;
            }

            if (BingoBrewers.packetHold.isEmpty()) return;

            if (System.currentTimeMillis() - BingoBrewers.lastPacketSentAt > 2500 && !BingoBrewers.packetHold.isEmpty()) {
                HypixelPacket packet = BingoBrewers.packetHold.get(0);
                BingoBrewers.INSTANCE.sendPacket(packet);
                BingoBrewers.packetHold.removeIf(hypixelPacket -> packet.getClass() == hypixelPacket.getClass());
                BingoBrewers.packetHold.remove(packet);
            }
        }
    }
}
