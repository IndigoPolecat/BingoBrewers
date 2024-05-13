package com.github.indigopolecat.kryo;

import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;

import java.util.ArrayList;
import java.util.HashMap;

public class CondensedItemSummary {
    public HashMap<String, CrystalHollowsItemTotal> condensedItems = new HashMap<>(); // display the totals on hover, contains all of the information on how to display each item
    public ArrayList<String> iconPaths = new ArrayList<>(); // in order, the list of icons to show (can be overlapped if space is an issue
    public String summaryName; // e.g. "Robot Parts"
    public int summaryNameColor; // color of the name text
    public String serverName;
    public long lastUpdated;
}
