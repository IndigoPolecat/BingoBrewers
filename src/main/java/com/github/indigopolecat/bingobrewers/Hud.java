package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;

import java.util.ArrayList;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;

import java.awt.Color;
import java.lang.Math;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Hud extends BasicHud {
    float lastLineRenderedAtY = -3;
    int lineCount = 0;
    boolean listTooLong = false;
    long renderCounter = 0;
    ArrayList<Long> latestSplash = new ArrayList<>(2);
    float totalHeight = 0;



    public Hud() {
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
                    ServerConnection.mapList.remove(infoMap);
                    continue;
                }
                // jank
                if (latestSplash.size() < 2) {
                    latestSplash.add(time);
                    continue;
                }

                long old;
                for (int j = 0; j < latestSplash.size(); j++) {
                    if (time > latestSplash.get(j)) {
                        old = latestSplash.get(j);
                        latestSplash.set(j, time);

                        for (int k = 0; k < latestSplash.size(); k++) {
                            if (latestSplash.get(k) < old) {
                                latestSplash.set(k, old);
                                break;
                            }
                        }
                        break;
                    }
                }

                if (!latestSplash.contains(time)) {
                    infoPanel.remove(infoMap);
                }
            }
        }

        // Render each item in the list
        for (int i = 0; i < infoPanel.size(); i++) {
            HashMap<String, ArrayList<String>> map = infoPanel.get(i);

            // White
            Color colorText = new Color(255, 255, 255);
            // Yellow
            Color colorPrefix = new Color(255, 255, 85);
            lineCount = 0;
            listTooLong = false;
            for (int k = 0; k < ServerConnection.keyOrder.size(); k++) {
                if (listTooLong) {
                    break;
                }
                String key = ServerConnection.keyOrder.get(k);
                ArrayList<String> splashInfo = map.get(key);
                if (!splashInfo.isEmpty()) {
                    // Consider moving this above the loops (unsure how this impacts performance currently)
                    instance.setupAndDraw(true, vg -> {
                        int fontSize = 6;
                        int width = 106;
                        String prefix = splashInfo.get(0);
                        instance.drawWrappedString(vg, prefix, x, y + (lastLineRenderedAtY), width, colorPrefix.getRGB(), fontSize, 1, Fonts.MINECRAFT_BOLD);
                        // When to start the line after the prefix
                        float nextStart = instance.getWrappedStringWidth(vg, prefix, width, fontSize, Fonts.MINECRAFT_BOLD) + 0.25F;
                        float height = instance.getWrappedStringHeight(vg, prefix, width, fontSize, 1, Fonts.MINECRAFT_BOLD);
                        height -= 5;
                        for (int j = 1; j < splashInfo.size(); j++) {
                            // Reset the offset if there is more than one line
                            if (j == 2) nextStart = 0;
                            String info = splashInfo.get(j);
                            if (lineCount >= 10) {
                                info = "...";
                                instance.drawWrappedString(vg, info, x + nextStart, y + (lastLineRenderedAtY), width, colorText.getRGB(), fontSize, 1, Fonts.MINECRAFT_REGULAR);
                                lineCount += 1;
                                lastLineRenderedAtY += 5;
                                listTooLong = true;
                                break;
                            }

                            float heightText = instance.getWrappedStringHeight(vg, info, width, fontSize, 1, Fonts.MINECRAFT_REGULAR);
                            if (heightText + (lineCount * 6) > 50 ) continue;
                            instance.drawWrappedString(vg, info, x + nextStart, y + (lastLineRenderedAtY), width, colorText.getRGB(), fontSize, 1, Fonts.MINECRAFT_REGULAR);
                            lastLineRenderedAtY += heightText;
                            lineCount += (int) ((height + heightText)/6);

                        }

                    });
                }
            }
            // buffer between parts
            lastLineRenderedAtY += 5;
        }

        // Reset at the end
        lastLineRenderedAtY = y;
        // Set height
        totalHeight = lastLineRenderedAtY - y;
        // Dwarven Mines Event

    }

    @Override
    protected float getWidth(float scale, boolean example) {
        return 106;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return totalHeight;

    }
}
