package com.github.indigopolecat.bingobrewers;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.Objects;


public class auctionAPI {

    public static ArrayList<Integer>  auctionAPISearch(ArrayList<String> items) {
        String apiURL = "https://api.hypixel.net/skyblock/auctions";

        // query api plus some anti error stuff
        String json = Objects.requireNonNull(queryAPI(apiURL)).toString();

        ArrayList<Item> itemList = new ArrayList<>();
        System.out.println("Size of Array " + items.size());
        for (String s : items) {
            Item item = new Item(s);
            System.out.println("for loop of array: " + item.getName());

            itemList.add(item);
        }
        System.out.println("itemList: " + itemList);
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

        int totalAuctions = jsonObject.get("auctions").getAsJsonArray().size();
        System.out.println("totalAuctions: " + totalAuctions);

        int totalPages = jsonObject.get("totalPages").getAsInt();
        //int i = 0;
        for (int i = 0; i < totalPages; i++) {
            String auctionPage = Objects.requireNonNull(queryAPI("https://api.hypixel.net/skyblock/auctions?page=" + i)).toString();
            JsonObject auctionJSON = new Gson().fromJson(auctionPage, JsonObject.class);
            JsonArray auctions = auctionJSON.get("auctions").getAsJsonArray();
            System.out.println("page: " + auctionJSON.get("page").getAsInt());

            for (int j = 0; j < auctions.size(); j++) {
                JsonObject auction = auctions.get(j).getAsJsonObject();
                String item = auction.get("item_name").getAsString();
                System.out.print(item + ", ");

                if (items.contains(item)) {
                    System.out.println("Found item!");

                    if (auction.get("bin").getAsBoolean()) {
                        System.out.println("Item is BIN!");
                        int price = auction.get("starting_bid").getAsInt();
                        Item itemObject = getItemByName(itemList, item);
                        if (itemObject != null) {
                            System.out.println(itemObject.getName());
                        }
                        if (itemObject != null) {
                            itemObject.addCost(price);
                        }


                    } else {
                        System.out.println("Item is not BIN, Ignoring!");
                    }
                }
                //i++;
            }
            System.out.println("done with page " + i);
        }
        ArrayList<Integer> costs = new ArrayList<>();
        for (Item item : itemList) {
            costs.add(item.getLowestCost());
        }
        return costs;
    }

    private static Item getItemByName(ArrayList<Item> itemList, String itemName) {
        for (Item item : itemList) {
            if (item.getName().equals(itemName)) {
                return item; // Return the found item
            }
        }
        return null; // Return null if the item is not found
    }


    private static StringBuffer queryAPI(String apiURL) {
        try {
            URL url = new URL(apiURL);

            HttpURLConnection api = (HttpURLConnection) url.openConnection();
            api.setRequestMethod("GET");
            int responseCode = api.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(api.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response;
            } else {
                System.out.println("API connection failed!");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        return null;
    }
}


