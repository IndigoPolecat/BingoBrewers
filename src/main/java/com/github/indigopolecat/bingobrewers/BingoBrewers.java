package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.github.indigopolecat.bingobrewers.config.ConfigSerializer;
import com.github.indigopolecat.bingobrewers.gui.ColorGuiProvider;
import com.github.indigopolecat.bingobrewers.hud.*;
import com.github.indigopolecat.bingobrewers.util.AutoUpdater;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.events.HypixelPackets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.context.CommandContext;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import lombok.*;

import java.awt.Color;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BingoBrewers implements ClientModInitializer {
    public static BingoBrewers INSTANCE;

    @Getter(onMethod_ = @Synchronized)
    @Setter(onMethod_ = @Synchronized)
    private static volatile Client client;
    public static final String version = "v0.3.9-beta";
    //TODO(matita): overridden detection for now, also it may better to be moved as ServerUtils.isHypixel()
    public static boolean onHypixel = true; // TODO(polecat): this doesn't work if someone is using a proxy to connect to hypixel, need better detection

    public static AutoUpdater autoUpdater = new AutoUpdater();
    public static HashMap<String, Integer> minecraftColors = new HashMap<>();

    public static CopyOnWriteArrayList<HypixelPacket> packetHold = new CopyOnWriteArrayList<>();
    public static HypixelPacket lastPacketSent;
    public static long lastPacketSentAt = 0;
    public static boolean waitingForPacketResponse;

    //1 = open at next tick, 2 = open now, 0 = do not open
    @Getter(value = AccessLevel.PRIVATE, onMethod_ = @Synchronized)
    @Setter(value = AccessLevel.PRIVATE, onMethod_ = @Synchronized)
    private static int openConfig = 0;

    private static void registerConfigCommandDelayed() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (getOpenConfig() == 0) return;
            if (getOpenConfig() == 1) {
                setOpenConfig(2);
                return;
            }
            setOpenConfig(0);

            Minecraft.getInstance().execute(() -> {
                boolean render = RenderSystem.isOnRenderThread();
                Log.LOG.debug("renderThread = {}", render);
                Log.LOG.debug("parent screen={}", Minecraft.getInstance().screen);
                Screen configScreen = AutoConfig.getConfigScreen(BingoBrewersConfig.class, null).get();
                Minecraft.getInstance().setScreen(configScreen);
                Log.LOG.debug("configScreen present={}, current screen={}", configScreen != null, Minecraft.getInstance().screen);
            });
        });
    }

    public static int configCommand(CommandContext<FabricClientCommandSource> context) {
        Log.info("Opening config menu");
        if (getOpenConfig() == 0) setOpenConfig(1);
        return 1; //1 is success
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        createServerThread();

        registerConfigCommandDelayed();

        //register the commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("bb").executes(BingoBrewers::configCommand));
            dispatcher.register(ClientCommandManager.literal("bingobrewers").executes(BingoBrewers::configCommand));
            // Debug command used to dump the list of currently active huds
            /*dispatcher.register(ClientCommandManager.literal("bbdebughud").executes(c -> {
                Log.info("Active huds: ");
                for(Hud hud : HudManager.activeHuds) {
                    Log.info(hud.getClass() + ": " + hud.isExpired() + (hud instanceof TimedHud th? " current: " + System.currentTimeMillis() + " end: " +
                        (th.getStartTime() + th.getDisplayTime()): "Not timed"));
                }
                return 1;
            }));*/
        });

        autoUpdater.registerUpdateCheck();

        minecraftColors.put("§0", 0x000000);  // Black
        minecraftColors.put("§1", 0x0000AA);  // Dark Blue
        minecraftColors.put("§2", 0x00AA00);  // Dark Green
        minecraftColors.put("§3", 0x00AAAA);  // Dark Aqua
        minecraftColors.put("§4", 0xAA0000);  // Dark Red
        minecraftColors.put("§5", 0xAA00AA);  // Dark Purple
        minecraftColors.put("§6", 0xFFAA00);  // Gold
        minecraftColors.put("§7", 0xAAAAAA);  // Gray
        minecraftColors.put("§8", 0x555555);  // Dark Gray
        minecraftColors.put("§9", 0x5555FF);  // Blue
        minecraftColors.put("§a", 0x55FF55);  // Green
        minecraftColors.put("§b", 0x55FFFF);  // Aqua
        minecraftColors.put("§c", 0xFF5555);  // Red
        minecraftColors.put("§d", 0xFF55FF);  // Light Purple
        minecraftColors.put("§e", 0xFFFF55);  // Yellow
        minecraftColors.put("§f", 0xFFFFFF);  // White

        HypixelModAPI.getInstance().createHandler(ClientboundPingPacket.class, HypixelPackets::onPingPacket);
        HypixelModAPI.getInstance().createHandler(ClientboundPartyInfoPacket.class, HypixelPackets::onPartyInfoPacket);
        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, HypixelPackets::onLocationEvent);
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);

        try {
            AutoConfig.register(BingoBrewersConfig.class, ConfigSerializer::new);
            GuiRegistry registry = AutoConfig.getGuiRegistry(BingoBrewersConfig.class);
            registry.registerTypeProvider(new ColorGuiProvider(), Color.class);
        } catch (Exception e) {
            Log.error("An error occurred while loading the configuration file", e);
        }

        PlayerInfo.registerEvents();
        CHChests.registerEvents();
        CHWaypoints.initRendering();
        HypixelPackets.registerEvents();

        HudManager.initialize();
    }

    public static void createServerThread() {
        try {
            ServerConnection serverConnection = new ServerConnection();
            Thread serverThread = new Thread(serverConnection);
            serverThread.start();
        } catch (Exception e) {
            Log.info("Server Connection Error: " + e.getMessage(), e);
        }
    }

    public void sendPacket(HypixelPacket packet) {
        System.out.println("packet time: " + (System.currentTimeMillis() - lastPacketSentAt));
        if (System.currentTimeMillis() - lastPacketSentAt > 2500) {
            lastPacketSentAt = System.currentTimeMillis();
            System.out.println("sending packet to hp");
            HypixelModAPI.getInstance().sendPacket(packet);
            lastPacketSent = packet;
            waitingForPacketResponse = true;
        } else {
            packetHold.add(packet);
        }
    }
}
