package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.events.Packets;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.CHChestItem;
import net.minecraft.client.Minecraft;
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
    public static boolean addMessages = false;
    private static long lastHardstoneChest = 0;
    public static boolean expectingHardstoneLoot;
    public static HashMap<String, ArrayList<String>> visitedChests = new HashMap<>();
    public static String regex = "^§[0-9a-fk-or]\\s+(§[0-9a-fk-or])+(.\\s)?([\\w\\s]+?)(\\s§[0-9a-fk-or]§[0-9a-fk-or]x([\\d,]{1,5}))?§[0-9a-fk-or]";
    public static int itemNameRegexGroup = 3;
    public static int itemCountRegexGroup = 5;
    public static int itemNameColorRegexGroup = 1;
    public static String signalLootChatMessage = "§r  §r§5§lLOOT CHEST COLLECTED §r";
    public static String signalLootChatMessageEnd = "§r§d§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r";
    public static Pattern ITEM_PATTERN = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    // potentially store this as a constant in the server that is downloaded on launch


    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!event.world.getBlockState(event.pos).getBlock().getUnlocalizedName().contains("chest")) return;
        if (!BingoBrewersConfig.crystalHollowsWaypointsToggle) return;
        // Add the chest to the list of chests to listen for
        if (!Packets.hardstone.containsKey(event.pos.toString())) {
            ArrayList<String> lobbyVisitedChests = visitedChests.get(PlayerInfo.currentServer);
            if (lobbyVisitedChests == null) {
                lobbyVisitedChests = new ArrayList<>();
            }
            if (!lobbyVisitedChests.contains(event.pos.getX() + event.pos.getY() + event.pos.getZ() + "")) {
                listeningChests.put(event.pos.toString(), System.currentTimeMillis());

                lobbyVisitedChests.add(event.pos.getX() + event.pos.getY() + event.pos.getZ() + "");
                visitedChests.put(PlayerInfo.currentServer, lobbyVisitedChests);
            }
        } else {
            lastHardstoneChest = System.currentTimeMillis();
            expectingHardstoneLoot = true;
        }

    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (System.currentTimeMillis() - lastHardstoneChest > 500) expectingHardstoneLoot = false;
    }

    public static void addChatMessage(String message) {
        if (!expectingHardstoneLoot) {
            addMessages = true;
            RecentChatMessages.add(message);
        }
        lastMessageTime = System.currentTimeMillis();


    }

    public static void parseChat() {
        // remove old chests
        listeningChests.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 9000);

        // Create a copy we'll remove entries too new to be valid from, and then use for calculations
        HashMap<String, Long> listeningChestsCopy = new HashMap<>(listeningChests);

        for(Long time : listeningChestsCopy.values()) {
            if (System.currentTimeMillis() - time < 3700) {
                System.out.println("Please report this message to indigo_polecat (CHChests beta 0.3+ logging for wrong timing on chest open) time to open: " + (System.currentTimeMillis() - time) + " Expected greater than 4200");
            }
        }

        // remove chests that were right clicked less than 3.7 seconds ago (temporarily)
        listeningChestsCopy.values().removeIf(entry -> System.currentTimeMillis() - entry < 3700);
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

        listeningChests.remove(coords);
        // add the opened chest to blacklist
        Packets.hardstone.put(coords, Long.MAX_VALUE);

        KryoNetwork.ClientSendCHItems chestLoot = new KryoNetwork.ClientSendCHItems();
        chestLoot.server = PlayerInfo.currentServer;
        chestLoot.day = (int) Minecraft.getMinecraft().theWorld.getWorldTime() / 24000;
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
            try {
                if (matcher.find()) {
                    chestItem.name = matcher.group(itemNameRegexGroup);
                    if (chestItem.name == null) {
                        System.out.println("skipping, name is null");
                    }

                    chestItem.count = matcher.group(itemCountRegexGroup);
                    if (chestItem.count == null) {
                        chestItem.count = "1";
                    } else {
                        chestItem.count = chestItem.count.replaceAll(",", "");
                    }

                    Optional<String> itemColorGroup = Optional.ofNullable(matcher.group(itemNameColorRegexGroup));
                    chestItem.itemColor = itemColorGroup.map(BingoBrewers.minecraftColors::get).orElse(chestItem.numberColor);
                    if (chestItem.itemColor == null) continue messageLoop;
                } else {
                    continue messageLoop;
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
            ServerConnection.sendTCP(chestLoot);
        }
        RecentChatMessages.clear();
    }
}
