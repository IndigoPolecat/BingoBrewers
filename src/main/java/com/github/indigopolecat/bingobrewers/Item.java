package com.github.indigopolecat.bingobrewers;

import java.util.ArrayList;
import java.util.Collections;

// This class is unused. keeping incase we need to do it on our own server in the future.
// Required for auctionAPI.java
public class Item {
    private final String name;
    private final ArrayList<Long> cost;

    public Item(String name) {
        this.name = name;
        this.cost = new ArrayList<Long>();
    }

    public void addCost(long price) {
        this.cost.add(price);
        System.out.println(price);
        System.out.println(this.cost);
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<Long> getCost() {
        return cost;
    }

    public long getLowestCost() {
        if (cost.isEmpty()) {
            System.out.println("No Auction Items Found!");
            return 0;
        } else {
            Collections.sort(cost);
            return cost.get(0);
        }
    }
}
