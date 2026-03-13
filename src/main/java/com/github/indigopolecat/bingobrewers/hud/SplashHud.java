package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.Log;
import com.github.indigopolecat.bingobrewers.util.ServerUtils;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import lombok.*;

import java.util.*;

public class SplashHud extends TextHud {
    @Getter private final long startTime = System.currentTimeMillis();
    @Getter private static final Map<String, SplashNotificationInfo> splashes = Collections.synchronizedMap(new HashMap<>());
    private static SplashHud INSTANCE;
    private static final String KEY_COLOR = "§e§l";
    
    public SplashHud() {
        super(0xFFFFFFFF);
    }
    
    public static void addSplash(SplashNotificationInfo info) {
        splashes.put(info.id, info);

        if (INSTANCE == null) {
            INSTANCE = new SplashHud();
            HudManager.addNewHud(INSTANCE);
        }

        //Setup values from config
        final BingoBrewersConfig.SplashHudSettings config = BingoBrewersConfig.getConfig().splashConfig;
        INSTANCE.offsetX = config.x;
        INSTANCE.offsetY = config.y;
    }
    
    public static void removeSplash(String id) {
        splashes.remove(id);
        
        if(splashes.isEmpty()) {
            HudManager.removeHud(INSTANCE);
            INSTANCE = null;
        }
    }
    
    private static ArrayList<String> createText(SplashNotificationInfo info) {
        ArrayList<String> builder = new ArrayList<>();
        final BingoBrewersConfig config = BingoBrewersConfig.getConfig();

        String hubPrefix = SplashNotificationInfo.HUB;

        if (info.dungeonHub) {
            hubPrefix = SplashNotificationInfo.DUNGEON_HUB;
        } else if (info.isPrivate) {
            hubPrefix = SplashNotificationInfo.PRIVATE_HUB;
        }


        String hubInfo = info.serverID.isEmpty() ? info.hub : info.hub + " (" + info.serverID + ")";
        builder.add(KEY_COLOR + hubPrefix + hubInfo);

        if(config.showPlayerCount && !info.lobbyPlayerCount.isEmpty()) builder.add(KEY_COLOR + SplashNotificationInfo.PLAYER_COUNT + info.lobbyPlayerCount);
        if(config.showSplasher) builder.add(KEY_COLOR + SplashNotificationInfo.SPLASHER + info.splasherIGN);
        if(config.showParty) builder.add(KEY_COLOR + SplashNotificationInfo.PARTY + info.bingoPartyJoinCommand);
        if(config.showLocation) builder.add(KEY_COLOR + SplashNotificationInfo.LOCATION + info.location);
        if(config.showNote) {
            // Add first element on the same line as the key ("Note: ")
            builder.add(KEY_COLOR + SplashNotificationInfo.NOTE + info.splasherNotes.getFirst());
            for (int i = 1; i < info.splasherNotes.size(); i++) builder.add(info.splasherNotes.get(i));
        }
        return builder;
    }
    
    @Override
    public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        // Check this during render so that someone can toggle the setting on or join skyblock, and if there is an active splash it will show up immediately.
        if(!BingoBrewersConfig.getConfig().splashNotificationsEnabled) return;
        if(!(BingoBrewersConfig.getConfig().splashNotificationsOutsideSkyblock || ServerUtils.isBingo())) return;

        List<String> textToRender = new ArrayList<>();
        
        final int maxTime = BingoBrewersConfig.getConfig().splashConfig.displayTime;
        
        splashes.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().timestamp > maxTime * 1000L);
        // may want to worry about sorting this by age, map isn't sorted
        splashes.forEach((id, splash) -> {
            Log.LOG.debug("Adding {} to the rendering queue", id);
            textToRender.addAll(createText(splash));
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
            HudManager.removeHud(INSTANCE);
            INSTANCE = null;
        }
    }
    
    @Override
    public boolean isExpired() {
        return splashes.isEmpty();
    }
}
