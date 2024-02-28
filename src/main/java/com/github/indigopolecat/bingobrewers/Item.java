package com.github.indigopolecat.bingobrewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

// This class is unused. keeping incase we need to do it on our own server in the future.
// Required for auctionAPI.java
public class Item {
    Logger logger = Logger.getLogger(Item.class.getName());
    private final String name;
    private final ArrayList<Double> cost;

    public Item(String name) {
        this.name = name;
        this.cost = new ArrayList<>();
    }

    public void addCost(double price) {
        this.cost.add(price);
        logger.info(String.valueOf(price));
        logger.info(String.valueOf(this.cost));
    }

    public String getName() {
        return this.name;
    }

    public List<Double> getCost() {
        return cost;
    }

    public double getLowestCost() {
        if (cost.isEmpty()) {
            logger.info("No Auction Items Found!");
            return 0;
        } else {
            Collections.sort(cost);
            return cost.get(0);
        }
    }
}
