package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CHChests {

    public static List<String> RecentChatMessages = new ArrayList<>();
    public static Map<String, Long> ChestBlacklist = new HashMap<>();
    static HashMap<String, Long> listeningChests = new HashMap<>();
    static long lastMessageTime = 0;
    static boolean addMessages = false;


    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!event.world.getBlockState(event.pos).getBlock().getUnlocalizedName().contains("chest")) return;
        System.out.println(event.pos.toString());
        //System.out.println(Packets.hardstone);
        // Add the chest to the list of chests to listen for
        if (!Packets.hardstone.containsKey(event.pos.toString())) {
            listeningChests.put(event.pos.toString(), System.currentTimeMillis());
        } else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("hardstone chest detected"));
        }

    }

    public static void addChatMessage(String message) {
        if (System.currentTimeMillis() - lastMessageTime > 45 && addMessages) {
            parseChat();
            addMessages = false;
            return;
        }
        if (message.contains("You received")) {
            addMessages = true;
            RecentChatMessages.add(message);
            lastMessageTime = System.currentTimeMillis();
        }


    }

    public static void parseChat() {

        //listeningChests.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 9000);

        // Create a copy we'll remove entries too new to be valid from, and then use for calculations
        HashMap<String, Long> listeningChestsCopy = new HashMap<>();
        listeningChestsCopy.putAll(listeningChests);
        System.out.println(listeningChestsCopy);

        //listeningChestsCopy.values().removeIf(entry -> System.currentTimeMillis() - entry < 3000);
        System.out.println(listeningChestsCopy);
        if (listeningChestsCopy.isEmpty()) {
            RecentChatMessages.clear();
            return;
        }
        long oldest = Long.MAX_VALUE;
        String coords = null;

        Set<String> keys = listeningChestsCopy.keySet();
        for (String key : keys) {
            if (listeningChestsCopy.get(key) < oldest) {
                oldest = listeningChestsCopy.get(key);
                coords = key;
                System.out.println(System.currentTimeMillis() - oldest);
            }
        }
        System.out.println("oldest: " + (System.currentTimeMillis() - oldest));
        System.out.println("coords: " + coords);
        LoggerUtil.LOGGER.info("structure chest detected");
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("structure chest detected"));

        // Once a "You received" message is received, set the time remaining to 1 second for messages to come in
        long newTime = oldest + 8500;
        listeningChests.remove(coords);
        listeningChests.put(coords, newTime);
        System.out.println("recent chat messages: " + RecentChatMessages);

        for (String message : RecentChatMessages) {
            message = message.replaceAll("§.", "");
            System.out.println("message: " + message);
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("item found"));
            Pattern itemPattern = Pattern.compile("^You received +?(\\d+)(\\w*\\s*)*");
            Matcher matcher = itemPattern.matcher(message);

            while (matcher.find()) {
                String item = matcher.group(2);
                LoggerUtil.LOGGER.info(item);
                LoggerUtil.LOGGER.info("item found: " + item);
                //Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("item found: " + item));
            }
        }
        RecentChatMessages.clear();
    }
}
