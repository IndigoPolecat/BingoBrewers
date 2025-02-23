package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import com.github.indigopolecat.events.Packets;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.lwjgl.input.Keyboard;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.indigopolecat.bingobrewers.Hud.SplashInfoHud.activeSplashes;

public class ChestInventories {
    public static final int POINTS_PER_BINGO = 85;
    public static boolean bingoShopOpen = false;
    boolean calculationsReady = false;
    ContainerChest containerChest;
    String itemName = null;
    String coinsPerPoint = null;
    int finalCostLineIndex;
    int finalExtraCostIndex;
    String finalExtraCost2;
    ArrayList<TooltipInfo> tooltipInfoList = new ArrayList<>();
    long lastRan;
    boolean hubSelectorOpen = false;
    boolean dungeonHubSelectorOpen = false;
    private int currentBingoPoints;
    public static boolean shiftPressed = false;
    public static HashMap<Integer, Integer> rankPriceMap = new HashMap<>();
    public static int bingoRank;

    @SubscribeEvent
    public void onShopOpen(GuiOpenEvent event) {
        calculationsReady = false;
        bingoShopOpen = false;
        shiftPressed = false;
        GuiChest guiChest;
        if (event.gui instanceof GuiChest) {
            guiChest = (GuiChest) event.gui;
            Container gui = guiChest.inventorySlots;
            if (gui instanceof ContainerChest) {
                containerChest = (ContainerChest) gui;
                String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
                switch (name) {
                    case "Bingo Shop":
                        if (!BingoBrewersConfig.showCoinsPerBingoPoint) return;
                        // If everything has been calculated within the last 60 seconds, don't bother recalculating
                        if (System.currentTimeMillis() - lastRan < 120000) {
                            calculationsReady = true;
                            return;
                        }
                        tooltipInfoList.clear();
                        bingoShopOpen = true;
                        break;
                    case "SkyBlock Hub Selector":
                        LoggerUtil.LOGGER.info("Hub Selector Open");
                        hubSelectorOpen = true;
                        break;
                    case "Dungeon Hub Selector":
                        LoggerUtil.LOGGER.info("Dungeon Hub Selector Open");
                        dungeonHubSelectorOpen = true;
                        break;
                }
            }
        }
    }

