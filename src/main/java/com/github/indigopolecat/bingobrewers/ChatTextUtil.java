package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatTextUtil {

    public static boolean cancelLocRawMessage = false;
    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        if (message.startsWith("{") && message.endsWith("}") && cancelLocRawMessage) {
            event.setCanceled(true);
            cancelLocRawMessage = false;
        }
    }
}
