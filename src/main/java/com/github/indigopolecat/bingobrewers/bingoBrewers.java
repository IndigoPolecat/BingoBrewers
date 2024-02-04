package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Server;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import com.github.indigopolecat.events.PacketListener;

@Mod(modid = "bingobrewers", version = "0.1", useMetadata=true)
public class bingoBrewers {
    private BingoBrewersConfig config;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Packets doneLoading = new Packets();
        MinecraftForge.EVENT_BUS.register(new BingoShop());
        MinecraftForge.EVENT_BUS.register(doneLoading);
        MinecraftForge.EVENT_BUS.register(new PacketListener());
        MinecraftForge.EVENT_BUS.register(new CHChests());
        MinecraftForge.EVENT_BUS.register(new PlayerInfo());
        config = new BingoBrewersConfig();
        ServerConnection serverConnection = new ServerConnection();
        try {
            Thread serverThread = new Thread(serverConnection);
            serverThread.start();
        } catch (Exception e) {
            System.out.println("Server Connection Error: " + e.getMessage());
        }

    }
}
