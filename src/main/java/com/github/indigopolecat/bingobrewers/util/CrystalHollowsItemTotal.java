package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import com.github.indigopolecat.kryo.KryoNetwork;

import java.util.HashMap;
import java.util.Map;

import static com.github.indigopolecat.bingobrewers.CHWaypoints.itemCounts;

public class CrystalHollowsItemTotal {
    public String itemName;
    public String itemCount;
    public int itemColor;
    public int countColor;

    public CrystalHollowsItemTotal(CrystalHollowsItemTotal total) {
        this.itemCount = total.itemCount;
        this.itemColor = total.itemColor;
        this.itemName = total.itemName;
        this.countColor = total.countColor;
    }

    public CrystalHollowsItemTotal() {
    }

    public static void sumItems(KryoNetwork.CHChestItem item, HashMap<String, CrystalHollowsItemTotal> itemCounts) {
        if (itemCounts.containsKey(item.name)) {
            CrystalHollowsItemTotal itemTotal = itemCounts.get(item.name);
            String itemCountExisting = itemTotal.itemCount;
            // handle ranges
            if (item.count.contains("-")) {
                itemCounts.put(item.name, sumPowder(itemCountExisting, item, itemTotal));
            } else {
                int itemCountIntegerExisting;
                int itemCountIntegerNew;
                try {
                    itemCountIntegerExisting = Integer.parseInt(itemCountExisting);
                    itemCountIntegerNew = Integer.parseInt(item.count);
                } catch (NumberFormatException e) {
                    System.out.println("Something went wrong, " + item.name + " with count " + itemCountExisting + " and new count " + item.count + " cannot be parsed. (normal)");
                    return;
                }
                System.out.println("itemCountNew: " + itemCountIntegerNew + " Item Count Existing: " + itemCountIntegerExisting);
                itemTotal.itemCount = (itemCountIntegerNew + itemCountIntegerExisting) + "";
                itemCounts.put(item.name, itemTotal);
            }
        } else {
            CrystalHollowsItemTotal itemTotal = new CrystalHollowsItemTotal();
            itemTotal.itemCount = item.count;
            itemTotal.itemName = item.name;
            itemTotal.itemColor = item.itemColor;
            itemTotal.countColor = item.numberColor;
            itemCounts.put(item.name, itemTotal);
        }
    }

    public static CrystalHollowsItemTotal sumPowder(String itemCountExisting, KryoNetwork.CHChestItem item, CrystalHollowsItemTotal itemTotal) {
        try {
            String[] range = item.count.split("-");
            if (range.length != 2) {
                System.out.println("Something went wrong, CH Item count range doesn't contain 2 numbers");
                return itemTotal;
            }
            int lowerRangeOfNewItem = Integer.parseInt(range[0]);
            int upperRangeOfNewItem = Integer.parseInt(range[1]);

            String[] currentRange = itemCountExisting.split("-");
            if (currentRange.length != 2) {
                System.out.println("Something went wrong, _existing_ CH Item count range doesn't contain 2 numbers");
                return itemTotal;
            }
            int lowerRangeOfExistingItem = Integer.parseInt(currentRange[0]);
            int upperRangeOfExistingItem = Integer.parseInt(currentRange[1]);

            int newLowerRange = lowerRangeOfExistingItem + lowerRangeOfNewItem;
            int newUpperRange = upperRangeOfNewItem + upperRangeOfExistingItem;
            itemTotal.itemCount = newLowerRange + "-" + newUpperRange;
            return itemTotal;
        } catch (NumberFormatException e) {
            System.out.println("Something went wrong, " + item.name + " with existing count " + itemCountExisting + " and new count " + item.count + " cannot be parsed. (ranged)");
        }
        return itemTotal;
    }

    public static void subtractTotal(HashMap<String, CrystalHollowsItemTotal> totalItems, HashMap<String, CrystalHollowsItemTotal> collectedItems, HashMap<String, CrystalHollowsItemTotal> outputMap) {
        for (Map.Entry<String, CrystalHollowsItemTotal> entry : totalItems.entrySet()) {
            CrystalHollowsItemTotal collectedItem = collectedItems.get(entry.getKey());
            if (collectedItem == null) continue;
            CrystalHollowsItemTotal itemTotal = totalItems.get(entry.getKey());

            String itemCountTotal = itemTotal.itemCount;
            String collectedCountTotal = itemTotal.itemCount;
            // handle ranges
            if (itemCountTotal.contains("-")) {
                outputMap.put(entry.getKey(), subtractPowder(itemCountTotal, collectedCountTotal, itemTotal));
            } else {
                int itemCountIntegerTotal;
                int itemCountIntegerCollected;
                try {
                    itemCountIntegerTotal = Integer.parseInt(itemCountTotal);
                    itemCountIntegerCollected = Integer.parseInt(collectedCountTotal);
                } catch (NumberFormatException e) {
                    System.out.println("Something went wrong, " + itemTotal.itemName + " with count " + itemCountTotal + " and new count " + itemTotal.itemCount + " cannot be parsed. (normal)");
                    return;
                }
                itemTotal.itemCount = (itemCountIntegerTotal - itemCountIntegerCollected) + "";
                outputMap.put(entry.getKey(), itemTotal);
            }
        }
    }

    public static CrystalHollowsItemTotal subtractPowder(String itemCountTotal, String itemCountCollected, CrystalHollowsItemTotal itemTotal) {
        try {
            String[] range = itemCountCollected.split("-");
            if (range.length != 2) {
                System.out.println("Something went wrong, CH Item count range doesn't contain 2 numbers");
                return itemTotal;
            }
            int lowerRangeOfCollected = Integer.parseInt(range[0]);
            int upperRangeOfCollected = Integer.parseInt(range[1]);

            String[] currentRange = itemCountTotal.split("-");
            if (currentRange.length != 2) {
                System.out.println("Something went wrong, _existing_ CH Item count range doesn't contain 2 numbers");
                return itemTotal;
            }
            int lowerRangeOfTotal = Integer.parseInt(currentRange[0]);
            int upperRangeOfTotal = Integer.parseInt(currentRange[1]);

            int newLowerRange = lowerRangeOfTotal - lowerRangeOfCollected;
            int newUpperRange = upperRangeOfTotal - upperRangeOfCollected;
            itemTotal.itemCount = newLowerRange + "-" + newUpperRange;
            return itemTotal;
        } catch (NumberFormatException e) {
            System.out.println("Something went wrong, " + itemTotal.itemName + " with existing count " + itemCountTotal + " and new count " + itemCountTotal + " cannot be parsed. (ranged)");
        }
        return itemTotal;
    }
}
