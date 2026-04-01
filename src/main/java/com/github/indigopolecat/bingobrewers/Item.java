package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.LoggerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// This class is unused. keeping in case we need to do it on our own server in the future.
// Required for auctionAPI.java
public class Item {
    private final String name;
    private final ArrayList<Double> cost;

    public Item(String name) {
        this.name = name;
        this.cost = new ArrayList<>();
    }

    public void addCost(double price) {
        this.cost.add(price);
        LoggerUtil.LOGGER.info(String.valueOf(price));
        LoggerUtil.LOGGER.info(String.valueOf(this.cost));
    }

    public String getName() {
        return this.name;
    }

    public List<Double> getCost() {
        return cost;
    }

    public double getLowestCost() {
        if (cost.isEmpty()) {
            LoggerUtil.LOGGER.info("No Auction Items Found!");
            return 0;
        } else {
            Collections.sort(cost);
            return cost.get(0);
        }
    }
}
