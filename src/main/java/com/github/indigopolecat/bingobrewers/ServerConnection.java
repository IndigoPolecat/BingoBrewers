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
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;
import static java.lang.String.valueOf;

public class ServerConnection extends Thread {

    // The Hud renderer checks this every time it renders
    public static ArrayList<HashMap<String, ArrayList<String>>> mapList = new ArrayList<>();
    public static ArrayList<String> keyOrder = new ArrayList<>();
    public static ResourceLocation alarm = new ResourceLocation("sounds", "BigWave");

    public void run() {
        int waitTime = 5000;
        boolean repeat = true;
        while (repeat) {
            try {
                Client client = new Client();
                Log.set(LEVEL_ERROR);
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
                            SplashNotification notif = (SplashNotification) object;
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
                                splashInfo.put("Splash", new ArrayList<>());

                            mapList.add(splashInfo);

                            Minecraft.getMinecraft().ingameGUI.displayTitle("Splash in Hub: " + hub + "\nBy " + splasher, "subtitle",  0, 5, 0);
                            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

                            System.out.println("attempting to play sound");

                            player.playSound(alarm.toString(), 1.0f, 1.0f);
                        }
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
                keyOrder.add("Hub");
                keyOrder.add("Splasher");
                keyOrder.add("Party");
                keyOrder.add("Location");
                keyOrder.add("Note");
                repeat = false;
            } catch (Exception e) {
                System.out.println("Server Connection Error: " + e.getMessage());
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (waitTime > 60000) continue;
                waitTime *= 2;
            }
            // Remove old splash notifications

        }

    }
}

