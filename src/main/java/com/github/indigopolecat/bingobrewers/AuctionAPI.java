package com.github.indigopolecat.bingobrewers;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

public class AuctionAPI {
    public static final String SKYBLOCK_BAZAAR_API = "https://api.hypixel.net/v2/skyblock/bazaar";
    public static final String MOULBERRY_LOWEST_BIN_API = "https://moulberry.codes/lowestbin.json.gz";

    private AuctionAPI() {
        throw new IllegalStateException("Utility class");
    }

    // input: array of display names, output: array of lowest bin prices from neu as doubles in a matching order
    // Assumes Display Name ID is DISPLAY_NAME
    // TODO: get item ID from NBT tag instead of display name
    static long lastFetch = 0;
    static String auctionJson = "";
    static String bazaarJson = "";

    public static CompletableFuture<ArrayList<Double>> fetchPriceMap(List<String> items) {

        return CompletableFuture.supplyAsync(() -> {

            if (lastFetch < System.currentTimeMillis() - 60000) {
                lastFetch = System.currentTimeMillis();
                ByteArrayOutputStream byteArrayOutputStream;
                try {
                    byteArrayOutputStream = getByteArrayOutputStream();
                    ByteArrayOutputStream byteArrayOutputStreamBz = getArrayOutputStream();
                    bazaarJson = byteArrayOutputStreamBz.toString(StandardCharsets.UTF_8);
                    auctionJson = byteArrayOutputStream.toString(String.valueOf(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            Type type = new TypeToken<HashMap<String, Double>>() {
            }.getType();
            HashMap<String, Double> lBinMap = new Gson().fromJson(auctionJson, type);

            ArrayList<Double> costs = new ArrayList<>();
            ArrayList<Item> itemList = new ArrayList<>();

            for (String s : items) {
                Item item = new Item(s);

                itemList.add(item);
            }

            for (String item : items) {
                item = item.replace(" ", "_").toUpperCase();
                if (lBinMap.containsKey(item)) {
                    costs.add(lBinMap.get(item));
                } else if (new Gson().fromJson(bazaarJson, JsonObject.class).getAsJsonObject("products").has(item)) {
                    // Try to get it from bz instead if ah fails
                    costs.add(new Gson().fromJson(bazaarJson, JsonObject.class).getAsJsonObject("products").getAsJsonObject(item).get("quick_status").getAsJsonObject().get("sellPrice").getAsDouble());
                } else {
                    costs.add(null);
                }
            }
            return costs;
        });
    }

    @NotNull
    private static ByteArrayOutputStream getArrayOutputStream() throws IOException {
       URL bazaarUrl;
       try {
          bazaarUrl = new URI(SKYBLOCK_BAZAAR_API).toURL();
       } catch (URISyntaxException e) {
          throw new AssertionError("SKYBLOCK_BAZAAR_API is invalid", e);
       }
       
       HttpURLConnection connectionBz = (HttpURLConnection) bazaarUrl.openConnection();
        connectionBz.setRequestMethod("GET");
        connectionBz.connect();

        InputStream inputStreamBz = connectionBz.getInputStream();

        ByteArrayOutputStream byteArrayOutputStreamBz = new ByteArrayOutputStream();

        byte[] bufferBz = new byte[1024];
        int lenbz;
        while ((lenbz = inputStreamBz.read(bufferBz)) > 0) {
            byteArrayOutputStreamBz.write(bufferBz, 0, lenbz);
        }
        return byteArrayOutputStreamBz;
    }

    @NotNull
    private static ByteArrayOutputStream getByteArrayOutputStream() throws IOException {
       URL apiURL;
       try {
          apiURL = new URI(MOULBERRY_LOWEST_BIN_API).toURL();
       } catch (URISyntaxException e) {
          throw new AssertionError("MOULBERRY_LOWEST_BIN_API is invalid", e);
       }
       
       HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.connect();

        InputStream inputStream = connection.getInputStream();
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buffer)) > 0) {
            byteArrayOutputStream.write(buffer, 0, len);

        }
        return byteArrayOutputStream;
    }
}
