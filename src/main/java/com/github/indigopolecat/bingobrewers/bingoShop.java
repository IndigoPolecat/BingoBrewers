package com.github.indigopolecat.bingobrewers;


import net.minecraftforge.common.MinecraftForge;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import java.util.List;
import java.util.ArrayList;

public class bingoShop {
    @SubscribeEvent
    public void onShopOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) event.gui;
            Container gui = guiChest.inventorySlots;

            if (gui instanceof ContainerChest) {
                ContainerChest containerChest = (ContainerChest) gui;
                String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
                if (name.equals("Bingo Shop")) {
                    System.out.println("Bingo Shop opened!");
                    MinecraftForge.EVENT_BUS.register(new bingoShop() {
                        @SubscribeEvent
                        // Event that occurs after items are loaded into the chest GUI
                        public void onInitGuiPost(GuiScreenEvent.DrawScreenEvent.Post eventPost) {
                            // set variables in correct scope
                            String cost = "";
                            int costInt = 0;
                            ArrayList<String> itemNames = new ArrayList<>();
                            List<ItemStack> chestInventory = containerChest.getInventory();

                            for (ItemStack item : chestInventory) {
                                if (item != null) {
                                    List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                                    String target = "§5§o§7Cost";
                                    boolean costFound = false;
                                    for (String element : itemLore) {
                                        // if the previous lore line was "§5§o§7Cost", set this line to the cost variable and break the loop
                                        if (costFound) {
                                            cost = element;
                                            String unformattedCost = removeFormatting(cost);
                                            try {
                                                costInt = Integer.parseInt(unformattedCost);
                                                System.out.println(costInt);
                                            } catch (NumberFormatException e) {
                                                System.out.println("Cost is not a number!");
                                            }
                                            String displayName = item.getDisplayName();
                                            itemNames.add(displayName);
                                            break;
                                        } else {
                                            // if lore line is "§5§o§7Cost"
                                            if (element.equals(target)) {
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
                            ArrayList<Integer> coinCosts = auctionAPI.auctionAPISearch(itemNamesFormatless);
                            System.out.println(coinCosts);
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

