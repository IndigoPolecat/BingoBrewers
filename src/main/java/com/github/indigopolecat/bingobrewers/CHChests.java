package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;

public class CHChests {

    static final Pattern ITEM_PATTERN = Pattern.compile("^\\W*([\\w][\\w\\s]+?)(?:\\sx([\\d,]+))?$", Pattern.CASE_INSENSITIVE);
    static final int ITEM_NAME_GROUP = 1;
    static final int ITEM_COUNT_GROUP = 2;
    static final String LOOT_CHAT_MESSAGE_START = "  LOOT CHEST COLLECTED ";
    static final String LOOT_CHAT_MESSAGE_END = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
    static List<String> recentChatMessages = new ArrayList<>();
    static Queue<BlockPos> listeningChests = new LinkedList<>();
    static HashSet<BlockPos> hardstone = new HashSet<>();
    static boolean addMessages = false;

    public static void registerEvents() {
        UseBlockCallback.EVENT.register((player, level, interactionHand, blockHitResult) -> {
            if (interactionHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            BlockPos pos = blockHitResult.getBlockPos();
            if (!(level.getBlockState(pos).getBlock() instanceof ChestBlock)) return InteractionResult.PASS;
            Block blockBelow = level.getBlockState(pos.below()).getBlock();
            if (blockBelow == Blocks.STONE || blockBelow == Blocks.AIR) return InteractionResult.PASS;

            listeningChests.add(pos);
            return InteractionResult.PASS;
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            onChatMessage(message.getString());
        });

    }

    public static void onChatMessage(String message) {
        if (!PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows")) return;
        if (!BingoBrewersConfig.getConfig().crystalHollowsWaypointsToggle) return;

        if (message.equals(LOOT_CHAT_MESSAGE_START)) {
            addMessages = true;
            recentChatMessages.clear();
        } else if (message.equals(LOOT_CHAT_MESSAGE_END)) {
            if (addMessages) {
                addMessages = false;
                parseChat();
            }
        } else if (addMessages) {
            recentChatMessages.add(message);
        }
    }

    public static void parseChat() {
        BlockPos coords = listeningChests.remove();
        hardstone.add(coords);

        KryoNetwork.ClientSendCHItems chestLoot = new KryoNetwork.ClientSendCHItems();
        chestLoot.server = PlayerInfo.currentServer;
        chestLoot.day = 1;
        if (Minecraft.getInstance().level != null) {
            chestLoot.day = (int) (Minecraft.getInstance().level.getGameTime() / 24000);
        }

        chestLoot.x = coords.getX();
        chestLoot.y = coords.getY();
        chestLoot.z = coords.getZ();

        for (String message : recentChatMessages) {
            KryoNetwork.CHChestItem chestItem = new KryoNetwork.CHChestItem();

            Matcher matcher = ITEM_PATTERN.matcher(message);
            try {
                if (!matcher.find()) continue;

                chestItem.name = matcher.group(ITEM_NAME_GROUP);
                if (chestItem.name == null) continue;

                chestItem.count = matcher.group(ITEM_COUNT_GROUP);
                if (chestItem.count == null) {
                    chestItem.count = "1";
                } else {
                    chestItem.count = chestItem.count.replaceAll(",", "");
                }

                chestLoot.items.add(chestItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!chestLoot.items.isEmpty()) {
            ServerConnection.sendTCP(chestLoot);
        }
        recentChatMessages.clear();
    }
}
