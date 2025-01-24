package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.bingobrewers.Hud.TitleHud;
import com.github.indigopolecat.bingobrewers.commands.ConfigCommand;
import com.github.indigopolecat.bingobrewers.commands.TempWarpCommand;
import com.github.indigopolecat.bingobrewers.util.AutoUpdater;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import com.github.indigopolecat.events.HypixelPackets;
import com.github.indigopolecat.events.Packets;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import com.github.indigopolecat.events.PacketListener;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod(modid = "bingobrewers", version = "0.4", useMetadata = true)
public class BingoBrewers {
    public static BingoBrewersConfig config;
    public static BingoBrewers INSTANCE;

    public static final String version = "v0.3.6-beta";

    public static volatile TitleHud activeTitle;
    public static volatile Client client;
    // controls which server is connected to
    public static final boolean TEST_INSTANCE = true;
    public static boolean onHypixel = false;

    public static AutoUpdater autoUpdater = new AutoUpdater();
    public static HashMap<String, Integer> minecraftColors = new HashMap<>();


    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        INSTANCE = this;
        Packets doneLoading = new Packets();
        MinecraftForge.EVENT_BUS.register(doneLoading);
        MinecraftForge.EVENT_BUS.register(new ChestInventories());
        MinecraftForge.EVENT_BUS.register(new PacketListener());
        MinecraftForge.EVENT_BUS.register(new CHChests());
        MinecraftForge.EVENT_BUS.register(new PlayerInfo());
        MinecraftForge.EVENT_BUS.register(new SplashHud());
        MinecraftForge.EVENT_BUS.register(new ChatTextUtil());
        MinecraftForge.EVENT_BUS.register(new Warping());
        MinecraftForge.EVENT_BUS.register(autoUpdater);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HypixelPackets());

        config = new BingoBrewersConfig();
        config.preload();

        createServerThread();
        createConstantsOnLaunch();

        ClientCommandHandler.instance.registerCommand(new ConfigCommand());
        ClientCommandHandler.instance.registerCommand(new TempWarpCommand());

        HypixelModAPI.getInstance().registerHandler(ClientboundPingPacket.class, HypixelPackets::onPingPacket);
        HypixelModAPI.getInstance().registerHandler(ClientboundPartyInfoPacket.class, HypixelPackets::onPartyInfoPacket);
        HypixelModAPI.getInstance().registerHandler(ClientboundLocationPacket.class, HypixelPackets::onLocationEvent);
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
    }


    public static void createServerThread() {
        try {
            ServerConnection serverConnection = new ServerConnection();
            Thread serverThread = new Thread(serverConnection);
            serverThread.start();
            PacketProcessing.setClientInstance(serverConnection);
        } catch (Exception e) {
            LoggerUtil.LOGGER.info("Server Connection Error: " + e.getMessage());
        }
    }


    public static CopyOnWriteArrayList<HypixelPacket> packetHold = new CopyOnWriteArrayList<>();
    public static HypixelPacket lastPacketSent;
    public static long lastPacketSentAt = 0;
    public static boolean waitingForPacketResponse;
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

    public static void createConstantsOnLaunch() {
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

        Warping.createPartyMessageMatchers();
    }
}
