package com.github.indigopolecat.bingobrewers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.indigopolecat.bingobrewers.BingoBrewersConfig.displayEggTimerReset;
import static com.github.indigopolecat.bingobrewers.BingoBrewersConfig.playEggTimerResetSound;

public class ChatTextUtil {
    public static boolean cancelLocRawMessage = false;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        if (message.startsWith("{") && message.endsWith("}") && cancelLocRawMessage) {
            event.setCanceled(true);
            cancelLocRawMessage = false;
            PlayerInfo.lastPositionUpdate = System.currentTimeMillis();
        } else if (message.equals("You laid an egg!") && (displayEggTimerReset || playEggTimerResetSound)) {
            notifyEggLayingReady();
        }
    }

    private static void notifyEggLayingReady() {
        scheduler.schedule(() -> {
            if (displayEggTimerReset) {
                TitleHud titleHud = new TitleHud("You can lay an egg again", Color.GREEN.getRGB(), 1000);
                ServerConnection serverConnection = new ServerConnection();
                serverConnection.setActiveHud(titleHud);
            }
            if (playEggTimerResetSound) {
                EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                player.playSound("bingobrewers:skill_xp", BingoBrewersConfig.splashNotificationVolume / 100f, 1.0f);
            }
        }, 5, TimeUnit.SECONDS);
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        // Render the waypoints
        System.out.println("rendering");
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            System.out.println("rendering waypiont: " + waypoint.shortName);
            CHWaypoints.renderPointLabel(waypoint, waypoint.pos, event.partialTicks);
        }
    }
}