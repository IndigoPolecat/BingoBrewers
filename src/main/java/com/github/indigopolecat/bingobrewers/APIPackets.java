package com.github.indigopolecat.bingobrewers;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class APIPackets {
    public void sendPacket() {
        HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
    }

    public void registerPacketHandler() {
        HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
            @Override
            public void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
                if (PlayerInfo.inParty && PlayerInfo.registeredToWarp) {
                    Map<UUID, ClientboundPartyInfoPacket.PartyMember> party = packet.getMemberMap();
                    ArrayList<String> uuids = new ArrayList<>();
                    for (UUID uuid : party.keySet()) {
                        if (party.get(uuid).getRole().equals(ClientboundPartyInfoPacket.PartyRole.LEADER)) continue;
                        uuids.add(uuid.toString());
                    }
                    if (Warping.accountsToWarp.keySet().containsAll(uuids) && uuids.containsAll(Warping.accountsToWarp.keySet())) {
                        Warping.warp();
                    } else if (Warping.accountsToWarp.keySet().containsAll(uuids) && Warping.inviteSent + 1000 < System.currentTimeMillis()) {

                    }
                }
                PlayerInfo.inParty = packet.isInParty();
            }
        });
    }
}
