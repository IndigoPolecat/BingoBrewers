package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
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
    @SubscribeEvent
    public void updateCheck(EntityJoinWorldEvent event) {
        if(!(event.entity instanceof EntityPlayer)) return;
        if (!updateChecked) {
            updateChecked = true;
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;

            System.out.println("Checking for updates...");
            checkUpdate().thenAccept(updateAvailable -> {
                if(updateAvailable) {
                    ChatComponentText update = new ChatComponentText(EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "  [UPDATE]  ");
                    update.setChatStyle(update.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingobrewersupdate")));
                    player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Bingo Brewers is outdated. Click the button below to auto update after you turn off the game next time.").appendSibling(update));
                }
            });
        }
    }
}
