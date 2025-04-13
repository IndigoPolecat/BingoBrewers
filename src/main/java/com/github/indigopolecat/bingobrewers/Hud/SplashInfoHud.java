package com.github.indigopolecat.bingobrewers.Hud;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.PlayerInfo;
import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;
import dev.deftu.omnicore.client.render.OmniMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplashInfoHud extends LegacyHud {
    public static CopyOnWriteArrayList<SplashNotificationInfo> activeSplashes = new CopyOnWriteArrayList<>();
    public static SplashNotificationInfo exampleNotificationInfo = new SplashNotificationInfo(true);
    public static float unscaledHudHeight;
    public static int MAX_SPLASH_NOTIFICATIONS_VISIBLE = 2;
    public static int MAX_LINES_OF_INFO_BEFORE_CUTOFF = 6; // how many lines of info (non-prefix) are shown before cutting it off to preserve space
    public static int MAX_STRING_WIDTH = 300; // controls length of strings before they get wrapped to a new line
    public static String PREFIX_INFO_DIVIDER = ": "; // placed between the prefix line (e.g. "Hub") and the actual info (e.g. "14") as a separator
    public static Color COLOR_INFO = new Color(255, 255, 255); // White
    public static Color COLOR_DIVIDER = new Color(255, 255, 255); // White
    public static Color COLOR_PREFIX = new Color(255, 255, 85); // Yellow
    public static float LINE_GAP = 10; // gap between lines, multiplied by scaleY
    public static float TOP_BOTTOM_PADDING = 3; // padding between the edge of the HUD's box and where the text is rendered vertically, scaled

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public float getWidth() {
        return MAX_STRING_WIDTH;
    }

    @Override
    public void setWidth(float v) {

    }

    @Override
    public float getHeight() {
        return unscaledHudHeight;
    }

    @Override
    public void setHeight(float v) {

    }

    @Override
    public void render(@NotNull OmniMatrixStack omniMatrixStack, float x, float y, float scaleX, float scaleY) {
        // don't divide by 0
        if (scaleX < 0.3) {
            scaleX = 0.3f;
        }

        if (scaleY < 0.3) {
            scaleY = 0.3f;
        }

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int heightScaled = scaledResolution.getScaledHeight();

        x = (x / scaleX);
        y = (y / scaleY);
        y = Math.min(y, heightScaled); // this is supposed to be heightScaled - getHeight()

        omniMatrixStack.scale(scaleX, scaleY, 1f);

        boolean example = !isReal();

        if (!PlayerInfo.onBingo && !example) return; // non-profile bingo splashes setting was here
        // limbo isn't listed, not sure if it should be, will see if anyone complains.
        if (PlayerInfo.inSkyblockOrPTL && !BingoBrewersConfig.splashNotificationsOutsideSkyblock && !example) return;
        if (!BingoBrewers.onHypixel) return;

        float heightToRenderAt = ((y + TOP_BOTTOM_PADDING) * scaleY); // advances each line drawn

        if (example && (activeSplashes.isEmpty() || !BingoBrewersConfig.splashNotificationsEnabled)) {
            // check the example actually exists. Why would it not? no idea
            if (exampleNotificationInfo != null && exampleNotificationInfo.example) {
                renderSplashNotification(exampleNotificationInfo, x, y, heightToRenderAt);
            }

        } else if (BingoBrewersConfig.splashNotificationsEnabled) {
            // shows the most recent splashes
            for (int i = 0; i < MAX_SPLASH_NOTIFICATIONS_VISIBLE; i++) {
                SplashNotificationInfo splashNotificationInfo = activeSplashes.get(i);
                heightToRenderAt = renderSplashNotification(splashNotificationInfo, x, scaleY, heightToRenderAt);
            }
        }

        unscaledHudHeight = (heightScaled + (TOP_BOTTOM_PADDING * scaleY)) / scaleY;
    }

    public static float renderSplashNotification(SplashNotificationInfo info, float x, float scaleY, float heightToRenderAt) {

        String hubPrefix = info.dungeonHub ? SplashNotificationInfo.DUNGEON_HUB : SplashNotificationInfo.HUB;
        String hubInfo = info.serverID.isEmpty() ? info.hubNumber : info.hubNumber + "(" + info.serverID + ")";
        heightToRenderAt = renderSplashHudSection(hubPrefix, Collections.singletonList(hubInfo), x, scaleY, heightToRenderAt);

        if (BingoBrewersConfig.showPlayerCount) {
            heightToRenderAt = renderSplashHudSection(SplashNotificationInfo.PLAYER_COUNT, Collections.singletonList(info.lobbyPlayerCount), x, scaleY, heightToRenderAt);
        }

        if (BingoBrewersConfig.showSplasher) {
            heightToRenderAt = renderSplashHudSection(SplashNotificationInfo.SPLASHER, Collections.singletonList(info.splasherIGN), x, scaleY, heightToRenderAt);
        }

        if (BingoBrewersConfig.showParty) {
            heightToRenderAt = renderSplashHudSection(SplashNotificationInfo.PARTY, Collections.singletonList(info.bingoPartyJoinCommand), x, scaleY, heightToRenderAt);
        }

        if (BingoBrewersConfig.showLocation) {
            heightToRenderAt = renderSplashHudSection(SplashNotificationInfo.LOCATION, Collections.singletonList(info.location), x, scaleY, heightToRenderAt);
        }

        if (BingoBrewersConfig.showNote) {
            heightToRenderAt = renderSplashHudSection(SplashNotificationInfo.NOTE, info.splasherNotes, x, scaleY, heightToRenderAt);
        }

        return heightToRenderAt + (LINE_GAP * scaleY);
    }

    public static float renderSplashHudSection(String prefix, List<String> info, float x, float scaleY, float heightToRenderAt) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;

        List<String> prefixList = fontRenderer.listFormattedStringToWidth(prefix, MAX_STRING_WIDTH);

        float nextStart = 0; // the x offset created by the last prefix line. This tells the info where to render so it is directly next to the prefix.
        for (String prefixLine : prefixList) {
            nextStart = renderBoldText(prefixLine, x, heightToRenderAt, COLOR_PREFIX.getRGB()) - x;
            heightToRenderAt += LINE_GAP * scaleY;
        }

        ArrayList<String> wrappedInfo = new ArrayList<>(); // stores the info text once it has been wrapped into multiple lines based on the max string width
        for (String infoLine : info) {
            List<String> infoList = fontRenderer.listFormattedStringToWidth(infoLine, MAX_STRING_WIDTH);
            wrappedInfo.addAll(infoList);
        }

        String firstInfoLine;
        if (!COLOR_INFO.equals(COLOR_DIVIDER)) {
            firstInfoLine = wrappedInfo.get(0); // set the rest of the info to be drawn, not including the divider since we draw it here
            fontRenderer.drawStringWithShadow(PREFIX_INFO_DIVIDER, x + nextStart, heightToRenderAt, COLOR_DIVIDER.getRGB());
            nextStart += fontRenderer.getStringWidth(PREFIX_INFO_DIVIDER);
        } else {
            firstInfoLine = PREFIX_INFO_DIVIDER + wrappedInfo.get(0); // otherwise include the prefix divider with the first line because they are the same color
        }
        // draw the first line adjacent to the last line of the prefix using the nextStart variable
        fontRenderer.drawStringWithShadow(firstInfoLine, x + nextStart, heightToRenderAt, COLOR_INFO.getRGB());
        heightToRenderAt += LINE_GAP * scaleY;
        wrappedInfo.remove(0);

        // draw the remainder of the lines
        for (int i = 0; i < MAX_LINES_OF_INFO_BEFORE_CUTOFF; i++) {
            String infoLine;

            if (i < MAX_LINES_OF_INFO_BEFORE_CUTOFF - 1) {
                infoLine = wrappedInfo.get(i);
            } else if (wrappedInfo.size() > MAX_LINES_OF_INFO_BEFORE_CUTOFF) {
                // if this isn't the last line, then
                // trim the string to the max width minus the length of a "..." string at the end, allowing us to append "..." without going over the max width
                infoLine = fontRenderer.trimStringToWidth(wrappedInfo.get(i), MAX_STRING_WIDTH - fontRenderer.getStringWidth("...")) + "...";
            } else {
                // if this is the last line, don't add "..." because there is nothing after
                infoLine = wrappedInfo.get(i);
            }

            fontRenderer.drawStringWithShadow(infoLine, x, heightToRenderAt, COLOR_INFO.getRGB());
            heightToRenderAt += LINE_GAP * scaleY;
        }

        return heightToRenderAt;
    }

    // renders bold text and returns its length
    public static float renderBoldText(String text, float x, float y, int color) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        // Render the text slightly offset for a bold effect and add extra space after each character
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int charWidth = fontRenderer.getCharWidth(character);
            // I don't know what I changed but when I switched to these, the y offset was fixed, switched back and it's still fixed
            //Platform.getGLPlatform().drawText(String.valueOf(character), x, y, color, false);
            //Platform.getGLPlatform().drawText(String.valueOf(character), x + 1, y, color, false);
            fontRenderer.drawString(String.valueOf(character), x + 1, y, color, false);
            fontRenderer.drawString(String.valueOf(character), x, y, color, false);
            // Add extra space after rendering each character
            x += charWidth + 1;
        }
        return x;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent tick) {
        if (!tick.phase.equals(TickEvent.Phase.END)) return;
        // remove outdated splash info
        activeSplashes.removeIf(splashNotificationInfo -> System.currentTimeMillis() - splashNotificationInfo.timestamp > 120000);
    }

    @NotNull
    @Override
    public String title() {
        return "Splash Hud";
    }

    @NotNull
    @Override
    public Category category() {
        return Category.getINFO();
    }

}
