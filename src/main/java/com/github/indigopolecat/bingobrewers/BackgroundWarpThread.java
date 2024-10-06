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
    public boolean chatWarpOverride = false; // if chat says everyone has joined the party we can just warp
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

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            } else {
                Warping.abort(true);
            }
            while (!stop) {
                waitForJoinAndWarp();
                kickPartyAndVerify();
            }
            resetWarpThread();
            System.out.println("ending execution");
        }
    }

    public void kickPartyAndVerify() {
        if (stop) return;

        if (kickParty && PARTY_EMPTY_KICK && timeOfLastKick + 350 <= System.currentTimeMillis()) {
            Warping.PHASE = Warping.WARP_PHASE.KICK;
            if (!accountsToKick.isEmpty()) {
                String accountToKickUUID = accountsToKick.keySet().toArray(new String[0])[0];
                String accountToKick = accountsToKick.get(accountToKickUUID);

                Warping.sendChatMessage("/p kick " + accountToKick);
                System.out.println("kicking " + accountToKick);

                timeOfLastKick = System.currentTimeMillis();
                accountsToKick.remove(accountToKickUUID);
                accountsKicked.put(accountToKickUUID, accountToKick);

            } else if (System.currentTimeMillis() - timeOfLastPartyInfo > 1750) {
                PHASE = null;
                kickParty = false;
                BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());
                timeOfLastPartyInfo = System.currentTimeMillis();

                try {
                    this.wait();
                    if (stop) return;
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
                if (stop) return;
                System.out.println("normal disband");

                Warping.sendChatMessage("/p disband");

                PHASE = null;
                kickParty = false;
                BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());

                try {
                    this.wait();
                    if (stop) return;
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
            accountsToWarp.clear();
            accountsKicked.clear();
            accountsToKick.clear();
            stop = true;
        } else if (kickParty) {
            PARTY_EMPTY_KICK = true;
        }
    }

    public void waitForJoinAndWarp() {
        if (stop) return;
        Warping.PHASE = WARP;

        if (System.currentTimeMillis() < warpTime) return;
        System.out.println("current: " + System.currentTimeMillis() + " warpTime: " + warpTime);

        if (!chatWarpOverride) {
            System.out.println("requesting packet");
            BingoBrewers.INSTANCE.sendPacket(new ServerboundPartyInfoPacket());

            try {
                this.wait();
                if (stop) return;
                System.out.println("continuing");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        ArrayList<String> uuids = PlayerInfo.partyMembers;

        System.out.println("uuids: " + uuids.toString());
        System.out.println("warp: " + accountsToWarp.toString());

        if (Warping.accountsToWarp.keySet().containsAll(uuids) && uuids.containsAll(Warping.accountsToWarp.keySet())) { // everyone is in the party
            System.out.println("normal warp");
            Warping.warp();
        } else if (Warping.accountsToWarp.keySet().containsAll(uuids) && warpTime + 3500 < System.currentTimeMillis()) { // If we're still missing someone after an extra 3.5s, warp anyway
            System.out.println("3.5s warp");
            Warping.warp();
        } else if (!Warping.accountsToWarp.keySet().containsAll(uuids)) { // there is someone who isn't supposed to be warped in the party
            Warping.PARTY_EMPTY_KICK = true;
            Warping.abort(true);
        } else if (PlayerInfo.partyMembers.isEmpty() && System.currentTimeMillis() - executionTimeBegan > 7000) {
            stop = true;
            Warping.sendChatMessage("/p disband");
            return;
        }

        if (warpAttempts == 1 && warpTime == Long.MAX_VALUE) {
            // aka if this was the 2nd attempt
            kickParty = true;
        }

    }

    public synchronized void resume() {
        this.notify();
    }
}
