package com.github.indigopolecat.bingobrewers;

import java.net.HttpURLConnection;
import java.net.URL;
public class auctionAPI {

    public static int auctionAPISearch(String item) {
        String apiURL = "https://api.hypixel.net/skyblock/auctions";

        try {
            URL url = new URL(apiURL);

            HttpURLConnection api = (HttpURLConnection) url.openConnection();
            api.setRequestMethod("GET");
            int responseCode = api.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {

            } else {
                System.out.println("API connection failed!");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}
