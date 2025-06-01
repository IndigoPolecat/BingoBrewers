package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.ChestInventories;
import com.github.indigopolecat.bingobrewers.PlayerInfo;
import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.events.Packets;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.github.indigopolecat.bingobrewers.ChestInventories.removeFormatting;
import static com.github.indigopolecat.bingobrewers.Hud.SplashInfoHud.activeSplashes;

public class SplashUtils {
    public static void setPlayerCount(int playercount, String splashID) {
        // this is run if the player is either in the server ID specified in the splash, or in the same lobby as the splasher IGN
        // This check is only run when the tablist playercount is updated, definetily not optimal but should be reasonably accurate
        int currentCount = PlayerInfo.playerCount;
        PlayerInfo.playerCount = playercount;
        // If the player count has changed
        if (currentCount != playercount) {
            KryoNetwork.PlayerCount count = new KryoNetwork.PlayerCount();
            count.playerCount = playercount;
            if (PlayerInfo.playerHubNumber == null) {
                System.out.println("Player hub number is null");
                return;
            }
            count.hub = PlayerInfo.playerHubNumber;
            count.serverID = PlayerInfo.currentServer;
            count.splashID = splashID;

            ServerConnection serverConnection = new ServerConnection();
            serverConnection.sendPlayerCount(count);
        }
    }

    public static void setReadyToNotify(String hub, boolean dungeonHub) {
        PlayerInfo.readyToNotify = true;
        PlayerInfo.splashHubNumberForNotification = hub;
        PlayerInfo.readyToNotifyDungeon = dungeonHub;
        PlayerInfo.lastNotification = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        World world = event.world;
        if (event.entity instanceof EntityPlayer) {
            if (activeSplashes.isEmpty()) return;
            PlayerInfo.currentRenderedPlayerEntities.clear(); // super inefficient but I'm pretty sure forge doesn't have an equivalant event for despawning an entity so this it is.
            for (EntityPlayer player : world.playerEntities) {
                PlayerInfo.currentRenderedPlayerEntities.add(player.getName());
            }
        }
    }

    public static boolean hubSelectorOpen;
    public static boolean dungeonHubSelectorOpen;
    public static HashSet<String> splashServerIDs = new HashSet<>(); // used to update the rendered item

    @SubscribeEvent
    public void onInventoryLastItemLoaded(Packets.InventoryLoadingDoneEvent event) {
        if (hubSelectorOpen || dungeonHubSelectorOpen) {
            List<ItemStack> chestInventory = ChestInventories.containerChest.getInventory();
            // Remove the last 36 slots in the chest inventory, which are the player inventory
            chestInventory.subList(chestInventory.size() - 36, chestInventory.size()).clear();

            // loop through the items in the chest
            for (ItemStack item : chestInventory) {
                // verify the item slot isn't empty
                if (item == null) continue;

                // Get the lore of the item
                List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                String hubNumber = null;
                String server = null;
                // loop through the lore lines of the item
                for (String s : itemLore) {
                    // look for the lore line that contains the hub number
                    if (s.contains("Hub #")) {
                        // Match the hub number and remove formatting codes
                        hubNumber = s.replaceAll("SkyBlock Hub #(\\d+)", "$1");
                        hubNumber = hubNumber.replaceAll("Dungeon Hub #(\\d+)", "$1");
                        hubNumber = removeFormatting(hubNumber);
                    } else if (s.contains("Server:") && hubNumber != null) { // Look for the lore line containing the server id, but if the hub number hasn't been set yet ignore
                        // Match the server id and remove formatting codes
                        server = s.replaceAll("Server: ((mini|mega)\\d{1,4}[a-zA-Z]{1,4})", "$1");
                        server = removeFormatting(server);

                    }
                }

                // if we're in a hub selector, add the server and hub number to the hubServerMap
                if (hubNumber != null && server != null) {
                    if (hubSelectorOpen) {
                        for (SplashNotificationInfo info : activeSplashes) {
                            if (!info.hubNumber.equals(hubNumber) && info.serverID.equalsIgnoreCase(server)) {
                                info.hubNumber = hubNumber; // update the hub number client side to reflect the server ID found in the hub selector
                            }
                        }
                    } else if (dungeonHubSelectorOpen) {
                        for (SplashNotificationInfo info : activeSplashes) {
                            if (!info.hubNumber.equals(hubNumber) && info.serverID.equalsIgnoreCase(server)) {
                                info.hubNumber = hubNumber; // update the hub number client side to reflect the server ID found in the hub selector
                            }
                        }
                    }
                }
            }
        }
    }


}
