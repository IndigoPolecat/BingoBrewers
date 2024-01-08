package com.github.indigopolecat.bingobrewers;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.text.DecimalFormat;

public class bingoShop {
    boolean bingoShopOpen = false;
    boolean calculationsReady = false;
    ContainerChest containerChest;
    String itemName = null;
    String coinsPerPoint = null;
    int finalCostLineIndex;
    int finalExtraCostIndex;
    String finalExtraCost2;
    ArrayList<TooltipInfo> tooltipInfoList = new ArrayList<>();

    long lastRan;

    @SubscribeEvent
    public void onShopOpen(GuiOpenEvent event) {
        calculationsReady = false;
        bingoShopOpen = false;
        GuiChest guiChest;
        if (event.gui instanceof GuiChest) {
            guiChest = (GuiChest) event.gui;
            Container gui = guiChest.inventorySlots;

            if (gui instanceof ContainerChest) {
                containerChest = (ContainerChest) gui;
                String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
                if (name.equals("Bingo Shop")) {
                    // If everything has been calculated within the last 60 seconds, don't bother recalculating
                    if (System.currentTimeMillis() - lastRan < 60000) {
                        calculationsReady = true;
                        return;
                    }
                    tooltipInfoList.clear();
                    bingoShopOpen = true;
                }
            }
        }
    }


    @SubscribeEvent
        // Event that occurs after the last item in the chest is loaded, or 3 seconds later.
    public void onInitGuiPost(doneLoading.InventoryLoadingDoneEvent event) {
        if (bingoShopOpen) {
            System.out.println("Bingo Shop loaded!");
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
                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    String target = "Cost";
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
                            if (removeFormatting(itemLore.get(i)).equals(target)) {
                                costFound = true;
                            }
                        }
                    }
                }
            }
            if (costInt == 0) {
                System.out.println("Something went wrong: Bingo Point Cost not found in inventory named Bingo Shop!");
            }
            ArrayList<String> itemNamesFormatless = new ArrayList<>();
            for (String itemName : itemNames) {
                itemNamesFormatless.add(removeFormatting(itemName));
            }
            ArrayList<String> extraItemsFormatless = new ArrayList<>();
            for (String extraItem : extraItems) {
                extraItemsFormatless.add(removeFormatting(extraItem));
            }

            CompletableFuture<ArrayList<Double>> costFuture = auctionAPI.fetchPriceMap(itemNamesFormatless).whenComplete((lbinMap, throwable) -> {
            });
            costFuture.thenAccept(coinCosts -> {
                CompletableFuture<ArrayList<Double>> extraItemFuture = auctionAPI.fetchPriceMap(extraItemsFormatless).whenComplete((lbinMap, throwable) -> {
                });
                extraItemFuture.thenAccept(extraCoinCosts -> {

                    if (itemCosts.size() == coinCosts.size() && itemCosts.size() == itemNames.size()) {
                        DecimalFormat decimalFormat = new DecimalFormat("#,###");
                        String extraName = null;
                        Double extraCoinCost = null;
                        for (int i = 0; i < itemCosts.size(); i++) {
                            Double coinCost = coinCosts.get(i);

                            // Skip the item if coin cost is null (item not found in auction house b/c soulbound or other reasons.)
                            if (coinCost == (null)) {
                                continue;
                            }

                            if (extraCoinCosts.get(i) != null) {
                                extraCoinCost = extraCoinCosts.get(i);
                                coinCost = coinCost - extraCoinCost;
                                extraName = extraItemsFormatless.get(i);
                            }
                            int bingoCost = itemCosts.get(i);
                            itemName = itemNames.get(i);

                            if (coinCost == 0) {
                                System.out.println("Item not found in auction house or price is somehow 0: " + itemName);
                            } else if (bingoCost == 0) {
                                System.out.println("Failed to get Bingo Point cost of item: " + itemName);
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
                                            TooltipInfo tooltipInfo = new TooltipInfo(itemName, coinsPerPoint, finalExtraCost2, finalCostLineIndex, finalExtraCostIndex);
                                            tooltipInfoList.add(tooltipInfo);
                                        }
                                    }
                                }
                            }
                        }
                        lastRan = System.currentTimeMillis();
                    } else {
                        System.out.println("Something went wrong: itemCosts, coinCosts, and itemNames are not the same size!");
                    }
                });
            });

        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (calculationsReady) {
            for (TooltipInfo item : tooltipInfoList) {
                ItemStack eventItem = event.itemStack;
                if (eventItem.getDisplayName().equals(item.getName())) {
                    event.toolTip.add(item.getCostIndex() + 1, "§6" + item.getCost() + " Coins/Point");
                }
                if (item.getExtraCostIndex() != -1 && item.getExtraCost() != null && eventItem.getDisplayName().equals(item.getName())) {
                    String extraCostLine = event.toolTip.get(item.getExtraCostIndex());
                    event.toolTip.set(item.getExtraCostIndex(), extraCostLine + " §6(" + item.getExtraCost() + " Coins)");
                }
            }
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
}


