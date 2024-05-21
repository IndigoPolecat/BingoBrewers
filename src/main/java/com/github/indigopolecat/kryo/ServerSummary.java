package com.github.indigopolecat.kryo;

import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.kryo.KryoNetwork.*;
import java.util.HashMap;

public class ServerSummary {
    public String server;
    public String serverType; // Ex. Crystal Hollows, just so the warp network can be implemented for other servers in the future easily, add a check to make sure it's "Crystal Hollows"
    public int availablePlayersToWarp; // how many players are in the lobby that can warp
    public long lastUpdated;
    public HashMap<String, CrystalHollowsItemTotal> condensedItems = new HashMap<>();

    public static void sendUpdatedSummariesRequest() {
        UpdateServers updateServers = new UpdateServers();
        for (ServerSummary summary : ServerConnection.serverSummaries.values()) {
            updateServers.serversAndLastUpdatedTime.put(summary.server, summary.lastUpdated);
        }
        ServerConnection.requestUpdatedServerSummaries(updateServers);
    }
}