    // Event that occurs once a packet from your inventory instead of the chest is sent, meaning the chest is loaded
    @SubscribeEvent
    public void onInitGuiPost(Packets.InventoryLoadingDoneEvent event) {
        if (bingoShopOpen) {
            // set variables in correct scope
            String cost;
            int costInt = 0;
            ArrayList<String> itemNames = new ArrayList<>();
            ArrayList<Integer> itemCosts = new ArrayList<>();
            ArrayList<String> extraItems = new ArrayList<>();
            List<ItemStack> chestInventory = containerChest.getInventory();
            // Remove the last 36 slots in the chest inventory, which are the player inventory
            chestInventory.subList(chestInventory.size() - 36, chestInventory.size()).clear();

            for (ItemStack item : chestInventory) {
                if (item != null) {
                    if (item.getDisplayName().contains("Upgrade Bingo Rank")) {
                        this.currentBingoPoints = gatherBingoPoints(item);
                        bingoRank = extractRankAsInt(item);
                    }

                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    boolean costFound = false;
                    for (int i = 0; i < itemLore.size(); i++) {
                        String extraItem = null;
                        // if the previous lore line was "Cost", set this line to the cost variable and break the loop
                        if (costFound) {
                            cost = itemLore.get(i);
                            String unformattedCost = removeFormatting(cost);

                            // if the next lore line is not empty, set it to the extra item variable, doesn't work for multiple extra items in the cost
                            if (!itemLore.get(i + 1).equals("")) {
                                extraItem = itemLore.get(i + 1);
                            }
                            try {
                                costInt = Integer.parseInt(unformattedCost);
                            } catch (NumberFormatException e) {
                                System.out.println("Cost is not a number!");
                            }
                            itemCosts.add(costInt);
                            extraItems.add(extraItem);
                            String displayName = item.getDisplayName();
                            itemNames.add(displayName);
                            break;
                        } else {
                            // if lore line is "§5§o§7Cost"
                            if (removeFormatting(itemLore.get(i)).equals("Cost")) {
                                costFound = true;
                            }
                        }
                    }
                }
            }
            if (costInt == 0) {
                LoggerUtil.LOGGER.info("Something went wrong: Bingo Point Cost not found in inventory named Bingo Shop!");
            }
            ArrayList<String> itemNamesFormatless = new ArrayList<>();
            for (String itemName : itemNames) {
                itemNamesFormatless.add(removeFormatting(itemName));
            }
            ArrayList<String> extraItemsFormatless = new ArrayList<>();
            for (String extraItem : extraItems) {
                extraItemsFormatless.add(removeFormatting(extraItem));
            }

            // Fetch the price map for all items in the chest with a cost, execute the rest of the code after
            CompletableFuture<ArrayList<Double>> costFuture = AuctionAPI.fetchPriceMap(itemNamesFormatless).whenComplete((lbinMap, throwable) -> {
            });
            costFuture.thenAccept(coinCosts -> {
                CompletableFuture<ArrayList<Double>> extraItemFuture = AuctionAPI.fetchPriceMap(extraItemsFormatless).whenComplete((lbinMap, throwable) -> {
                });
                extraItemFuture.thenAccept(extraCoinCosts -> {

                    if (itemCosts.size() == coinCosts.size() && itemCosts.size() == itemNames.size()) {
                        DecimalFormat decimalFormat = new DecimalFormat("#,###");
                        String extraName = null;
                        Double extraCoinCost = null;
                        tooltipInfoList.clear();
                        for (int i = 0; i < itemCosts.size(); i++) {
                            Double coinCost = coinCosts.get(i);

                            // Skip the item if coin cost is null (item not found in auction house b/c soulbound or other reasons.)
                            if (coinCost == (null)) {
                                continue;
                            }

                            // If there is an extra item, subtract the cost of the extra item from the total cost
                            if (extraCoinCosts.get(i) != null) {
                                extraCoinCost = extraCoinCosts.get(i);
                                coinCost = coinCost - extraCoinCost;
                                extraName = extraItemsFormatless.get(i);
                            }
                            int bingoCost = itemCosts.get(i);
                            itemName = itemNames.get(i);

                            if (coinCost == 0) {
                                LoggerUtil.LOGGER.info("Item not found in auction house or price is somehow 0: " + itemName);
                            } else if (bingoCost == 0) {
                                LoggerUtil.LOGGER.info("Failed to get Bingo Point cost of item: " + itemName);
                            } else {
                                double coinsPerPointdouble = coinCost / bingoCost;
                                long coinsPerPointLong = Math.round(coinsPerPointdouble);
                                coinsPerPoint = decimalFormat.format(coinsPerPointLong);

                                for (ItemStack item : chestInventory) {
                                    if (item != null) {
                                        String displayName = item.getDisplayName();

                                        // compare the display name of the item in the chest loop to the item name in the name array (aka the current one we are calculating for)
                                        if (displayName.equals(itemName)) {
                                            NBTTagCompound nbt = item.getTagCompound();
                                            NBTTagCompound displayTag = nbt.getCompoundTag("display");
                                            NBTTagList loreList = displayTag.getTagList("Lore", 8);

                                            int costLineIndex = -1;
                                            int extraCostIndex = -1;
                                            int bingoRankRequired = extractRankAsInt(item);
                                            for (int j = 0; j < loreList.tagCount(); j++) {
                                                // Compare the current line without formatting to the cost in bingo points
                                                // removeFormatting method removes " Bingo Points" from the end of the string
                                                if (removeFormatting(loreList.getStringTagAt(j)).equals(Integer.toString(bingoCost))) {
                                                    costLineIndex = j + 1;

                                                    if (extraName != null && extraName.equals(removeFormatting(loreList.getStringTagAt(j + 1)))) {
                                                        extraCostIndex = j + 2;
                                                        costLineIndex += 1;
                                                    }
                                                }
                                            }
                                            // If no empty line is found after the cost line, set the cost line index to the end of the lore list
                                            if (costLineIndex == -1) {
                                                // Add one because the tooltip list used to add the line includes the display name and is 1 longer as a result
                                                costLineIndex = loreList.tagCount() + 1;
                                            }

                                            finalCostLineIndex = costLineIndex;
                                            finalExtraCostIndex = extraCostIndex;
                                            String finalExtraCost = null;
                                            if (extraCoinCost != null) {
                                                finalExtraCost = formatNumber(Math.round(extraCoinCost));
                                            }
                                            finalExtraCost2 = finalExtraCost;
                                            calculationsReady = true;
                                            TooltipInfo tooltipInfo = new TooltipInfo(itemName, coinsPerPoint, finalExtraCost2, finalCostLineIndex, finalExtraCostIndex, bingoCost, bingoRankRequired);
                                            tooltipInfoList.add(tooltipInfo);
                                        }
                                    }
                                }
                            }
                        }
                        lastRan = System.currentTimeMillis();
                    } else {
                        LoggerUtil.LOGGER.info("Something went wrong: itemCosts, coinCosts, and itemNames are not the same size!");
                    }
                });
            });

        } else if (hubSelectorOpen || dungeonHubSelectorOpen) {
            List<ItemStack> chestInventory = containerChest.getInventory();
            // Remove the last 36 slots in the chest inventory, which are the player inventory
            chestInventory.subList(chestInventory.size() - 36, chestInventory.size()).clear();

            // TODO: Change this to just directly update the hub # based on known server ids
            // loop through the items in the chest
            for (ItemStack item : chestInventory) {
                // verify the item slot isn't empty
                if (item != null) {
                    // Get the lore of the item
                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    String hubNumber = null;
                    String server = null;
                    // loop through the lore lines of the item
                    for (String s : itemLore) {
                        // look for the lore line that contains the hub number
                        if (s.contains("Hub #")) {
                            // Match the hub number and remove formatting codes
                            hubNumber = s.replaceAll("SkyBlock Hub #(\\d+)", "$1");
                            hubNumber = hubNumber.replaceAll("Dungeon Hub #(\\d+)", "$1");
                            hubNumber = removeFormatting(hubNumber);
                        } else if (s.contains("Server:") && hubNumber != null) { // Look for the lore line containing the server id, but if the hub number hasn't been set yet ignore
                            // Match the server id and remove formatting codes
                            server = s.replaceAll("Server: (.+)", "$1");
                            server = removeFormatting(server);

                        }
                        // if we're in a hub selector, add the server and hub number to the hubServerMap
                        if (hubNumber != null && server != null && hubSelectorOpen) {
                            for (SplashNotificationInfo info : activeSplashes) {
                                if (info.hubNumber.equals(hubNumber) && info.serverID.equalsIgnoreCase(server)) {

                                }
                            }
                            PlayerInfo.hubServerMap.put(server, hubNumber);
                        } else if (hubNumber != null && server != null && dungeonHubSelectorOpen) { // if we're in a dungeon hub selector, add the server and hub number to the dungeonHubServerMap
                            PlayerInfo.dungeonHubServerMap.put(server, hubNumber);
                        }
                    }
                }
            }
        }
    }

