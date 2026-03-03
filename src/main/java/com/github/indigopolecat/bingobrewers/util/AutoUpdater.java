package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import moe.nea.libautoupdate.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

//TODO: there seems to be some duplicated code
public class AutoUpdater {
    public static boolean updateScreen = false;

    private final UpdateContext context = new UpdateContext(
            UpdateSource.githubUpdateSource("IndigoPolecat", "BingoBrewers"),
            UpdateTarget.deleteAndSaveInTheSameFolder(AutoUpdater.class),
            CurrentVersion.ofTag(BingoBrewers.version),
            "BingoBrewers"
    );

    public CompletableFuture<Boolean> checkUpdate() {
        return CompletableFuture.supplyAsync(() -> {
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
        return context.checkUpdate(updaterType).thenComposeAsync(potentialUpdate -> {
            if(potentialUpdate.isUpdateAvailable()) {
                return potentialUpdate.launchUpdate().thenApply((ignored) -> {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Bingo Brewers has been updated to the latest version! Please restart your game to apply the update."), true);
                    return true;
                });
            }
            return CompletableFuture.completedFuture(false);
        });
    }

    static boolean updateChecked = false;

    public static boolean isThereUpdate = false;
    
    public void registerUpdateCheck() {
       ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
          if (updateChecked) return;
          updateChecked = true;
          
          UpdateUtils.patchConnection(connection -> {
             if (connection instanceof HttpsURLConnection https) {
                //https.setSSLSocketFactory(ctx.getSocketFactory()); //TODO (matita): removed keystore for now
             }
          });
          
          System.out.println("Checking for updates...");
          checkUpdate().thenAccept(updateAvailable -> {
             if (!updateAvailable) return;
             
             if (BingoBrewersConfig.getConfig().autoDownload) {
                BingoBrewers.autoUpdater.update();
                //BingoBrewers.activeTitle = new TitleHud("Bingo Brewers will update on game close.", 0x47EB62, 4000,false); //TODO(matita): redo this
             } else {
                isThereUpdate = true;
                updateScreen = true;
                client.execute(() -> {
                   client.setScreen(new UpdateScreen());
                });
             }
          });
       });
       
       ServerWorldEvents.LOAD.register((server, level) ->{
          if(!updateChecked) {
             updateChecked = true;
             
             UpdateUtils.patchConnection(connection->{
                if(connection instanceof HttpsURLConnection) {
                   //((HttpsURLConnection)connection).setSSLSocketFactory(ctx.getSocketFactory()); //TODO (matita): removed keystore for now
                }
             });
             
             System.out.println("Checking for updates...");
             checkUpdate().thenAccept(updateAvailable->{
                if(updateAvailable) {
                   if(BingoBrewersConfig.getConfig().autoDownload) {
                      BingoBrewers.autoUpdater.update();
                      //BingoBrewers.activeTitle = new TitleHud("Bingo Brewers will update on game close.", 0x47EB62, 4000, false); //TODO(matita): redo this
                   } else {
                      isThereUpdate = true;
                      updateScreen = true;
                   }
                }
             });
          }
       });
    }

    public String getChangelog() {
        try {
            URL url = (new URI("https://api.github.com/repos/IndigoPolecat/BingoBrewers/releases/latest")).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String input;
                StringBuilder response = new StringBuilder();

                while ((input = in.readLine()) != null) {
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
