package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.indigopolecat.bingobrewers.BingoBrewersConfig.displayEggTimerReset;

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
        } else if (message.equals("You laid an egg!") && displayEggTimerReset) {
            scheduler.schedule(() -> {
                TitleHud titleHud = new TitleHud("You can lay an egg again", Color.WHITE.getRGB(), 1000);
                ServerConnection serverConnection = new ServerConnection();
                serverConnection.setActiveHud(titleHud);
            }, 5, TimeUnit.SECONDS);
        }
    }
}