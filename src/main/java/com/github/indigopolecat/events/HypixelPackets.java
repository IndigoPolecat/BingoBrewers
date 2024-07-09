package com.github.indigopolecat.events;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.PlayerInfo;
import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.bingobrewers.Warping;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class HypixelPackets {

    public void onHelloEvent(ClientboundHelloPacket packet) {
    }
    public static void onPingPacket(ClientboundPingPacket packet) {
        System.out.println("PING");
    }

    public static void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
        PlayerInfo.inParty = packet.isInParty();
        Map<UUID, ClientboundPartyInfoPacket.PartyMember> party = packet.getMemberMap();
        ArrayList<String> uuids = new ArrayList<>();

        for (UUID uuid : party.keySet()) {
            if (party.get(uuid).getRole().equals(ClientboundPartyInfoPacket.PartyRole.LEADER)) continue;
            else uuids.add(uuid.toString());
        }
        PlayerInfo.partyMembers.addAll(uuids);

        if (Warping.warpThread != null) Warping.warpThread.resume();

    }

    public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
    }

    public void onLocationEvent(ClientboundLocationPacket packet) {
    }
}
