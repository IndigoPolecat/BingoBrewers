package com.github.indigopolecat.bingobrewers;

public class TooltipInfo {
    // Contains all the necessary information to add tooltip to bingo shop items
    private final String name;
    private final String costPerPoint;
    private final String extraCost;
    private final int extraCostIndex;
    private final int costIndex;
    private final int bingoPointsPrice;
    private final int bingoRankRequired;

    /**
     * Constructs a TooltipInfo object with the given parameters.
     *
     * @param name The name of the item.
     * @param cost The cost per point of the item.
     * @param extraCost The extra cost associated with the item, if any.
     * @param costIndex The index in the tooltip where the cost per point should be inserted.
     * @param extraCostIndex The index in the tooltip where the extra cost should be inserted.
     * @param bingoPointsPrice The price of the item in Bingo Points.
     */
    public TooltipInfo(String name, String cost, String extraCost, int costIndex, int extraCostIndex, int bingoPointsPrice, int bingoRankRequired) {
        this.name = name;
        this.costPerPoint = cost;
        this.extraCost = extraCost;
        this.costIndex = costIndex;
        this.extraCostIndex = extraCostIndex;
        this.bingoPointsPrice = bingoPointsPrice;
        this.bingoRankRequired = bingoRankRequired;
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

    public int getBingoPointsPrice() {
        return bingoPointsPrice;
    }
    public int getBingoRankRequired() {
        return bingoRankRequired;
    }
}
