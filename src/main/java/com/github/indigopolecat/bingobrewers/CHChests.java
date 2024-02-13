package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CHChests {
    public static ArrayList<String> RecentChatMessages = new ArrayList<>();
    public static HashMap<Long, String> ChestBlacklist = new HashMap<>();
    static HashMap<String, Long> listeningChests = new HashMap<>();
    static long lastMessageTime = 0;
    static boolean addMessages = false;


    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!event.world.getBlockState(event.pos).getBlock().getUnlocalizedName().contains("chest")) return;
        System.out.println(event.pos.toString());
        // Add the chest to the list of chests to listen for
        listeningChests.put(event.pos.toString(), System.currentTimeMillis());
    }

    public static void addChatMessage(String message) {
        if (System.currentTimeMillis() - lastMessageTime > 45 && addMessages) {
            parseChat();
            addMessages = false;
            return;
        }
        if (!message.contains("You received")) return;
        addMessages = true;
        RecentChatMessages.add(message);
        lastMessageTime = System.currentTimeMillis();


    }

    public static void parseChat() {

        System.out.println("chat messages done");
        Object[] ChestBlacklistArray = ChestBlacklist.values().toArray();

        for (int i = 0; i < ChestBlacklist.size(); i++) {
            String key = (String) ChestBlacklistArray[i];
            // Try to remove blacklisted chests
            listeningChests.remove(key);
        }

        listeningChests.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 9000);

        // Create a copy we'll remove entries too new to be valid from, and then use for calculations
        HashMap<String, Long> listeningChestsCopy = new HashMap<>(listeningChests);
        listeningChestsCopy.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() < 3000);
        if (listeningChestsCopy.isEmpty()) {
            RecentChatMessages.clear();
            return;
        }
        long oldest = 0;
        String coords = null;

        Set<String> keys = listeningChestsCopy.keySet();
        for (String key : keys) {
            if (listeningChestsCopy.get(key) > oldest) {
                oldest = listeningChestsCopy.get(key);
                coords = key;
                System.out.println(System.currentTimeMillis() - oldest);
            }
        }
        System.out.println("going4");

        // Once a "You received" message is received, set the time remaining to 1 second for messages to come in
        long newTime = oldest + 8500;
        listeningChests.remove(coords);
        listeningChests.put(coords, newTime);
        //String message = event.message.toString().replaceAll("§.", "");
        for (String message : RecentChatMessages) {
            message = removeFormatting(message);
            Pattern itemPattern = Pattern.compile("^[You received] +?(\\d+)\\[\\w+(\\s+\\w+)*].");
            Matcher matcher = itemPattern.matcher(message);

            while (matcher.find()) {
                String item = matcher.group(2);
                System.out.println(item);
                System.out.println("item found");
            }
        }
        RecentChatMessages.clear();
    }

    public static String removeFormatting(String message) {
        return message.replaceAll("§.", "");
    }
}
