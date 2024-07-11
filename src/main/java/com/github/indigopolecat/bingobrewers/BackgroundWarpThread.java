package com.github.indigopolecat.bingobrewers;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.indigopolecat.bingobrewers.Warping.*;
import static com.github.indigopolecat.bingobrewers.Warping.WARP_PHASE.INVITE;
import static com.github.indigopolecat.bingobrewers.Warping.WARP_PHASE.WARP;

public class BackgroundWarpThread implements Runnable {
    public volatile boolean stop = false;
    public static long executionTimeBegan;
    public static long timeOfLastKick;
    public static ConcurrentHashMap<String, String> accountsKicked;
    public long inviteSent;


    @Override
    public void run() {
        executionTimeBegan = System.currentTimeMillis();
        Warping.PHASE = INVITE;
        if (PlayerInfo.currentServer.equals(Warping.server) && PlayerInfo.registeredToWarp) {
            StringBuilder inviteCommand = new StringBuilder("/p invite");
            for (String ign : accountsToWarp.values()) {
                inviteCommand.append(" ").append(ign);
            }
            Warping.sendChatMessage(inviteCommand.toString());
            inviteSent = System.currentTimeMillis();
        } else {
            Warping.abort(true);
        }
        while (!stop) {
            waitForJoinAndWarp();
            kickPartyAndVerify();
        }
        Warping.warpThread = null;
    }

    public void kickPartyAndVerify() {
        if (kickParty && PARTY_EMPTY_KICK && timeOfLastKick + 350 <= System.currentTimeMillis()) {
            if (!accountsToWarp.isEmpty()) {
                String accountToKickUUID = accountsToWarp.keySet().toArray(new String[0])[0];
                String accountToKick = accountsToWarp.get(accountToKickUUID);

                Warping.sendChatMessage("/p kick " + accountToKick);
                accountsToWarp.remove(accountToKickUUID);
                accountsKicked.put(accountToKickUUID, accountToKick);

            } else {
                kickParty = false;
                BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());

                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // stop waiting once packet is received and resume() is called

                if (PlayerInfo.inParty) {
                    for (String uuid : PlayerInfo.partyMembers) {
                        if (accountsKicked.containsKey(uuid)) {
                            String ignToRekick = accountsKicked.get(uuid);
                            accountsToWarp.put(uuid, ignToRekick);
                            kickParty = true;
                        }
                    }
                }
            }
        } else if (kickParty) {
            int loopCounter = 0;
            while (PlayerInfo.inParty) {
                Warping.sendChatMessage("/p disband");
                kickParty = false;
                BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());

                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                loopCounter++;
                if (loopCounter > 3) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Warping.sendChatMessage("/p disband");
                    break;
                }
            }
        }
    }

    public void waitForJoinAndWarp() {
        Warping.PHASE = WARP;
        if (inviteSent == 0) return;
        BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());

        try {
            this.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ArrayList<String> uuids = PlayerInfo.partyMembers;

        if (Warping.accountsToWarp.keySet().containsAll(uuids) && uuids.containsAll(Warping.accountsToWarp.keySet())) { // everyone is in the party
            Warping.warp();
        } else if (Warping.accountsToWarp.keySet().containsAll(uuids) && Warping.warpThread.inviteSent + 5000 < System.currentTimeMillis()) { // warp after 5 seconds even if the party isn't full
            Warping.warp();
        } else if (!Warping.accountsToWarp.keySet().containsAll(uuids)) { // there is someone who isn't supposed to be warped in the party
            Warping.PARTY_EMPTY_KICK = true;
            Warping.abort(true);
        } else {
            Warping.lastPartyUpdate = System.currentTimeMillis();
        }
    }

    public synchronized void resume() {
        this.notify();
    }
    public synchronized void end() {
        stop = true;
    }
}
