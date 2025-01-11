package com.github.indigopolecat.bingobrewers.Hud;

import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.entity.EntityPlayerSP;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.hud.Hud;
import java.util.ArrayList;
import org.polyfrost.universal.UMatrixStack;
import com.github.indigopolecat.bingobrewers.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import static com.github.indigopolecat.bingobrewers.ServerConnection.setActiveHud;
import static java.lang.String.valueOf;

public class SplashHud extends Hud {
    public static final String DUNGEON_HUB = "Dungeon Hub";
    public static final String HUB = "Hub";
    public static final String SPLASHER = "Splasher";
    public static final String PARTY = "Party";
    public static final String LOCATION = "Location";
    public static final String NOTE = "Note";
    // The Hud renderer checks this every time it renders the splash hud for a list of active splashes
    public static ArrayList<HashMap<String, ArrayList<String>>> mapList = new ArrayList<>();
    public static ArrayList<String> keyOrder = new ArrayList<>();
    public static ArrayList<String> hubList = new ArrayList<>();

    float lastLineRenderedAtY = 0;
    int totalLines = 0;
    boolean listTooLong = false;
    long renderCounter = 0;
    // For some reason, latestSplash becomes bloated because it is stored in a config class, don't know how to fix but it's not a massive issue immediately, though it will inflate file size.
    public static ArrayList<Long> latestSplash = new ArrayList<>(2);
    float totalHeight = 0;
    float longestWidth = 0;
    float fontSize = 0.2F;
    // This is in this class so it is stored between game instances
    public static boolean onBingo = false;
    public static boolean inSkyblockorPTLobbyorLimbo = false;


    public SplashHud() {
        super(true);
        EventManager.INSTANCE.register(this);
    }


    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        // don't divide by 0
        if (scale < 0.3) {
            scale = 0.3f;
        }
        ArrayList<HashMap<String, ArrayList<String>>> infoPanel = new ArrayList<>();
        if(!onBingo && !example) return; // non-profile bingo splashes setting was here
        if(!SplashHud.inSkyblockorPTLobbyorLimbo && !BingoBrewersConfig.splashNotificationsOutsideSkyblock && !example) return;
        if(!BingoBrewers.onHypixel) return;
        if (example && (mapList.isEmpty() || !BingoBrewersConfig.splashNotificationsEnabled)) {
            // Example splash displayed in settings if none is active
            HashMap<String, ArrayList<String>> infoMap = getExampleHud();
            infoPanel.add(infoMap);
        } else if (BingoBrewersConfig.splashNotificationsEnabled) {
            renderCounter++;
            // The list containing each hashmap of info to be displayed
            infoPanel = mapList;
            // Only render the most recent splash and the oldest splash
            if (renderCounter % 60 == 0) {
                // Reset the counter even though it will never max lol
                renderCounter = 0;
                for (int i = 0; i < infoPanel.size(); i++) {
                    HashMap<String, ArrayList<String>> infoMap = infoPanel.get(i);
                    if (!infoMap.containsKey("Splash")) {
                        continue;
                    }

                    long time = Long.parseLong(infoMap.get("Time").get(0));

                    if (System.currentTimeMillis() - time > 120000) {
                        String hubNumber = infoMap.get("Hub").get(1).substring(2);
                        hubList.remove(hubNumber);
                        hubList.remove("DH" + hubNumber);
                        mapList.remove(infoMap);
                        latestSplash.remove(time);
                        if (PlayerInfo.playerHubNumber == null) {
                            PlayerInfo.inSplashHub = false;
                            continue;
                        }
                        if (PlayerInfo.playerHubNumber.equals(hubNumber) || PlayerInfo.playerHubNumber.equals("DH" + hubNumber)) {
                            PlayerInfo.inSplashHub = false;
                        }
                        continue;
                    }

                    latestSplash.add(time);

                    // This is a mess but it works, can't easily expand
                    long newSplash = 0;
                    long newerSplash = 0;
                    for (int j = 0; j < latestSplash.size(); j++) {
                        if (latestSplash.get(i) > newerSplash) {
                            newSplash = newerSplash;
                            newerSplash = latestSplash.get(i);
                        } else if (latestSplash.get(i) > newSplash) {
                            newSplash = latestSplash.get(i);
                        }

                    }

                    if (!latestSplash.contains(newerSplash) && !latestSplash.contains(newSplash)) {
                        infoPanel.remove(infoMap);
                    }
                }
            }

            x += 1;
        }
        // Render each item in the list
        renderSplashHud(infoPanel, x + 1, y, scale);

        // Set height of background
        totalHeight = totalLines * 10 - 7;

