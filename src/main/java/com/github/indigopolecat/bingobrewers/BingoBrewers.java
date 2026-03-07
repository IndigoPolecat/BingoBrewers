package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.github.indigopolecat.bingobrewers.config.ConfigSerializer;
import com.github.indigopolecat.bingobrewers.gui.ColorGuiProvider;
import com.github.indigopolecat.bingobrewers.hud.*;
import com.github.indigopolecat.bingobrewers.util.AutoUpdater;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.events.HypixelPackets;
import com.mojang.brigadier.context.CommandContext;
import lombok.*;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BingoBrewers implements ClientModInitializer {
    public static BingoBrewers INSTANCE;

    //public static volatile TitleHud activeTitle; //TODO(matita): redo this
    @Getter(onMethod_ = @Synchronized) @Setter(onMethod_ = @Synchronized)
    private static volatile Client client;
    // controls which server is connected to
    public static final boolean TEST_INSTANCE = true;
    public static final String version = "v0.4.0-beta+1.21";
    //TODO(matita): overridden detection for now, also it may better to be moved as ServerUtils.isHypixel()
    public static boolean onHypixel = true; // TODO(polecat): this doesn't work if someone is using a proxy to connect to hypixel, need better detection

    public static AutoUpdater autoUpdater = new AutoUpdater();
    public static HashMap<String, Integer> minecraftColors = new HashMap<>();
    
    public static CopyOnWriteArrayList<HypixelPacket> packetHold = new CopyOnWriteArrayList<>();
    public static HypixelPacket lastPacketSent;
    public static long lastPacketSentAt = 0;
    public static boolean waitingForPacketResponse;
   
   public static int configCommand(CommandContext<FabricClientCommandSource> context) {
       Log.info("Opening config menu");
       Screen configScreen = AutoConfig.getConfigScreen(BingoBrewersConfig.class, Minecraft.getInstance().screen).get();
       Minecraft.getInstance().setScreen(configScreen);
       Log.info("configScreen present=" + (configScreen == null));
       return 1; //1 is success
   }
   
    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        createServerThread();
        
        //register the commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("bb").executes(BingoBrewers::configCommand)
                                                    .redirect(dispatcher.register(ClientCommandManager.literal("bingobrewers").executes(BingoBrewers::configCommand))));
            dispatcher.register(ClientCommandManager.literal("bingobrewerstestgui").executes(ctx -> {
                HudManager.addNewHud(new TitleHud(1000*15, "Test string", 0x47EB62));
                HudManager.addNewHud(new TimedTextHud(1000*30, 0xFFFFFFFF,1.5f,
                    "This is a §1 example §rtext", "that spans multiple lines", "§k abcdefg", "Is it scaled?"));
                return 1; //1 is success
            }));
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
