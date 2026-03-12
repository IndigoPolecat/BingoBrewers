package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import lombok.*;

import java.util.*;

public class SplashHud extends TextHud {
    @Getter private final long startTime = System.currentTimeMillis();
    private static final Map<String, SplashTimed> splashes = Collections.synchronizedMap(new HashMap<>());
    private static SplashHud INSTANCE;
    private static boolean notifiedScale = false;
    
    public SplashHud() {
        super(0xFFFFFFFF);
    }
    
    public static void addSplash(KryoNetwork.SplashNotification notification) {
        splashes.put(notification.splash, new SplashTimed(notification, System.currentTimeMillis()));
        
        if(INSTANCE == null) {
            INSTANCE = new SplashHud();
            HudManager.addNewHud(INSTANCE);
        }
        
        //Setup values from config
        final BingoBrewersConfig.SplashHudSettings config = BingoBrewersConfig.getConfig().splashConfig;
        INSTANCE.offsetX = config.x;
        INSTANCE.offsetY = config.y;
        try {
            INSTANCE.setScale(config.scale/100f);
        } catch (IllegalArgumentException e) {
            if(notifiedScale) return;
            notifiedScale = true;
            Log.warn("Config.hud.scale is set to an invalid value: " + config.scale + "(scaled: " + config.scale/100f + ")");
            Log.info("Invalid scale", e);
        }
    }
    
    public static void removeSplash(String id) {
        splashes.remove(id);
        
        if(splashes.isEmpty()) {
            HudManager.removeHud(INSTANCE);
            INSTANCE = null;
        }
    }
    
    private static String createText(KryoNetwork.SplashNotification notification) {
        StringBuilder builder = new StringBuilder();
        final BingoBrewersConfig config = BingoBrewersConfig.getConfig();
        
        if(notification.dungeonHub) builder.append("§l§6Dungeon ");
        
        builder.append("§l§6Hub:§r ").append(notification.hub).append(" (").append(notification.serverID).append(")");
        if(config.showSplasher) builder.append("\n§l§6Splasher:§r ").append(notification.splasher);
        if(config.showParty) builder.append("\n§l§6Party:§r ").append(notification.partyHost);
        if(config.showLocation) builder.append("\n§l§6Location:§r ").append(notification.location);
        if(config.showNote) {
            builder.append("\n§l§6Notes:§r ");
            for (String note : notification.note) builder.append(note).append("\n ");
        }
        return builder.toString();
    }
    
    @Override
    public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        List<String> textToRender = new ArrayList<>();
        
        final int maxTime = BingoBrewersConfig.getConfig().splashConfig.displayTime;
        
        splashes.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().startTime > maxTime * 1000L);
        splashes.forEach((id, splash) -> {
            Log.LOG.debug("Adding {} to the rendering queue", id);
            textToRender.addAll(Arrays.asList(createText(splash.notification).split("\n")));
        });
        
        text = textToRender.toArray(String[]::new);
        
        Log.LOG.debug("Rendering {} lines", text.length);
        try {
            Log.LOG.debug("First line: {}", text[0]);
            super.render(graphics, tickCounter);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            Log.warn("There is no text to display, but this instance is still valid");
            Log.info("Removing " + this + " as there are no valid splashes");
            HudManager.removeHud(INSTANCE);
            INSTANCE = null;
        }
    }
    
    @Override
    public boolean isExpired() {
        return splashes.isEmpty();
    }
    
    private record SplashTimed(KryoNetwork.SplashNotification notification, long startTime) { }
}
