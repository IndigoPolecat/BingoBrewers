package com.github.indigopolecat.bingobrewers;

import java.util.ArrayList;
import java.util.Collections;

public class Item {
    private final String name;
    private final ArrayList<Integer> cost;

    public Item(String name) {
        this.name = name;
        this.cost = new ArrayList<>();
    }

    public void addCost(int price) {
        this.cost.add(price);
        System.out.println(price);
        System.out.println(this.cost);
    }

    public String getName() {
        return this.name;
    }

    public int getLowestCost() {
        if (cost.isEmpty()) {
            System.out.println("empty");
            return 0;
        } else {
            Collections.sort(cost);
            return cost.get(0);
        }
    }
}
