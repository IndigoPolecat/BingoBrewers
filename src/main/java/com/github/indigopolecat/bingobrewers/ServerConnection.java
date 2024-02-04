package com.github.indigopolecat.bingobrewers;

import java.io.IOException;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.ReceivedString;
import com.github.indigopolecat.kryo.KryoNetwork.ResponseString;
import com.github.indigopolecat.kryo.KryoNetwork.SplashNotification;
import com.esotericsoftware.kryonet.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static com.esotericsoftware.minlog.Log.LEVEL_TRACE;
import static java.lang.String.valueOf;

public class ServerConnection extends Listener implements Runnable {

    // The Hud renderer checks this every time it renders
    public static ArrayList<HashMap<String, ArrayList<String>>> mapList = new ArrayList<>();
    public static ArrayList<String> keyOrder = new ArrayList<>();
    public static ResourceLocation alarm = new ResourceLocation("sounds", "BigWave");
    int waitTime;
    boolean repeat;
    private Client client;

    @Override
    public void run() {
        this.client = new Client();
        waitTime = 5000;
        System.out.println("Disconnected from server. Reconnecting in " + waitTime / 1000 + " seconds.");
        repeat = true;
        while (repeat) {
            try {
                connection();
            } catch (Exception e) {
                System.out.println("Server Connection Error: " + e.getMessage());
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (waitTime < 60000) {
                    waitTime *= 2;
                }
            }
        }

    }

    private void connection() throws IOException {
            Log.set(LEVEL_TRACE);
            KryoNetwork.register(client);
            client.addListener(new Listener() {
                public void received(Connection connection, Object object) {
                    if (object instanceof ReceivedString) {
                        ReceivedString request = (ReceivedString) object;
                        System.out.println(request.hello);
                        ResponseString response = new ResponseString();
                        response.hello = "Hello from the server!";
                        connection.sendTCP(response);
                    } else if (object instanceof SplashNotification) {
                        System.out.println("Received splash notification");
                        boolean sendNotif = true;
                        SplashNotification notif = (SplashNotification) object;
                        // Remove the previous splash notification with the same ID (if message is edited)
                        for (int i = 0; i < mapList.size(); i++) {
                            if (mapList.get(i).get("Splash").get(0).equals(notif.splash)) {
                                // Don't send notification if the hub # hasn't changed
                                if (mapList.get(i).get("Hub").get(1).contains(notif.message)) {
                                    sendNotif = false;
                                }
                                mapList.remove(mapList.get(i));
                            }
                        }
                        updateMapList(notif, sendNotif);
                    }
                }


                @Override
                public void disconnected(Connection connection) {
                    reconnect();
                }

            });
            client.start();
            client.connect(3000, "38.46.216.110", 8080, 7070);
            System.out.println("Connected to server.");
            ReceivedString request = new ReceivedString();
            request.hello = "Here is a request!";
            System.out.println("sending");
            client.sendTCP(request);
            System.out.println("sent");
            // List of all keys that may be used in infopanel, in the order they'll be rendered in an element
            keyOrder.clear(); // clear the list so it doesn't keep adding the same keys every time you reconnect
            keyOrder.add("Hub");
            keyOrder.add("Splasher");
            keyOrder.add("Party");
            keyOrder.add("Location");
            keyOrder.add("Note");
            repeat = false;
    }

    public void updateMapList(SplashNotification notif, boolean sendNotif) {
        String hub = notif.message;
        String splasher = notif.splasher;
        String partyHost = notif.partyHost;
        if (!partyHost.equals("No Party")) {
            partyHost = "/p join " + partyHost;
        }
        List<String> note = notif.note;
        // This should always be "Bea House" but is hard coded server side incase it ever needs to change quickly
        String location = notif.location;

        HashMap<String, ArrayList<String>> splashInfo = new HashMap<String, ArrayList<String>>();

        ArrayList<String> hubInfo = new ArrayList<>();
        hubInfo.add("Hub");
        hubInfo.add(": " + hub);
        splashInfo.put("Hub", hubInfo);

        ArrayList<String> splasherInfo = new ArrayList<>();
        splasherInfo.add("Splasher");
        splasherInfo.add(": " + splasher);
        splashInfo.put("Splasher", splasherInfo);

        ArrayList<String> partyInfo = new ArrayList<>();
        partyInfo.add("Bingo Party");
        partyInfo.add(": " + partyHost);
        splashInfo.put("Party", partyInfo);

        ArrayList<String> locationInfo = new ArrayList<>();
        locationInfo.add("Location");
        locationInfo.add(": " + location);
        splashInfo.put("Location", locationInfo);

        ArrayList<String> noteInfo = new ArrayList<>();
        noteInfo.add("Note");
        if (note.isEmpty()) {
            noteInfo.add(": No Note");
        } else {
            noteInfo.add(": ");
            noteInfo.addAll(note);
        }
        splashInfo.put("Note", noteInfo);

        ArrayList<String> timeInfo = new ArrayList<>();
        timeInfo.add(valueOf(System.currentTimeMillis()));
        splashInfo.put("Time", timeInfo);
        ArrayList<String> splashId = new ArrayList<>();
        splashId.add(notif.splash);
        splashInfo.put("Splash", splashId);

        mapList.add(splashInfo);
        if (sendNotif) {
            notification(hub);
        }
    }

    public void notification(String hub) {
        Minecraft.getMinecraft().ingameGUI.displayTitle("Splash in Hub " + hub, "subtitle", 0, 5, 0);
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

        System.out.println("attempting to play sound");

        player.playSound(alarm.toString(), 1.0f, 1.0f);
    }

    public void reconnect() {
        client.close();
        waitTime = 5000;
        System.out.println("Disconnected from server. Reconnecting in " + waitTime / 1000 + " seconds.");
        repeat = true;
        while (repeat) {
            try {
                this.client = new Client();
                connection();
            } catch (Exception e) {
                System.out.println("Server Connection Error: " + e.getMessage());
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (waitTime < 60000) {
                    waitTime *= 2;
                }
            }
        }
    }





}

