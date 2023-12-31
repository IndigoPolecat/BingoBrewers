package com.github.indigopolecat.bingobrewers;


import net.minecraftforge.common.MinecraftForge;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class bingoShop {
    @SubscribeEvent
    public void onShopOpen(GuiOpenEvent event) {
        GuiChest guiChest;
        if (event.gui instanceof GuiChest) {
            guiChest = (GuiChest) event.gui;
            Container gui = guiChest.inventorySlots;

            if (gui instanceof ContainerChest) {
                ContainerChest containerChest = (ContainerChest) gui;
                String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
                System.out.println(containerChest);
                if (name.equals("Bingo Shop")) {
                    System.out.println("Bingo Shop opened!");
                    doneLoading chestLoad = new doneLoading();
                    chestLoad.onInventoryChanged(containerChest);
                    MinecraftForge.EVENT_BUS.register(new bingoShop() {
                        @SubscribeEvent
                        // Event that occurs after items are loaded into the chest GUI
                        //public void onInitGuiPost(GuiScreenEvent.DrawScreenEvent.Post eventPost) {
                        public void onInitGuiPost(doneLoading.InventoryLoadingDoneEvent event) {
                            // set variables in correct scope
                            String cost;
                            int costInt = 0;
                            ArrayList<String> itemNames = new ArrayList<>();
                            ArrayList<Integer> itemCosts = new ArrayList<>();
                            List<ItemStack> chestInventory = containerChest.getInventory();

                            for (ItemStack item : chestInventory) {
                                if (item != null) {
                                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                                    String target = "Cost";
                                    boolean costFound = false;
                                    for (String element : itemLore) {
                                        // if the previous lore line was "Cost", set this line to the cost variable and break the loop
                                        if (costFound) {
                                            cost = element;
                                            String unformattedCost = removeFormatting(cost);
                                            System.out.println(cost);
                                            try {
                                                costInt = Integer.parseInt(unformattedCost);
                                                System.out.println(costInt);
                                            } catch (NumberFormatException e) {
                                                System.out.println("Cost is not a number!");
                                            }
                                            itemCosts.add(costInt);
                                            String displayName = item.getDisplayName();
                                            itemNames.add(displayName);
                                            break;
                                        } else {
                                            // if lore line is "§5§o§7Cost"
                                            if (removeFormatting(element).equals(target)) {
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

                            System.out.println(itemNamesFormatless);
                            CompletableFuture<ArrayList<Double>> costFuture = auctionAPI.fetchPriceMap(itemNamesFormatless).whenComplete((lbinMap, throwable) -> {
                            });
                            costFuture.thenAccept (coinCosts -> {
                                System.out.println("coinCosts: " + coinCosts);
                            System.out.println(coinCosts);

                            if (itemCosts.size() == coinCosts.size() && itemCosts.size() == itemNames.size()) {
                                for (int i = 0; i < itemCosts.size(); i++) {
                                    Double coinCost = coinCosts.get(i);
                                    if (coinCost == (null)) {
                                        continue;
                                    }
                                    int bingoCost = itemCosts.get(i);
                                    String itemName = itemNames.get(i);
                                    //if (coinCost == null)
                                    if (coinCost == 0) {
                                        System.out.println("Item not found in auction house or price is somehow 0: " + itemName);
                                    } else if (bingoCost == 0) {
                                        System.out.println("Failed to get Bingo Point cost of item: " + itemName);
                                    } else {
                                        double coinsPerPointdouble = coinCost / bingoCost;
                                        long coinsPerPoint = Math.round(coinsPerPointdouble);
                                        System.out.println("Coins per Bingo Point: " + coinsPerPoint);

                                        for (ItemStack item : chestInventory) {
                                            if (item != null) {
                                                String displayName = item.getDisplayName();

                                                // nbt witchcraft
                                                if (displayName.equals(itemName)) {
                                                    NBTTagCompound nbt = item.getTagCompound();
                                                    NBTTagCompound displayTag = nbt.getCompoundTag("display");
                                                    NBTTagList loreList = displayTag.getTagList("Lore", 8);
                                                    System.out.println(nbt);

                                                    // Find the position of bingo point cost in lore
                                                    int costLineIndex = -1;
                                                    for (int j = 0; j < loreList.tagCount(); j++) {
                                                        System.out.println(removeFormatting(loreList.getStringTagAt(j)));
                                                        // Compare the current line without formatting to the cost in bingo points
                                                        // removeFormatting method removes " Bingo Points" from the end of the string
                                                        if (removeFormatting(loreList.getStringTagAt(j)).equals(Integer.toString(bingoCost))) {
                                                            costLineIndex = j;
                                                            System.out.println("costLineIndex: " + costLineIndex);
                                                            break;
                                                        }
                                                    }

                                                    loreList.appendTag(new NBTTagString(""));
                                                    for (int j = loreList.tagCount() - 1; j > costLineIndex + 1; j--) {
                                                        // Shift existing lines down to make space for the new line
                                                        loreList.set(j, loreList.get(j - 1));
                                                    }

                                                   //loreList.appendTag(new NBTTagString("§6§l" + coinsPerPoint + " Coins/Point"));
                                                    loreList.set(costLineIndex + 1, new NBTTagString("§6§l" + coinsPerPoint + " Coins/Point"));
                                                        System.out.println("loreList: " + loreList);
                                                    item.setTagCompound(nbt);

                                                    NBTTagCompound nbt2 = item.getTagCompound();
                                                    NBTTagCompound displayTag2 = nbt2.getCompoundTag("display");
                                                    NBTTagList loreList2 = displayTag2.getTagList("Lore", 8);
                                                    System.out.println("loreList2: " + loreList2);
                                                    System.out.println(nbt);


                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Something went wrong: itemCosts, coinCosts, and itemNames are not the same size!");
                            }
                            });
                            MinecraftForge.EVENT_BUS.unregister(this);
                        }
                    });
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
}

