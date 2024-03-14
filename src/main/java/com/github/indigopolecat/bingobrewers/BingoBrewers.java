package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.github.indigopolecat.bingobrewers.commands.ConfigCommand;
import com.github.indigopolecat.bingobrewers.util.AutoUpdater;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import com.github.indigopolecat.events.PacketListener;

@Mod(modid = "bingobrewers", version = "0.3", useMetadata = true)
public class BingoBrewers {
    public static BingoBrewersConfig config;

    public static final String version = "v0.3-beta";

    public static volatile TitleHud activeTitle;
    public static volatile Client client;
    // controls which server is connected to
    public static final boolean TEST_INSTANCE = false;
    public static boolean onHypixel = false;

    public static AutoUpdater autoUpdater = new AutoUpdater();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Packets doneLoading = new Packets();
        MinecraftForge.EVENT_BUS.register(doneLoading);
        MinecraftForge.EVENT_BUS.register(new ChestInventories());
        MinecraftForge.EVENT_BUS.register(new PacketListener());
        MinecraftForge.EVENT_BUS.register(new CHChests());
        MinecraftForge.EVENT_BUS.register(new PlayerInfo());
        MinecraftForge.EVENT_BUS.register(new HudRendering());
        MinecraftForge.EVENT_BUS.register(new ChatTextUtil());
        MinecraftForge.EVENT_BUS.register(autoUpdater);
        config = new BingoBrewersConfig();
        createServerThread();
        ClientCommandHandler.instance.registerCommand(new ConfigCommand());
    }

    public static void createServerThread() {
        try {
            ServerConnection serverConnection = new ServerConnection();
            Thread serverThread = new Thread(serverConnection);
            serverThread.start();
        } catch (Exception e) {
            LoggerUtil.LOGGER.info("Server Connection Error: " + e.getMessage());
        }
    }
}
