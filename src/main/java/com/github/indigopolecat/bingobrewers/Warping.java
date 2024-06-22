package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ConcurrentHashMap;

public class Warping {
    public static ConcurrentHashMap<String, String> accountsToWarp = new ConcurrentHashMap<>();
    public static String server;
    public static long warpDelay;
    // when the party invite was sent out
    public static long inviteSent;
    public static boolean requestLiveParty = false;
    public static long lastPartyUpdate;

    public static void warp() {
        requestLiveParty = false;
    }

    public static void abort(boolean ineligible) {
        KryoNetwork.AbortWarpTask abort = new KryoNetwork.AbortWarpTask();
        abort.ign = Minecraft.getMinecraft().thePlayer.getDisplayNameString();
        abort.ineligible = ineligible;
        ServerConnection.sendTCP(abort);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) {
            if (requestLiveParty && lastPartyUpdate + 2000 < System.currentTimeMillis()) {
                lastPartyUpdate = System.currentTimeMillis();
                BingoBrewers bb = new BingoBrewers();
                bb.sendPacket(new ServerboundPartyInfoPacket());
            }
        }
    }
}
