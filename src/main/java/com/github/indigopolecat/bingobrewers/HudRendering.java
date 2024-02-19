package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import java.util.ArrayList;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import java.awt.Color;
import java.util.HashMap;

public class HudRendering extends Hud {
    float lastLineRenderedAtY = 0;
    int lineCount = 0;
    boolean listTooLong = false;
    long renderCounter = 0;
    ArrayList<Long> latestSplash = new ArrayList<>(2);
    float totalHeight = 0;
    float longestWidth = 0;
    float fontSize = 6;




    public HudRendering() {
        super(true);
        EventManager.INSTANCE.register(this);
    }


    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        renderCounter++;
        NanoVGHelper instance = NanoVGHelper.INSTANCE;
        // The list containing each hashmap of info to be displayed
        ArrayList<HashMap<String, ArrayList<String>>> infoPanel = ServerConnection.mapList;
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
                    ServerConnection.hubList.remove(hubNumber);
                    ServerConnection.hubList.remove("DH" + hubNumber);
                    ServerConnection.mapList.remove(infoMap);
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
                    } else if(latestSplash.get(i) > newSplash) {
                        newSplash = latestSplash.get(i);
                    }

                }

                if (!latestSplash.contains(newerSplash) && !latestSplash.contains(newSplash)) {
                    infoPanel.remove(infoMap);
                }
            }
        }

        x+=1;
        // Render each item in the list
        renderSplashHud(infoPanel, instance, x, y, scale);

        // Set height of background
        totalHeight = lastLineRenderedAtY;

        // Reset at the end
        lastLineRenderedAtY = y + fontSize/2;

        // Dwarven Mines Event

    }

    private void renderSplashHud(ArrayList<HashMap<String, ArrayList<String>>> infoPanel, NanoVGHelper instance, float x, float y, float scale) {
        for (HashMap<String, ArrayList<String>> map : infoPanel) {
            // White
            Color colorText = new Color(255, 255, 255);
            // Yellow
            Color colorPrefix = new Color(255, 255, 85);
            lineCount = 0;
            listTooLong = false;
            instance.setupAndDraw(true, vg -> {
                for (int k = 0; k < ServerConnection.keyOrder.size(); k++) {
                    if (listTooLong) {
                        break;
                    }
                    String key = ServerConnection.keyOrder.get(k);
                    ArrayList<String> splashInfo = map.get(key);
                    if (splashInfo.isEmpty()) return;

                    fontSize = 6 * scale;
                    float width = 106 * scale;
                    String prefix = splashInfo.get(0);

                    instance.drawWrappedString(vg, prefix, x, y + (lastLineRenderedAtY), width, colorPrefix.getRGB(), fontSize, 1, Fonts.MINECRAFT_BOLD);
                    // When to start the line after the prefix
                    float nextStart = instance.getWrappedStringWidth(vg, prefix, width, fontSize, Fonts.MINECRAFT_BOLD) + 0.25F;
                    float height = instance.getWrappedStringHeight(vg, prefix, width, fontSize, 1, Fonts.MINECRAFT_BOLD);
                    height -= 5;
                    for (int j = 1; j < splashInfo.size(); j++) {

                        // Break the loop early if the text is too long
                        if (listTooLong) {
                            break;
                        }

                        // Reset the offset if there is more than one line
                        if (j == 2) nextStart = 0;
                        String info = splashInfo.get(j);
                        if (lineCount >= 14) {
                            info = info + "...";
                            listTooLong = true;
                        }

                        float heightText = instance.getWrappedStringHeight(vg, info, width, fontSize, 1, Fonts.MINECRAFT_REGULAR);
                        instance.drawWrappedString(vg, info, x + nextStart, y + (lastLineRenderedAtY), width, colorText.getRGB(), fontSize, 1, Fonts.MINECRAFT_REGULAR);
                        float textWidth = instance.getWrappedStringWidth(vg, info, width, fontSize, Fonts.MINECRAFT_REGULAR);

                        if ((textWidth + nextStart) > longestWidth) {
                            longestWidth = textWidth + nextStart;
                        }

                        lastLineRenderedAtY += heightText;
                        lineCount += (int) ((height + heightText) / fontSize);

                        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
                        int heightScaled = scaledResolution.getScaledHeight();
                        if (lastLineRenderedAtY > heightScaled - 2 * fontSize) {
                            listTooLong = true;
                            // stop rendering if we're off the screen
                            return;
                        }

                    }
                }

            });
            // buffer between parts
            lastLineRenderedAtY += fontSize;
        }

    }

    @Override
    protected float getWidth(float scale, boolean example) {
        // the string wraps at 106
        if (longestWidth > 106) {
            longestWidth = 106;
        }
        return longestWidth * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return totalHeight * scale;

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void renderGameOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        TitleHud activeTitle = bingoBrewers.activeTitle;
        if (activeTitle != null && activeTitle.displayTime > System.currentTimeMillis() - activeTitle.startTime) {
            activeTitle.drawTitle();
        }
    }
}
