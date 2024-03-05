package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import moe.nea.libautoupdate.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class AutoUpdater {
    private final UpdateContext context = new UpdateContext(
            UpdateSource.githubUpdateSource("IndigoPolecat", "BingoBrewers"),
            UpdateTarget.deleteAndSaveInTheSameFolder(AutoUpdater.class),
            CurrentVersion.ofTag(BingoBrewers.version),
            "BingoBrewers"
    );

    public CompletableFuture<Boolean> checkUpdate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String updaterType = "none";
                if(BingoBrewersConfig.autoUpdaterType == 0) updaterType = "full";
                if(BingoBrewersConfig.autoUpdaterType == 1) updaterType = "pre";
                PotentialUpdate potentialUpdate = context.checkUpdate(updaterType).join();
                Thread.sleep(1000);
                return potentialUpdate.isUpdateAvailable();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Executors.newSingleThreadExecutor());
    }

    public void update() {
        new Thread(() -> {
            String updaterType = "none";
            if(BingoBrewersConfig.autoUpdaterType == 0) updaterType = "full";
            if(BingoBrewersConfig.autoUpdaterType == 1) updaterType = "pre";
            context.checkUpdate(updaterType).thenAcceptAsync(potentialUpdate -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(potentialUpdate.isUpdateAvailable()) {
                    potentialUpdate.launchUpdate();
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bingo Brewers has been updated to the latest version! Please restart your game to apply the update."));
                }
            }, Executors.newSingleThreadExecutor());
        }).start();
    }

    static boolean updateChecked = false;

    public static boolean isThereUpdate = false;
    @SubscribeEvent
    public void updateCheck(EntityJoinWorldEvent event) {
        if(!(event.entity instanceof EntityPlayer)) return;
        if (!updateChecked) {
            updateChecked = true;
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;

            System.out.println("Checking for updates...");
            checkUpdate().thenAccept(updateAvailable -> {
                if(updateAvailable) {
                    isThereUpdate = true;
                    Minecraft.getMinecraft().displayGuiScreen(new UpdateScreen());
                }
            });
        }
    }

    public String getChangelog() {
        try {
            URL url = new URL("https://api.github.com/repos/IndigoPolecat/BingoBrewers/releases/latest");
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "Could not fetch changelog";
    }
}
