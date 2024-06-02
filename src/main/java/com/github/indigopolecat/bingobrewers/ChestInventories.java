package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Server;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import com.github.indigopolecat.bingobrewers.util.BingoShopItem;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChestInventories {
    public static int POINTS_PER_BINGO = 85; // default value, defined from server
    public static int POINTS_PER_BINGO_COMMUNITIES = 160; // default value, defined from server
    public static HashMap<Integer, Integer> rankPriceMap = new HashMap<>(); // default value, defined from server
    private static boolean calculationsReady = false;
    private static boolean bingoShopOpen = false;
    private static boolean hubSelectorOpen = false;
    private static boolean dungeonHubSelectorOpen = false;
    private static boolean shiftToggled = false;
    private static boolean waitingForLbinMap = true;
    private static long lastCalculated = 0;
    private static ContainerChest containerChest = null;
    private static int currentPoints = 0;
    private static int currentRank = 0;
    private static final ArrayList<BingoShopItem> shopItems = new ArrayList<>();
    public static ConcurrentHashMap<String, Integer> lbinMap = new ConcurrentHashMap<>();


    @SubscribeEvent
    public void onInventoryOpen(GuiOpenEvent event) {
        bingoShopOpen = false;
        hubSelectorOpen = false;
        dungeonHubSelectorOpen = false;
        shiftToggled = false;
        waitingForLbinMap = false;
        calculationsReady = false;
        if (event.gui instanceof GuiChest) {
            GuiChest chest = (GuiChest) event.gui;
            String inventoryName = getInventoryName(chest);
            containerChest = (ContainerChest) chest.inventorySlots;
            switch (inventoryName) {
                case "Bingo Shop":
                    if (System.currentTimeMillis() - lastCalculated < 300000) {
                        //calculationsReady = true;
                        break;
                    }
                    bingoShopOpen = true;
                    break;
                case "SkyBlock Hub Selector":
                    hubSelectorOpen = true;
                    break;
                case "Dungeon Hub Selector":
                    dungeonHubSelectorOpen = true;
                    break;
            }
        }

    }

    @SubscribeEvent
    public void onInventoryDoneLoading(Packets.InventoryLoadingDoneEvent event) {
        if (bingoShopOpen) {
            shopItems.clear();
            List<ItemStack> chestInventory = containerChest.getInventory();
            chestInventory.subList(0, chestInventory.size() - 36);
            for (ItemStack item : chestInventory) {
                if (item != null) {
                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    if (item.getDisplayName().contains("Bingo Rank")) {
                        currentRank = getCurrentBingoRank(item);
                        currentPoints = getCurrentBingoPoints(item);
                    } else {
                        for (String s : itemLore) {
                            if (removeFormatting(s).equals("Cost")) {
                                BingoShopItem shopItem = new BingoShopItem(item);
                                shopItems.add(shopItem);
                            }
                        }
                    }

                }
            }
            ArrayList<String> itemPriceQuery = getItemPriceQuery();
            ServerConnection.requestLbin(itemPriceQuery);
            waitingForLbinMap = true;

        } else if (hubSelectorOpen || dungeonHubSelectorOpen) {
            List<ItemStack> chestInventory = containerChest.getInventory();
            // Remove the last 36 slots in the chest inventory, which are the player inventory
            chestInventory.subList(chestInventory.size() - 36, chestInventory.size()).clear();

            // loop through the items in the chest
            for (ItemStack item : chestInventory) {
                // verify the item slot isn't empty
                if (item != null) {
                    // Get the lore of the item
                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    String hubNumber = null;
                    // loop through the lore lines of the item
                    for (String s : itemLore) {
                        // look for the lore line that contains the hub number
                        if (s.contains("Hub #")) {
                            // Match the hub number and remove formatting codes
                            hubNumber = s.replaceAll("SkyBlock Hub #(\\d+)", "$1");
                            hubNumber = hubNumber.replaceAll("Dungeon Hub #(\\d+)", "$1");
                            hubNumber = removeFormatting(hubNumber);
                        } else if (s.contains("Server:")) { // Look for the lore line containing the server id
                            // Match the server id and remove formatting codes
                            String server = s.replaceAll("Server: (.+)", "$1");
                            server = removeFormatting(server);

                            // if we're in a hub selector, add the server and hub number to the hubServerMap
                            if (hubNumber != null && hubSelectorOpen) {
                                PlayerInfo.hubServerMap.put(server, hubNumber);
                            } else if (hubNumber != null && dungeonHubSelectorOpen) { // if we're in a dungeon hub selector, add the server and hub number to the dungeonHubServerMap
                                PlayerInfo.dungeonHubServerMap.put(server, hubNumber);
                            }
                        }
                    }

                }
            }
        }
    }

    private static ArrayList<String> getItemPriceQuery() {
        ArrayList<String> itemPriceQuery = new ArrayList<>();
        HashMap<String, String> itemMap = new HashMap<>();
        // map item names to ids
        for (BingoShopItem item : shopItems) {
            if (item.soulbound) continue;
            itemMap.put(item.itemName, item.itemID);
        }
        for (BingoShopItem item : shopItems) {
            if (item.soulbound) continue;
            if (!item.extraCost.isEmpty()) {
                for (String s : item.extraCost) {
                    if (itemMap.containsKey(s)) {
                        itemPriceQuery.add(itemMap.get(s));
                    }
                }
            }
            itemPriceQuery.add(item.itemID);
        }
        return itemPriceQuery;
    }


    public static void appendRemainingLore(ItemTooltipEvent event, BingoShopItem item) {
        if(currentPoints == -1 || !BingoBrewersConfig.displayMissingBingoPoints || !BingoBrewersConfig.displayMissingBingoes) {
            return;
        }
        //int toolTipIndex = item.itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).tagCount() - 3;
        int toolTipIndex = item.itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).tagCount() - 1;
        int pointsPerBingo;
        // controls the text in the toggle to switch
        String withOrWithout = "";
        String withOrWithout2 = POINTS_PER_BINGO_COMMUNITIES + "";
        if (shiftToggled) {
            withOrWithout = " (Communities)";
            pointsPerBingo = POINTS_PER_BINGO_COMMUNITIES;
            withOrWithout2 = POINTS_PER_BINGO + "";
        } else {
            pointsPerBingo = POINTS_PER_BINGO;
        }
        int i = currentRank;
        int rankCost = 0;
        while (i < item.bingoRankRequired) {
            i++;
            if (rankPriceMap.containsKey(i)) {
                rankCost += rankPriceMap.get(i);
            }
        }
        DecimalFormat df = new DecimalFormat("#.##");

        int itemCostInBingoPoints = item.costInPoints + rankCost;
        int pointsLeftToAffordItem = itemCostInBingoPoints - currentPoints;
        String bingoesRequired = df.format( pointsLeftToAffordItem / pointsPerBingo);

        if (pointsLeftToAffordItem > 0 && BingoBrewersConfig.displayMissingBingoPoints) {
            event.toolTip.add(0, "");
            event.toolTip.add(0, "§7Points Missing: §c" + pointsLeftToAffordItem);
            toolTipIndex++;
        }

        if (Double.parseDouble(bingoesRequired) > 0 && BingoBrewersConfig.displayMissingBingoes) {
            event.toolTip.add("§8[SHIFT FOR " + withOrWithout2 + " POINTS/BINGO]");
            event.toolTip.add("§7Events Remaining" + withOrWithout + ": §c" + bingoesRequired);
        }

    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (waitingForLbinMap) return;
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        for (BingoShopItem item : shopItems) {
            if (!item.itemName.equals(removeFormatting(event.itemStack.getDisplayName()))) continue;
            if (!item.soulbound) {
                if (!item.extraCost.isEmpty()) {
                    int totalCost = 0;
                    for (String s : item.extraCost) {
                        System.out.println("string: " + s);
                        System.out.println(lbinMap.toString());
                        if (lbinMap.containsKey(s)) {
                            totalCost += lbinMap.get(s);
                            item.extraCostMap.put(s, lbinMap.get(s));
                        }
                    }
                    item.costInCoins = totalCost;
                }
                if (lbinMap.containsKey(item.itemID)) {
                    item.costInCoins += lbinMap.get(item.itemID);
                }

                ItemStack itemStack = item.itemStack;
                NBTTagCompound nbt = itemStack.getTagCompound();
                NBTTagCompound displayTag = nbt.getCompoundTag("display");
                NBTTagList loreList = displayTag.getTagList("Lore", 8);

                for (int i = 0; i < item.extraCostIndex.size(); i++) {
                    String lore = loreList.getStringTagAt(item.extraCostIndex.get(i));
                    System.out.println("lore " + lore);
                    System.out.println("extraCostIndex " + item.extraCostIndex.get(i));
                    System.out.println("extraCost " + item.extraCost.get(i));
                    System.out.println("Extra Cost Map: " + item.extraCostMap.toString());
                    System.out.println("extraCostItem " + item.extraCostMap.get(item.extraCost.get(i)));
                    System.out.println("extraCostIndex " + (item.extraCostIndex.get(i) + 1));
                    System.out.println(event.toolTip);
                    event.toolTip.add(item.extraCostIndex.get(i) + 1, lore + "§6(" + formatNumber(item.extraCostMap.get(item.extraCost.get(i))) + ")");
                    if (i == item.extraCostIndex.size() - 1) {
                        event.toolTip.add(item.extraCostIndex.get(i) + 2, "§6" + decimalFormat.format(item.costInCoins / item.costInPoints) + " Coins/Point");
                    }
                }
            }
            appendRemainingLore(event, item);
        }
    }

    public static String getInventoryName (GuiChest chest) {
        ContainerChest containerChest = (ContainerChest) chest.inventorySlots;
        return containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();

    }

    public static int getCurrentBingoPoints(ItemStack item) {
        List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        Pattern pointPattern = Pattern.compile("Available Bingo Points: §.([0-9]+)");
        for (String s : itemLore) {
            s = s.replaceAll(",", "");
            Matcher pointMatcher = pointPattern.matcher(s);
            if (pointMatcher.find()) {
                return Integer.parseInt(pointMatcher.group(1));
            }
        }
        return -1;
    }

    public static int getCurrentBingoRank(ItemStack item) {
        List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        Pattern rankPattern = Pattern.compile("Your Rank: §.* (Bingo Rank ([IVXLCDM]+))");
        for (String s : itemLore) {
            Matcher rankMatcher = rankPattern.matcher(s);
            if (rankMatcher.find()) {
                String rank = rankMatcher.group(1);
                switch (rank) {
                    case "I":
                        return 1;
                    case "II":
                        return 2;
                    case "III":
                        return 3;
                    case "IV":
                        return 4;
                    case "V":
                        return 5;
                    case "VI":
                        return 6;
                    case "VII":
                        return 7;
                    case "VIII":
                        return 8;
                    case "IX":
                        return 9;
                    case "X":
                        return 10;
                }
            }
        }
        return 0;
    }

    private static String removeFormatting(String s) {
        String news = s.replaceAll("§.", "");
        if (news.endsWith(" Bingo Points")) {
            news = news.substring(0, news.length() - 13);
        }
        return news;
    }

    public static String formatNumber(long number) {
        if (number < 1_000) {
            return String.valueOf(number);
        } else {
            String pattern;
            double value;

            if (number < 1_000_000) {
                pattern = "#.#k";
                value = number / 1_000.0;
            } else {
                pattern = "#.#M";
                value = number / 1_000_000.0;
            }

            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            return decimalFormat.format(value);
        }
    }

    @SubscribeEvent
    public void onKeyPress(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if((Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))  && ChestInventories.bingoShopOpen ) {
            shiftToggled = !shiftToggled;
        }

    }






}
