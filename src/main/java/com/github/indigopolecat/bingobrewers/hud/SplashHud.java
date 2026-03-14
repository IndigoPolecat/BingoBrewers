package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.bingobrewers.util.ServerUtils;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import lombok.*;

import java.util.*;

public class SplashHud extends TextHud {
    @Getter private final long startTime = System.currentTimeMillis();
    public static final Map<String, SplashNotificationInfo> splashes = Collections.synchronizedMap(new HashMap<>());
    private static SplashHud INSTANCE;
    private static boolean notifiedScale = false;
    
    public SplashHud() {
        super(0xFFFFFFFF);
    }
    
    public static void addSplash(KryoNetwork.SplashNotification notif) {
        if(splashes.get(notif.splash) == null) splashes.put(notif.splash, new SplashNotificationInfo(notif));
        else splashes.get(notif.splash).update(notif);

        if (INSTANCE == null) {
            INSTANCE = new SplashHud();
            HudManager.addNewHud(INSTANCE);
        }
    }
    
    public static void removeSplash(String id) {
        splashes.remove(id);
        
        if(splashes.isEmpty()) {
            HudManager.removeHud(INSTANCE);
            INSTANCE = null;
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        final BingoBrewersConfig.SplashHudSettings config = BingoBrewersConfig.getConfig().splashConfig;
        // Check this during render so that someone can toggle the setting on or join skyblock, and if there is an active splash it will show up immediately.
        if(!BingoBrewersConfig.getConfig().splashNotificationsEnabled) return;
        if(!(BingoBrewersConfig.getConfig().splashNotificationsOutsideSkyblock || ServerUtils.isBingo())) return;
        
        //Setup values from config every frame, to allow for hot-edits
        offsetX = config.x;
        offsetY = config.y;
        try {
            setScale(config.scale/100f);
        } catch (IllegalArgumentException e) {
            if(!notifiedScale) {
                notifiedScale = true;
                Log.warn("Config.hud.scale is set to an invalid value: " + config.scale + "(scaled: " + config.scale / 100f + ")");
                Log.info("Invalid scale", e);
            }
        }
        
        List<String> textToRender = new ArrayList<>();
        
        final int maxTime = config.displayTime;
        
        splashes.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().lastNotif.timestamp > maxTime * 1000L);
        // may want to worry about sorting this by age, map isn't sorted
        splashes.forEach((id, splash) -> {
            Log.LOG.debug("Adding {} to the rendering queue", id);
            textToRender.addAll(splash.getText());
            textToRender.add(" "); // empty buffer line between splashes
        });
        
        text = textToRender.toArray(String[]::new);
        
        Log.LOG.debug("Rendering {} lines", text.length);
        try {
            Log.LOG.debug("First line: {}", text[0]);
            super.render(graphics, tickCounter);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            Log.warn("There is no text to display, but this instance is still valid");
            Log.info("Removing " + this + " as there are no valid splashes");
            INSTANCE = null;
            HudManager.removeHud(this);
        }
    }
    
    @Override
    public boolean isExpired() {
        return splashes.isEmpty();
    }
}
