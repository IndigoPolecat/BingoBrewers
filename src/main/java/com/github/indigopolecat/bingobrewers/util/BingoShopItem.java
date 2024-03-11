package com.github.indigopolecat.bingobrewers.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BingoShopItem {
    public String itemID;
    public String itemName; // used to get item id for extra cost items
    public int costInCoins;
    public int bingoRankRequired;
    public int costInPoints;
    public List<String> extraCost = new ArrayList<>();
    public HashMap<String, Integer> extraCostMap = new HashMap<>();
    public boolean soulbound = true;
    public ArrayList<Integer> extraCostIndex = new ArrayList<>();
    public ItemStack itemStack;


    public BingoShopItem(ItemStack item) {
        this.itemStack = item;
        NBTTagCompound nbt = item.getTagCompound();
        NBTTagCompound displayTag = nbt.getCompoundTag("display");
        NBTTagList loreList = displayTag.getTagList("Lore", 8);
        NBTTagCompound extraAttributes = nbt.getCompoundTag("ExtraAttributes");
        this.itemName = displayTag.getString("Name").replaceAll("§.", "");
        this.itemID = extraAttributes.getString("id");
        for (int i = 0; i < loreList.tagCount(); i++) {
            String lore = loreList.getStringTagAt(i);
            if (lore.equals("§7Cost")) {
                this.costInPoints = Integer.parseInt(loreList.getStringTagAt(i + 1).replaceAll("\\D", ""));
                while (!loreList.getStringTagAt(i + 2).replaceAll("§.", "").isEmpty()) {

                    extraCost.add(loreList.getStringTagAt(i + 2).replaceAll("§.", ""));
                    extraCostIndex.add(i + 2);
                    i++;
                }
            } else if (lore.contains("Bingo Rank")) {
                Pattern rankPattern = Pattern.compile("Bingo Rank ([IVXLCDM]+)");
                Matcher rankMatcher = rankPattern.matcher(lore);
                if (rankMatcher.find()) {
                    String rank = rankMatcher.group(1);
                    switch (rank) {
                        case "I":
                            this.bingoRankRequired = 1;
                        case "II":
                            this.bingoRankRequired = 2;
                        case "III":
                            this.bingoRankRequired = 3;
                        case "IV":
                            this.bingoRankRequired = 4;
                        case "V":
                            this.bingoRankRequired = 5;
                        case "VI":
                            this.bingoRankRequired = 6;
                        case "VII":
                            this.bingoRankRequired = 7;
                        case "VIII":
                            this.bingoRankRequired = 8;
                        case "IX":
                            this.bingoRankRequired = 9;
                        case "X":
                            this.bingoRankRequired = 10;
                        default:
                            this.bingoRankRequired = 0;
                    }
                }
            } else if (!lore.contains("Soulbound")) {
                this.soulbound = false;
            }
        }


    }

}
