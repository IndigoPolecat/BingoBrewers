package com.github.indigopolecat.kryo;

import java.util.HashMap;

public class ServerSummary {
    public String server;
    public int availablePlayersToWarp; // how many players are in the lobby that can warp
    public long lastUpdated;
    public HashMap<String, CondensedItemSummary> condensedItems = new HashMap<>();
}