    private int gatherBingoPoints(ItemStack item) {
        List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        for (String s : itemLore) {
            if (s.contains("Available Bingo Points: ")) {
                return extractAvailableBingoPoints(s);
            }
        }
        return -1;
    }

    public static int extractAvailableBingoPoints(String s) {
        // If your Bingo Points are above 1000 the number will be formatted with a comma, so we remove it
        String bingoPointsString = s.replace("Available Bingo Points: ", "").replace(",", "");
        return Integer.parseInt(bingoPointsString.replaceAll("§.", ""));
    }

    public static int extractRankAsInt(ItemStack item) {
        List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        Pattern pattern = Pattern.compile("(Bingo Rank [IV]+)");
        for (String s : itemLore) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                s = matcher.group(1).replaceAll("§.", "");
                switch (s) {
                    case "Bingo Rank I":
                        return 1;
                    case "Bingo Rank II":
                        return 2;
                    case "Bingo Rank III":
                        return 3;
                    case "Bingo Rank IV":
                        return 4;
                    case "Bingo Rank V":
                        return 5;
                }
            }
        }

        return 0;
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (calculationsReady) {
            for (TooltipInfo item : tooltipInfoList) {
                ItemStack eventItem = event.itemStack;
                if (eventItem.getDisplayName().equals(item.getName())) {
                    event.toolTip.add(item.getCostIndex() + 1, "§6" + item.getCost() + " Coins/Point");
                    appendPointsAndBingoLeft(event, item);
                }
                if (item.getExtraCostIndex() != -1 && item.getExtraCost() != null && eventItem.getDisplayName().equals(item.getName())) {
                    String extraCostLine = event.toolTip.get(item.getExtraCostIndex());
                    event.toolTip.set(item.getExtraCostIndex(), extraCostLine + " §6(" + item.getExtraCost() + " Coins)");
                }
            }
        }
    }

    private void appendPointsAndBingoLeft(ItemTooltipEvent event, TooltipInfo item) {
        if(this.currentBingoPoints == -1 || !BingoBrewersConfig.displayMissingBingoPoints || !BingoBrewersConfig.displayMissingBingoes) {
            return;
        }
        int pointsPerBingo;
        // controls the text in the toggle to switch
        String withOrWithout = "";
        String withOrWithout2 = "160";
        if (shiftPressed) {
            withOrWithout = " (Communities)";
            pointsPerBingo = 160;
            withOrWithout2 = "85";
        } else {
            pointsPerBingo = POINTS_PER_BINGO;
        }
        int i = bingoRank;
        int rankCost = 0;
        while (i < item.getBingoRankRequired()) {
            i++;
            if (rankPriceMap.containsKey(i)) {
                 rankCost += rankPriceMap.get(i);
            }
        }
        int itemCostInBingoPoints = item.getBingoPointsPrice() + rankCost;
        int pointsLeftToAffordItem = itemCostInBingoPoints - this.currentBingoPoints;
        int bingoesRequired = (int) Math.ceil((double) pointsLeftToAffordItem / pointsPerBingo);
        int toolTipIndex = item.getCostIndex() + 2;

        if (pointsLeftToAffordItem > 0 && BingoBrewersConfig.displayMissingBingoPoints) {
            event.toolTip.add(toolTipIndex, "");
            event.toolTip.add(toolTipIndex + 1,"§7Points Missing: §c" + pointsLeftToAffordItem);
            toolTipIndex++;
        }

        if (bingoesRequired > 0 && BingoBrewersConfig.displayMissingBingoes) {
            event.toolTip.add(toolTipIndex + 1,"§7Events Remaining" + withOrWithout + ": §c" + bingoesRequired);
            event.toolTip.add(toolTipIndex, "§8[SHIFT FOR " + withOrWithout2 + " POINTS/BINGO]");
        }
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
            shiftPressed = !shiftPressed;
        }

    }
}


