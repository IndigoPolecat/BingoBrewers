package com.conutik.bingobrewers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class BingoBrewersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println(System.getProperty("java.class.path"));
        System.out.println("Hello from CoflTestClient!");
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("Joined the server!");
            if(MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("Hello!"), true);
            }
        });
    }
}
