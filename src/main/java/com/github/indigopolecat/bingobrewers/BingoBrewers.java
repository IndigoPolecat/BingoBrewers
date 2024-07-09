package com.github.indigopolecat.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.bingobrewers.Hud.TitleHud;
import com.github.indigopolecat.bingobrewers.commands.ConfigCommand;
import com.github.indigopolecat.bingobrewers.util.AutoUpdater;
import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import com.github.indigopolecat.events.HypixelPackets;
import com.github.indigopolecat.events.Packets;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import com.github.indigopolecat.events.PacketListener;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(modid = "bingobrewers", version = "0.3.3", useMetadata = true)
public class BingoBrewers {
    public static BingoBrewersConfig config;

    public static final String version = "v0.3.3-beta";

    public static volatile TitleHud activeTitle;
    public static volatile Client client;
    // controls which server is connected to
    public static final boolean TEST_INSTANCE = false;
    public static boolean onHypixel = false;

    public static AutoUpdater autoUpdater = new AutoUpdater();
    public static HashMap<String, Integer> minecraftColors = new HashMap<>();


    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
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
        config = new BingoBrewersConfig();
        createServerThread();
        ClientCommandHandler.instance.registerCommand(new ConfigCommand());

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

        HypixelModAPI.getInstance().registerHandler(ClientboundPingPacket.class, HypixelPackets::onPingPacket);
        HypixelModAPI.getInstance().registerHandler(ClientboundPartyInfoPacket.class, HypixelPackets::onPartyInfoPacket);
        HypixelModAPI.getInstance().setPacketSender(this::sendPacket);
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


    private static final Logger LOGGER = Logger.getLogger("HypixelModAPI");
    private NetHandlerPlayClient netHandler;
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        netHandler = (NetHandlerPlayClient) event.handler;
        event.manager.channel().pipeline().addBefore("packet_handler", "hypixel_mod_api_packet_handler", HypixelPacketHandler.INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        netHandler = null;
    }

    public boolean sendPacket(HypixelPacket packet) {
        if (netHandler == null) {
            return false;
        }
        if (!netHandler.getNetworkManager().isChannelOpen()) {
            LOGGER.warning("Attempted to send packet while channel is closed!");
            netHandler = null;
            return false;
        }
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        PacketSerializer serializer = new PacketSerializer(buf);
        packet.write(serializer);
        netHandler.addToSendQueue(new C17PacketCustomPayload(packet.getIdentifier(), buf));
        return true;
    }

    @ChannelHandler.Sharable
    private static class HypixelPacketHandler extends SimpleChannelInboundHandler<Packet<?>> {
        private static final HypixelPacketHandler INSTANCE = new HypixelPacketHandler();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet<?> msg) {
            ctx.fireChannelRead(msg);

            if (!(msg instanceof S3FPacketCustomPayload)) {
                return;
            }

            S3FPacketCustomPayload packet = (S3FPacketCustomPayload) msg;
            String identifier = packet.getChannelName();
            if (!HypixelModAPI.getInstance().getRegistry().isRegistered(identifier)) {
                return;
            }

            PacketBuffer buffer = packet.getBufferData();
            buffer.retain();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                try {
                    HypixelModAPI.getInstance().handle(identifier, new PacketSerializer(buffer));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to handle packet " + identifier, e);
                } finally {
                    buffer.release();
                }
            });
        }
    }
}