        // Reset at the end
        lastLineRenderedAtY = y + 3;

        // Dwarven Mines Event

    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (scale == 0) {
            scale = 1;
        }
        // the string wraps at 106
        if (longestWidth > 200 * scale) {
            longestWidth = 200;
        }

        return (longestWidth * scale) + 3;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        if (scale == 0) {
            scale = 1;
        }
        return totalHeight * scale;

    }

    public void renderSplashHud(List<HashMap<String, ArrayList<String>>> infoPanel, float x, float y, float scale) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int heightScaled = scaledResolution.getScaledHeight();

        fontSize = scale;
        longestWidth = 0;
        totalLines = 0;

        // set font size
        GL11.glPushMatrix();
        GL11.glScalef(fontSize, fontSize, scale);
        x = (x / fontSize);
        y = (y / fontSize );// (heightScaled + 2));
        y = Math.min(y, heightScaled - getHeight(scale, false));
        lastLineRenderedAtY /= scale;



        // loop through splashes to render
        for (HashMap<String, ArrayList<String>> map : infoPanel) {
            // set color of text
            // White
            Color colorText = new Color(255, 255, 255);
            // Yellow
            Color colorPrefix = new Color(255, 255, 85);
            int lineCount = 0;
            listTooLong = false;
            // loop through the hashmap of the splash
            for (int k = 0; k < keyOrder.size(); k++) {
                String key = keyOrder.get(k);

                // wrap width
                float maxWidth = 200 * scale;

                // render prefix
                ArrayList<String> splashInfo = map.get(key);
                String prefix = splashInfo.get(0);
                float nextStart = renderBoldText(prefix, (x), (lastLineRenderedAtY), colorPrefix.getRGB()) - x;

                // loop through the info
                infoRenderLoop:
                for (int j = 1; j < splashInfo.size(); j++) {
                    // reset the offset if there is more than one line
                    if (j == 2) nextStart = 0;

                    // find strings that are too long and trim them into a list of separate strings
                    String info = splashInfo.get(j);
                    List<String> wrappedLines = fontRenderer.listFormattedStringToWidth(info, (int) maxWidth);

                    for (int l = 0; l < wrappedLines.size(); l++) {
                        String line = wrappedLines.get(l);

                        if (fontRenderer.getStringWidth(line) + nextStart > longestWidth) {
                            longestWidth = fontRenderer.getStringWidth(line) + nextStart;
                        }
                        // reset the offset if there is more than one line
                        if (l == 1) nextStart = 0;
                        // if the line count is greater than 14, break the loop on the next iteration and set the end of the string to "..."
                        if (listTooLong) {
                            break infoRenderLoop;
                        }

                        if (lineCount >= 14) {
                            line = line.substring(0, line.length() - 2) + "...";
                            listTooLong = true;
                        }


                        // render the string
                        fontRenderer.drawStringWithShadow(line, (x + nextStart), (lastLineRenderedAtY), colorText.getRGB());
                        // mark the last y value we rendered a string at
                        lastLineRenderedAtY += 10;
                        // increase the line count
                        lineCount++;
                        // stop rendering if we're off screen
                        if (lastLineRenderedAtY > heightScaled - 10) {
                            listTooLong = true;
                            break;
                        }
                    }
                }
            }
            totalLines += lineCount + 1;
            // add a buffer between parts
            lastLineRenderedAtY += 10;
        }
        GL11.glPopMatrix();
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void renderGameOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        TitleHud activeTitle = BingoBrewers.activeTitle;
        if (activeTitle != null && activeTitle.displayTime > System.currentTimeMillis() - activeTitle.startTime) {
            activeTitle.drawTitle();
        }
    }

    public void updateMapList(KryoNetwork.SplashNotification notif, boolean sendNotif) {
        String hub = notif.message;
        if (hub == null) {
            hub = "Unknown Hub";
        }
        String splasher = notif.splasher;
        String partyHost = notif.partyHost;
        if (!partyHost.equals("No Party")) {
            partyHost = "/p join " + partyHost;
        }
        List<String> note = notif.note;
        // This should always be "Bea House" but is hard coded server side incase it ever needs to change quickly
        String location = notif.location;

        HashMap<String, ArrayList<String>> splashInfo = new HashMap<>();

        ArrayList<String> hubInfo = new ArrayList<>();
        if (!notif.dungeonHub) {
            hubInfo.add(HUB);
            hubList.add(hub);
        } else {
            hubInfo.add(DUNGEON_HUB);
            // Identify a hub as a dungeonhub to avoid mixing up regular hubs and dhubs
            hubList.add("DH" + hub);
        }
        hubInfo.add(": " + hub);
        splashInfo.put(HUB, hubInfo);

        ArrayList<String> splasherInfo = new ArrayList<>();
        splasherInfo.add(SPLASHER);
        splasherInfo.add(": " + splasher);
        splashInfo.put(SPLASHER, splasherInfo);

        ArrayList<String> partyInfo = new ArrayList<>();
        partyInfo.add("Bingo Party");
        partyInfo.add(": " + partyHost);
        splashInfo.put(PARTY, partyInfo);

        ArrayList<String> locationInfo = new ArrayList<>();
        locationInfo.add(LOCATION);
        locationInfo.add(": " + location);
        splashInfo.put(LOCATION, locationInfo);

        ArrayList<String> noteInfo = new ArrayList<>();
        noteInfo.add(NOTE);
        if (note == null || note.isEmpty()) {
            noteInfo.add(": No Note");
        } else {
            noteInfo.add(": ");
            noteInfo.addAll(note);
        }
        splashInfo.put(NOTE, noteInfo);

        ArrayList<String> timeInfo = new ArrayList<>();
        if (originalTime != -1) {
            timeInfo.add(valueOf(originalTime));
            originalTime = -1;
        } else {
            timeInfo.add(valueOf(System.currentTimeMillis()));
        }
        splashInfo.put("Time", timeInfo);
        ArrayList<String> splashId = new ArrayList<>();
        splashId.add(notif.splash);
        splashInfo.put("Splash", splashId);

        mapList.add(splashInfo);
        if (sendNotif) {
            PlayerInfo.setReadyToNotify(hub, notif.dungeonHub);
        }
    }

    // This is called onTickEvent in PlayerInfo when the player is not null
    public synchronized void notification(String hub, boolean dungeonHub) {
        if (!BingoBrewersConfig.splashNotificationsEnabled) return;
        if(!SplashHud.onBingo) return; // non-profile bingo splashes setting was here
        if(!SplashHud.inSkyblockorPTLobbyorLimbo && !BingoBrewersConfig.splashNotificationsOutsideSkyblock) return;
        if(!BingoBrewers.onHypixel) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (!dungeonHub) {
            if (hub.equalsIgnoreCase("Unknown Hub")) {
                hub = "Unknown Hub";
            } else {
                hub = "Hub " + hub;
            }
            TitleHud titleHud = new TitleHud("Splash in " + hub, BingoBrewersConfig.alertTextColor.getRGB(), 4000, false);
            setActiveHud(titleHud);
        } else {
            if (hub.equalsIgnoreCase("Unknown Hub")) {
                hub = "Unknown Dungeon Hub";
            } else {
                hub = "Dungeon Hub " + hub;
            }
            TitleHud titleHud = new TitleHud("Splash in " + hub, BingoBrewersConfig.alertTextColor.getRGB(), 4000, false);
            setActiveHud(titleHud);
        }

        player.playSound("bingobrewers:splash_notification", BingoBrewersConfig.splashNotificationVolume/100f, 1.0f);
    }

    @NotNull
    private static HashMap<String, ArrayList<String>> getExampleHud() {
        HashMap<String, ArrayList<String>> infoMap = new HashMap<>();
        ArrayList<String> hubInfo = new ArrayList<>();
        hubInfo.add("Hub");
        hubInfo.add(": 14");
        infoMap.put("Hub", hubInfo);

        ArrayList<String> splasherInfo = new ArrayList<>();
        splasherInfo.add("Splasher");
        splasherInfo.add(": indigo_polecat");
        infoMap.put("Splasher", splasherInfo);

        ArrayList<String> partyInfo = new ArrayList<>();
        partyInfo.add("Bingo Party");
        partyInfo.add(": /p join BingoParty");
        infoMap.put("Party", partyInfo);

        ArrayList<String> locationInfo = new ArrayList<>();
        locationInfo.add("Location");
        locationInfo.add(": Bea House");
        infoMap.put("Location", locationInfo);

        ArrayList<String> noteInfo = new ArrayList<>();
        noteInfo.add("Note");
        noteInfo.add(":");
        noteInfo.add("This is an example splash");
        infoMap.put("Note", noteInfo);

        ArrayList<String> timeInfo = new ArrayList<>();
        timeInfo.add("Time");
        timeInfo.add(String.valueOf(Long.MAX_VALUE));
        infoMap.put("Time", timeInfo);

        ArrayList<String> splashId = new ArrayList<>();
        splashId.add("1");
        infoMap.put("Splash", splashId);
        return infoMap;
    }
}
