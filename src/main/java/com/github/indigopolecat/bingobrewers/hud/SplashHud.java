package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.kryo.KryoNetwork;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SplashHud extends TimedTextHud{
    @Getter private final long startTime = System.currentTimeMillis();
    private static final List<SplashHud> splashes = Collections.synchronizedList(new ArrayList<>());
    public final KryoNetwork.SplashNotification notif;
    
    public SplashHud(KryoNetwork.SplashNotification notification) {
        super(1000L * BingoBrewersConfig.getConfig().splashConfig.displayTime, 0xFFFFFFFF, createText(notification));
        notif = notification;
        
        //Setup values from config
        final BingoBrewersConfig.SplashHudSettings config = BingoBrewersConfig.getConfig().splashConfig;
        offsetX = config.x;
        offsetY = config.y;
        try {
            setScale(config.scale/100f);
        } catch (IllegalArgumentException e) {
            Log.warn("Config.hud.scale is set to an invalid value: " + config.scale);
            Log.info("Invalid scale", e);
        }
        
        if(getHud(notif.splash) != null) getHud(notif.splash).invalidate(); //If the splash got an update remove it manually
        
        splashes.add(this);
    }
    
    public static SplashHud getHud(String id) {
        return splashes.parallelStream().filter(h -> h.notif.splash.equals(id)).findFirst().orElse(null);
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
    public long getDisplayTime() {
        return 1000L * BingoBrewersConfig.getConfig().splashConfig.displayTime;
    }
    
    @Override
    public boolean expired() {
        return splashes.contains(this) && super.expired();
    }
    
    public void invalidate() {
        splashes.remove(this);
        HudManager.removeHud(this);
    }
}
