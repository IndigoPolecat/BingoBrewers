package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CHChests {
    public static ArrayList<String> RecentChatMessages = new ArrayList<>();
    public static HashMap<Long, String> ChestBlacklist = new HashMap<>();
    boolean listening = false;
    long listeningStart;
    static HashMap<String, Long> listeningChests = new HashMap<>();
    static long lastMessageTime = 0;


    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!event.world.getBlockState(event.pos).getBlock().getUnlocalizedName().contains("chest")) return;
        System.out.println(event.pos.toString());

        listening = true;
        listeningChests.put(event.pos.toString(), System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        RecentChatMessages.add(event.message.getUnformattedText());
        if (RecentChatMessages.size() > 20) {
            RecentChatMessages.remove(0);
        }
        lastMessageTime = System.currentTimeMillis();

        Object[] ChestBlacklistArray = ChestBlacklist.values().toArray();

        for (int i = 0; i < ChestBlacklist.size(); i++) {
            String coords = (String) ChestBlacklistArray[i];
            // Try to remove blacklisted chests
            listeningChests.remove(coords);

        }


    }

    public static void parseChat(String message) {


        if (!message.contains("You received")) return;
        System.out.println("going1");
        listeningChests.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 9000);

        HashMap<String, Long> listeningChestsCopy = new HashMap<>(listeningChests);
        listeningChestsCopy.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() < 3000);
        long oldest = 0;
        String coords = null;
        if (listeningChestsCopy.isEmpty()) return;

        Object[] keys = listeningChestsCopy.keySet().toArray();
        for (int i = 0; i < listeningChestsCopy.size(); i++) {
            String key = (String) keys[i];
            long timestamp = listeningChestsCopy.get(key);
            if (oldest < timestamp) {
                oldest = timestamp;
                coords = key;
                System.out.println("oldest" + oldest);
            }
        }
        System.out.println("going4");
        /*Pattern coordPattern = Pattern.compile("\\{x=(\\d+), y=(\\d+), z=(\\d+)}");
        Matcher coordMatcher = coordPattern.matcher(coords);
        String x = null; String y = null; String z = null;
        while (coordMatcher.find()) {
            x = coordMatcher.group(1);
            y = coordMatcher.group(2);
            z = coordMatcher.group(3);
        }
        System.out.println("x: " + x + " y: " + y + " z: " + z);*/
        // Once a "You received" message is received, set the time remaining to 1 second for messages to come in
        long newTime = oldest + 8000;
        listeningChests.remove(coords);
        listeningChests.put(coords, newTime);
        //String message = event.message.toString().replaceAll("§.", "");
        System.out.println(message);
        Pattern itemPattern = Pattern.compile("^[You received] +?(\\d+)\\[\\w+(\\s+\\w+)*].");
        Matcher matcher = itemPattern.matcher(message);

        while (matcher.find()) {
            String item = matcher.group(2);
            System.out.println(item);
            System.out.println("item found");
        }
    }
}
