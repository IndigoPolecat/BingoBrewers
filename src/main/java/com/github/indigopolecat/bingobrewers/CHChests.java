package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.CHChestItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CHChests {

    public static List<String> RecentChatMessages = new ArrayList<>();
    public static ConcurrentHashMap<String, Long> listeningChests = new ConcurrentHashMap<>();
    static long lastMessageTime = 0;
    static boolean addMessages = false;
    private static long lastHardstoneChest = 0;
    private static boolean expectingHardstoneLoot;
    // Regex made with the help of Aerh
    public static Pattern ITEM_PATTERN = Pattern.compile("§[0-9a-fk-or]§[0-9a-fk-or]You received §[0-9a-fk-or](§[0-9a-fk-or])\\+?([\\d,]{1,5})\\s(?:§[0-9a-fk-or](§[0-9a-fk-or])*)?(?:.\\s)?(.+?)?(?:§[0-9a-fk-or])*\\.", Pattern.CASE_INSENSITIVE);
    // potentially store this as a constant in the server that is downloaded on launch


    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!event.world.getBlockState(event.pos).getBlock().getUnlocalizedName().contains("chest")) return;
        // Add the chest to the list of chests to listen for
        if (!Packets.hardstone.containsKey(event.pos.toString())) {
            listeningChests.put(event.pos.toString(), System.currentTimeMillis());
        } else {
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
        // remove old chests
        listeningChests.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 9000);

        // Create a copy we'll remove entries too new to be valid from, and then use for calculations
        HashMap<String, Long> listeningChestsCopy = new HashMap<>(listeningChests);

        // remove chests that were right clicked less than 4.8 seconds ago (temporarily)
        listeningChestsCopy.values().removeIf(entry -> System.currentTimeMillis() - entry < 4800);
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
            }
        }
        if (coords == null) return;

        LoggerUtil.LOGGER.info("structure chest detected");
        listeningChests.remove(coords);
        // add the opened chest to blacklist
        Packets.hardstone.put(coords, Long.MAX_VALUE);

        KryoNetwork.sendCHItems chestLoot = new KryoNetwork.sendCHItems();
        chestLoot.server = PlayerInfo.currentServer;
        // coords are in the format BlockPos{x=420, y=124, z=576}
        Pattern coordPattern = Pattern.compile("BlockPos\\{x=(\\d+), y=(\\d+), z=(\\d+)}");
        Matcher coordMatcher = coordPattern.matcher(coords);
        if (coordMatcher.find()) {
            chestLoot.x = Integer.parseInt(coordMatcher.group(1));
            chestLoot.y = Integer.parseInt(coordMatcher.group(2));
            chestLoot.z = Integer.parseInt(coordMatcher.group(3));
            System.out.println("Detected chest at " + chestLoot.x + ", " + chestLoot.y + ", " + chestLoot.z);
        } else {
            RecentChatMessages.clear();
            return;
        }

        messageLoop:
        for (String message : RecentChatMessages) {
            KryoNetwork.CHChestItem chestItem = new KryoNetwork.CHChestItem();


            Matcher matcher = ITEM_PATTERN.matcher(message);
            System.out.println(message);
            try {
                while (matcher.find()) {
                    System.out.println("match found");
                    chestItem.name = matcher.group(4);

                    chestItem.count = matcher.group(2).replaceAll(",", "");
                    System.out.println(chestItem.count);

                    Optional<String> numberColorGroup = Optional.ofNullable(matcher.group(1));
                    Optional<String> itemColorGroup = Optional.ofNullable(matcher.group(3));
                    chestItem.numberColor = numberColorGroup.map(BingoBrewers.minecraftColors::get).orElse(null);
                    chestItem.itemColor = itemColorGroup.map(BingoBrewers.minecraftColors::get).orElse(chestItem.numberColor);
                    System.out.println("number color: " + chestItem.numberColor);
                    System.out.println("item color: " + chestItem.itemColor);
                    if (chestItem.itemColor == null) continue messageLoop;
                }

                for (int i = 0; i < chestLoot.items.size(); i++) {
                    CHChestItem existingItem = chestLoot.items.get(i);
                    if (existingItem.name == null || chestItem.name == null) {
                        System.out.println(existingItem.name);
                        System.out.println(chestItem.name);
                        System.out.println("null item in chest");
                        continue;
                    }
                    if (existingItem.name.equals(chestItem.name)) {
                        existingItem.count = (Integer.parseInt(chestItem.count) + Integer.parseInt(existingItem.count)) + "";
                        continue messageLoop;
                    }
                }
                chestLoot.items.add(chestItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!chestLoot.items.isEmpty()) {
            ServerConnection serverConnection = new ServerConnection();
            serverConnection.sendCHItems(chestLoot);
        }
        RecentChatMessages.clear();
    }
}
