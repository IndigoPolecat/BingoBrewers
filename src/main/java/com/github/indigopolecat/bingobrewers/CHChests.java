package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CHChests {

    public static List<String> RecentChatMessages = new ArrayList<>();
    static HashMap<String, Long> listeningChests = new HashMap<>();
    static long lastMessageTime = 0;
    static boolean addMessages = false;
    private static long lastHardstoneChest = 0;
    private static boolean expectingHardstoneLoot;


    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!event.world.getBlockState(event.pos).getBlock().getUnlocalizedName().contains("chest")) return;
        System.out.println(event.pos.toString());
        System.out.println(Packets.hardstone);
        // Add the chest to the list of chests to listen for
        if (!Packets.hardstone.containsKey(event.pos.toString())) {
            listeningChests.put(event.pos.toString(), System.currentTimeMillis());
        } else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("hardstone chest detected"));
            lastHardstoneChest = System.currentTimeMillis();
            expectingHardstoneLoot = true;
        }

    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (System.currentTimeMillis() - lastHardstoneChest > 500) expectingHardstoneLoot = false;
        if (System.currentTimeMillis() - lastMessageTime > 100 && addMessages) {
            parseChat();
            addMessages = false;
            expectingHardstoneLoot = false;
        }
    }

    public static void addChatMessage(String message) {
        if (message.contains("You received")) {
            if (!expectingHardstoneLoot) {
                addMessages = true;
                RecentChatMessages.add(message);
            }
            lastMessageTime = System.currentTimeMillis();
        }


    }

    public static void parseChat() {

        listeningChests.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 7000);

        // Create a copy we'll remove entries too new to be valid from, and then use for calculations
        HashMap<String, Long> listeningChestsCopy = new HashMap<>();
        listeningChestsCopy.putAll(listeningChests);
        System.out.println(listeningChestsCopy);

        listeningChestsCopy.values().removeIf(entry -> System.currentTimeMillis() - entry < 4800);
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
        if (coords == null) return;

        LoggerUtil.LOGGER.info("structure chest detected");
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("structure chest detected"));

        // don't need this anymore but idk what I can remove so it stays cuz it doesn't matter
        // Once a "You received" message is received, set the time remaining to 1 second for messages to come in
        long newTime = oldest + 8500;
        listeningChests.remove(coords);
        listeningChests.put(coords, newTime);
        // add the opened chest to blacklist
        Packets.hardstone.put(coords, Long.MAX_VALUE);
        System.out.println("recent chat messages: " + RecentChatMessages);

        KryoNetwork.sendCHItems chestLoot = new KryoNetwork.sendCHItems();
        chestLoot.server = PlayerInfo.currentServer;
        // coords are in the format BlockPos{x=420, y=124, z=576}
        Pattern coordPattern = Pattern.compile("BlockPos\\{x=(\\d+), y=(\\d+), z=(\\d+)}");
        Matcher coordMatcher = coordPattern.matcher(coords);
        if (coordMatcher.find()) {
            chestLoot.x = Integer.parseInt(coordMatcher.group(1));
            chestLoot.y = Integer.parseInt(coordMatcher.group(2));
            chestLoot.z = Integer.parseInt(coordMatcher.group(3));
        } else {
            RecentChatMessages.clear();
            return;
        }

        for (String message : RecentChatMessages) {
            message = message.replaceAll("§.", "");

            Pattern itemPattern = Pattern.compile("^You received \\+?([\\d,]+) .?\\s?\\b([\\w*\\s]*)");
            Matcher matcher = itemPattern.matcher(message);

            while (matcher.find()) {
                String item = matcher.group(2);
                String amount = matcher.group(1).replaceAll(",", "");
                LoggerUtil.LOGGER.info("item found: " + item);
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("item found: " + amount + " " + item));
                if (!amount.equals("1")) {
                    item = amount + "x" + item;
                }
                chestLoot.items.add(item);
            }
        }
        if (!chestLoot.items.isEmpty()) {
            ServerConnection serverConnection = new ServerConnection();
            serverConnection.sendCHItems(chestLoot);
        }
        RecentChatMessages.clear();
    }
}
