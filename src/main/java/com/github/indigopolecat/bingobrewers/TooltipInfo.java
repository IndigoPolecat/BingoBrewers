package com.github.indigopolecat.bingobrewers;

public class TooltipInfo {
    // Contains all of the necessary information to add tooltip to bingo shop items
    private final String name;
    private final String costPerPoint;
    private final String extraCost;
    private final int extraCostIndex;
    private final int costIndex;

    public TooltipInfo(String name, String cost, String extraCost, int costIndex, int extraCostIndex) {
        this.name = name;
        this.costPerPoint = cost;
        this.extraCost = extraCost;
        this.costIndex = costIndex;
        this.extraCostIndex = extraCostIndex;
    }

    public String getName() {
        return this.name;
    }

    public String getCost() {
        return costPerPoint;
    }

    public String getExtraCost() {
        return extraCost;
    }

    public int getExtraCostIndex() {
        return extraCostIndex;
    }

    public int getCostIndex() {
        return costIndex;
    }
}
