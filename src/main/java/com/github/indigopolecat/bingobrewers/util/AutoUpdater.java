package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.*;
import moe.nea.libautoupdate.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class AutoUpdater {
    public static boolean updateScreen = false;
    public static boolean isThereUpdate = false;
    static boolean updateChecked = false;
    private final UpdateContext context = new UpdateContext(
        UpdateSource.githubUpdateSource("IndigoPolecat", "BingoBrewers"),
        UpdateTarget.deleteAndSaveInTheSameFolder(AutoUpdater.class),
        new StringSemVerCurrentVersion(BingoBrewers.version),
        "BingoBrewers"
    );
    
    static class StringSemVerCurrentVersion implements CurrentVersion {
        static final String[] patchOrder = {"ALFA", "BETA", "", "PATCH"};
        final String ver;
        final int[] parts;
        final String type;
        
        public StringSemVerCurrentVersion(String ver) {
            this.ver = ver;
            parts = parseVersionParts(ver);
            type = parseType(ver).toLowerCase(Locale.ROOT);
        }
        
        @Override
        public String display() {
            return ver;
        }
        
        @Override
        public boolean isOlderThan(JsonElement elm) {
            if(!elm.isJsonPrimitive()) return false;
            
            //no need to parse the version
            if(elm.getAsString().equalsIgnoreCase(ver)) return false;
            
            int[] otherParts = parseVersionParts(elm.getAsString());
            int min = Math.min(parts.length, otherParts.length);
            
            for(int i = 0; i < min; i++) {
                // if we are on a later version (maybe from another source) we don't need to upgrade
                if(parts[i] != otherParts[i]) return parts[i] < otherParts[i];
            }
            if(parts.length != otherParts.length) return min == parts.length; // 1.1 < 1.1.1
            
            String oType = parseType(elm.getAsString()).toLowerCase(Locale.ROOT);
            if(oType.equals(type)) return false;
            
            for(String origPattern : patchOrder) {
                String pattern = origPattern.trim().toLowerCase(Locale.ROOT) + "[0-9]*";
                int flag = (type.matches(pattern)? 2: 0) + (oType.matches(pattern)? 1: 0); //no need to evaluate up to 3 times each regex
                switch(flag) {
                    case 0: continue;
                    case 1: return true;
                    case 2: return false;
                    case 3: //both match the pattern, check the number at the end
                        String digitType = type.replaceFirst(origPattern, "").strip();
                        String digitOtherType = oType.replaceFirst(origPattern, "").strip();
                        if(digitType.matches("[0-9]+") && digitOtherType.matches("[0-9]+")) {
                            return Integer.parseInt(digitType) < Integer.parseInt(digitOtherType);
                        }
                        return digitType.isEmpty();
                }
            }
            
            String digType = type.replaceAll("^\\D+", "");
            String digOType = oType.replaceAll("^\\D+", "");
            if(!digType.isBlank() || !digOType.isBlank()) {
                if(digType.matches("[0-9]+") && digOType.matches("[0-9]+")) {
                    return Integer.parseInt(digType) < Integer.parseInt(digOType);
                }
                return digType.isEmpty();
            }
            
            //both unrecognized, may want to lexicographically compare
            return false;
        }
        
        @Override
        public String toString() {
            String version = Arrays.stream(parts).mapToObj(Integer::toString).reduce("", (a, b) -> a + "." + b);
            if(!type.isBlank()) version+= "-" + type;
            return "SemVerVersion (" + version + ")";
        }
        
        static int[] parseVersionParts(String tag) {
            if(tag.startsWith("v")) tag = tag.substring(1); //skip leading 'v'
            
            String[] parts = tag.split("\\.", 3);
            
            if(parts.length == 0) return new int[] {0, 0, 0};
            if(parts.length == 1 || parts[0].matches(".*[a-fA-F].*")) {// the version is in hex or a sequence number
                return new int[] {Integer.parseInt(parts[0].strip(), 16)};
            }
            
            int[] ret = new int[parts.length];
            int i;  // here since the version may be x.x[-type] or x.x.x[-type]
            for (i = 0; i < parts.length-1; i++) {
                ret[i] = Integer.parseInt(parts[i].strip());
            }
            
            if(parts[i].matches(" *[0-9]+ *")) { // no type present
                ret[i] = Integer.parseInt(parts[i].strip());
            } else {
                ret[i] = Integer.parseInt(parts[i].substring(0, parts[i].indexOf('-')).strip()); //remove "-BETA" or similar
            }
            
            return ret;
        }
        
        static String parseType(String tag) {
            if(!tag.contains("-")) return "";
            return tag.substring(tag.indexOf('-') + 1); //remove the '-'
        }
    }
    
    public void registerUpdateCheck() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client)->{
            if(updateChecked) return;
            updateChecked = true;
            
            Log.info("Checking for updates...");
            checkUpdate().thenAccept(updateAvailable -> {
                if(!updateAvailable) return;
                
                if(BingoBrewersConfig.getConfig().autoDownload) {
                    BingoBrewers.autoUpdater.update();
                    //BingoBrewers.activeTitle = new TitleHud("Bingo Brewers will update on game close.", 0x47EB62, 4000,false); //TODO(matita): redo this
                } else {
                    isThereUpdate = true;
                    updateScreen = true;
                    client.execute(() -> client.setScreen(new UpdateScreen()));
                }
            });
        });
        
        ServerWorldEvents.LOAD.register((server, level)->{
            if(updateChecked) return;
            updateChecked = true;
            
            Log.info("Checking for updates...");
            checkUpdate().thenAccept(updateAvailable -> {
                if(!updateAvailable) return;
                if(BingoBrewersConfig.getConfig().autoDownload) {
                    BingoBrewers.autoUpdater.update();
                    //BingoBrewers.activeTitle = new TitleHud("Bingo Brewers will update on game close.", 0x47EB62, 4000, false); //TODO(matita): redo this
                } else {
                    isThereUpdate = true;
                    updateScreen = true;
                }
            });
        });
    }
    
    public CompletableFuture<Boolean> checkUpdate() {
        return CompletableFuture.supplyAsync(()->{
            try {
                String updaterType = switch(BingoBrewersConfig.getConfig().autoUpdaterType) {
                    case BETA -> "pre";
                    case STABLE -> "full";
                    default -> "none";
                };
                PotentialUpdate potentialUpdate = context.checkUpdate(updaterType).join();
                Thread.sleep(1000);
                return potentialUpdate.isUpdateAvailable();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Executors.newSingleThreadExecutor());
    }
    
    public CompletableFuture<Boolean> update() {
        String updaterType = switch(BingoBrewersConfig.getConfig().autoUpdaterType) {
            case BETA -> "pre";
            case STABLE -> "full";
            default -> "none";
        };
        return context.checkUpdate(updaterType).thenComposeAsync(potentialUpdate->{
            if(potentialUpdate.isUpdateAvailable()) {
                return potentialUpdate.launchUpdate().thenApply((ignored)->{
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Bingo Brewers has been updated to the latest version! Please restart your game to apply the update."), true);
                    return true;
                });
            }
            return CompletableFuture.completedFuture(false);
        });
    }
    
    public String getChangelog() {
        try {
            URL url = (new URI("https://api.github.com/repos/IndigoPolecat/BingoBrewers/releases/latest")).toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String input;
                StringBuilder response = new StringBuilder();
                
                while((input = in.readLine()) != null) {
                    response.append(input);
                }
                in.close();
                
                Gson gson = new Gson();
                
                return gson.fromJson(response.toString(), JsonObject.class).get("body").getAsString();
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return "Could not fetch changelog";
    }
}
