package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.kryo.KryoNetwork;

import java.util.Arrays;

import static com.github.indigopolecat.bingobrewers.CHWaypoints.itemCounts;

public class CrystalHollowsItemTotal {
    public String itemName;
    public String itemCount;
    public int itemColor;
    public int countColor;

    public static void sumItems(KryoNetwork.CHChestItem item) {
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
                System.out.println("range.length: " + range.length + " item: " + item.name + " count: " + item.count + "range: " + Arrays.toString(range));
                System.out.println("Something went wrong, CH Item count range doesn't contain 2 numbers");
                return itemTotal;
            }
            int lowerRangeOfNewItem = Integer.parseInt(range[0]);
            int upperRangeOfNewItem = Integer.parseInt(range[1]);

            String[] currentRange = itemCountExisting.split("-");
            if (currentRange.length != 2) {
                System.out.println("currentRange.length: " + currentRange.length + " item: " + item.name + " count: " + itemCountExisting + "range: " + Arrays.toString(currentRange));
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
}
