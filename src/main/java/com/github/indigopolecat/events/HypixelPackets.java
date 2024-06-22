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
        if (PlayerInfo.inParty && PlayerInfo.registeredToWarp) {
            if (!Warping.accountsToWarp.isEmpty()) {
                Map<UUID, ClientboundPartyInfoPacket.PartyMember> party = packet.getMemberMap();
                ArrayList<String> uuids = new ArrayList<>();

                for (UUID uuid : party.keySet()) {
                    if (party.get(uuid).getRole().equals(ClientboundPartyInfoPacket.PartyRole.LEADER)) continue;
                    else uuids.add(uuid.toString());
                }

                if (Warping.accountsToWarp.keySet().containsAll(uuids) && uuids.containsAll(Warping.accountsToWarp.keySet())) { // everyone is in the party
                    Warping.warp();
                } else if (Warping.accountsToWarp.keySet().containsAll(uuids) && Warping.inviteSent + 5000 < System.currentTimeMillis()) { // warp after 5 seconds even if the party isn't full
                    Warping.warp();
                } else if (!Warping.accountsToWarp.keySet().containsAll(uuids)) { // there is someone who isn't supposed to be warped in the party
                    Warping.requestLiveParty = false;
                    Warping.abort(true);
                } else {
                    Warping.requestLiveParty = true;
                    Warping.lastPartyUpdate = System.currentTimeMillis();
                    BingoBrewers bb = new BingoBrewers();
                    bb.sendPacket(new ServerboundPartyInfoPacket());
                }
            } else {
                KryoNetwork.RegisterToWarpServer register = new KryoNetwork.RegisterToWarpServer();
                register.server = PlayerInfo.currentServer;
                register.unregister = true;
                ServerConnection.sendTCP(register);
            }
        }
    }

    public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
    }

    public void onLocationEvent(ClientboundLocationPacket packet) {
    }
}
