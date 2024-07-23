package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

import java.util.ArrayList;

import static com.github.indigopolecat.bingobrewers.Warping.*;
import static com.github.indigopolecat.bingobrewers.Warping.WARP_PHASE.INVITE;
import static com.github.indigopolecat.bingobrewers.Warping.WARP_PHASE.WARP;

public class BackgroundWarpThread implements Runnable {
    public volatile boolean stop = false;
    public long executionTimeBegan;
    public static long timeOfLastKick;
    public KryoNetwork.DoneWithWarpTask conclusion = new KryoNetwork.DoneWithWarpTask();
    public static long timeOfLastPartyInfo;
    public long warpTime = 0;
    public int warpAttempts = 0;


    @Override
    public void run() {
        synchronized (this) {
            System.out.println("beginning execution");
            executionTimeBegan = System.currentTimeMillis();
            Warping.PHASE = INVITE;
            if (PlayerInfo.currentServer.equals(Warping.server) && PlayerInfo.registeredToWarp) {
                StringBuilder inviteCommand = new StringBuilder("/p invite");
                for (String ign : accountsToWarp.values()) {
                    inviteCommand.append(" ").append(ign);
                }
                System.out.println("sending: " + inviteCommand.toString());
                Warping.sendChatMessage(inviteCommand.toString());
                warpTime = System.currentTimeMillis() + 1500;
            } else {
                Warping.abort(true);
            }
            while (!stop) {
                waitForJoinAndWarp();
                kickPartyAndVerify();
            }
            System.out.println("ending execution");
            Warping.warpThread = null;
        }
    }

    public void kickPartyAndVerify() {
        if (kickParty && PARTY_EMPTY_KICK && timeOfLastKick + 350 <= System.currentTimeMillis()) {
            Warping.PHASE = Warping.WARP_PHASE.KICK;
            if (!accountsToKick.isEmpty()) {
                String accountToKickUUID = accountsToKick.keySet().toArray(new String[0])[0];
                String accountToKick = accountsToKick.get(accountToKickUUID);

                Warping.sendChatMessage("/p kick " + accountToKick);
                System.out.println("kicking " + accountToKick);
                accountsToKick.remove(accountToKickUUID);
                accountsKicked.put(accountToKickUUID, accountToKick);

            } else if (System.currentTimeMillis() - timeOfLastPartyInfo > 1750) {
                PHASE = null;
                kickParty = false;
                BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());
                timeOfLastPartyInfo = System.currentTimeMillis();

                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // stop waiting once packet is received and resume() is called

                if (PlayerInfo.inParty) {
                    for (String uuid : PlayerInfo.partyMembers) {
                        if (accountsKicked.containsKey(uuid)) {
                            System.out.println("failed to kick (rekicking) " + accountsToKick);
                            String ignToRekick = accountsKicked.get(uuid);
                            accountsToKick.put(uuid, ignToRekick);
                            kickParty = true;
                        }
                    }
                    accountsKicked.clear();
                }
            }

        } else if (kickParty && (warpAttempts > 1 || accountsToWarp.isEmpty())) {
            Warping.PHASE = Warping.WARP_PHASE.KICK;
            int loopCounter = 0;
            while (PlayerInfo.inParty) {
                Warping.sendChatMessage("/p disband");

                PHASE = null;
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
                        Thread.sleep(1000 * loopCounter);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Warping.sendChatMessage("/p disband");
                    break;
                }
            }
        } else if (kickParty) {
            PARTY_EMPTY_KICK = true;
        }
    }

    public void waitForJoinAndWarp() {
        Warping.PHASE = WARP;
        System.out.println("current time: " + System.currentTimeMillis() + " warptime: " + warpTime);

        if (System.currentTimeMillis() < warpTime && !accountsToWarp.values().containsAll(PlayerInfo.partyMembers)) return;

        BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());


        try {
            this.wait();
            System.out.println("continuing");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ArrayList<String> uuids = PlayerInfo.partyMembers;

        System.out.println("uuids: " + uuids.toString());
        System.out.println("warp: " + accountsToWarp.toString());

        if (Warping.accountsToWarp.keySet().containsAll(uuids) && uuids.containsAll(Warping.accountsToWarp.keySet())) { // everyone is in the party
            Warping.warp();
        } else if (Warping.accountsToWarp.keySet().containsAll(uuids) && warpTime + 3500 < System.currentTimeMillis()) { // If we're still missing someone after an extra 3.5s, warp anyway
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
