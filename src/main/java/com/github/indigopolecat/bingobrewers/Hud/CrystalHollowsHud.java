package com.github.indigopolecat.bingobrewers.Hud;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CrystalHollowsHud extends Hud {
    float lastLineRenderedAtY = 0;
    int totalLines = 0;
    long renderCounter = 0;
    // For some reason, latestSplash becomes bloated because it is stored in a config class, don't know how to fix but it's not a massive issue immediately, though it will inflate file size.
    public static ArrayList<Long> latestSplash = new ArrayList<>(2);
    float totalHeight = 0;
    float longestWidth = 0;
    float fontSize = 0.2F;
    public static LinkedHashMap<String, CrystalHollowsItemTotal> filteredItems = new LinkedHashMap<>();



    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        renderCounter++;
        LinkedHashMap<String, CrystalHollowsItemTotal> items = new LinkedHashMap<>();
        if (example) {
            CrystalHollowsItemTotal item1 = new CrystalHollowsItemTotal();
            item1.itemCount = "80-4800";
            item1.countColor = 0x55FFFF;
            item1.itemName = "Mithril Powder";
            item1.itemColor = 0x00AA00;
            items.put(item1.itemName, item1);

            CrystalHollowsItemTotal item2 = new CrystalHollowsItemTotal();
            item2.itemCount = "2";
            item2.itemName = "Robotron Reflector";
            item2.countColor = 0xFFFFFF;
            item2.itemColor = 0x5555FF;
            items.put(item2.itemName, item2);

            CrystalHollowsItemTotal item3 = new CrystalHollowsItemTotal();
            item3.itemCount = "1";
            item3.itemName = "Prehistoric Egg";
            item3.countColor = 0xFFFFFF;
            item3.itemColor = 0xFFFFFF;
            items.put(item3.itemName, item3);

            CrystalHollowsItemTotal item4 = new CrystalHollowsItemTotal();
            item4.itemCount = "1";
            item4.itemName = "Blue Goblin Egg";
            item4.countColor = 0xFFFFFF;
            item4.itemColor = 0x00AAAA;
            items.put(item4.itemName, item4);

            CrystalHollowsItemTotal item5 = new CrystalHollowsItemTotal();
            item5.itemCount = "1";
            item5.itemName = "Flawless Sapphire Gemstone";
            item5.countColor = 0xFFFFFF;
            item5.itemColor = 0xAA00AA;
            items.put(item5.itemName, item5);

        } else {
            items = filteredItems;
        }
        renderCrystalHollowsHud(items, x, y, scale);

        totalHeight = totalLines * 10 + 3;

        // Reset at the end
        lastLineRenderedAtY = y + 3;
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (BingoBrewersConfig.justifyAlignmentCHHud) {
            return BingoBrewersConfig.justifySeparation * scale;
        }

        if (scale == 0) {
            scale = 1;
        }
        // the string wraps at 106
        if (longestWidth > 300 * scale) {
            longestWidth = 300;
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

    public void renderCrystalHollowsHud(LinkedHashMap<String, CrystalHollowsItemTotal> items, float x, float y, float scale) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int heightScaled = scaledResolution.getScaledHeight();

        fontSize = scale;
        longestWidth = 0;
        totalLines = 0;

        // set font size
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        x = (x / scale);
        y = (y / scale );
        y = Math.min(y, heightScaled - getHeight(scale, false));
        lastLineRenderedAtY /= scale;



        // set color of text
        int lineCount = 0;
        Set<String> keys = items.keySet();
        // loop through the hashmap of the splash
        for (String item : keys) {
            CrystalHollowsItemTotal itemTotal = items.get(item);
            String itemCount = itemTotal.itemCount;
            String itemName = itemTotal.itemName;

            // wrap width
            float maxWidth = 200 * scale;

            // Render item count
            fontRenderer.drawStringWithShadow(itemCount, (x), (lastLineRenderedAtY), itemTotal.countColor);
            float nextStart = fontRenderer.getStringWidth(itemCount + " ");

            List<String> wrappedLines = fontRenderer.listFormattedStringToWidth(itemName, (int) maxWidth);

            for (int l = 0; l < wrappedLines.size(); l++) {
                String line = wrappedLines.get(l);
                // reset the offset if there is more than one line
                if (l == 1) nextStart = 0;

                if (BingoBrewersConfig.justifyAlignmentCHHud) {
                    maxWidth = BingoBrewersConfig.justifySeparation * scale;
                    nextStart = (maxWidth / scale) - (fontRenderer.getStringWidth(line));
                } else if (fontRenderer.getStringWidth(line) + nextStart > longestWidth) {
                    longestWidth = fontRenderer.getStringWidth(line) + nextStart;
                }
                // render the string
                fontRenderer.drawStringWithShadow(line, (x + nextStart), (lastLineRenderedAtY), itemTotal.itemColor);
                // mark the last y value we rendered a string at
                lastLineRenderedAtY += 10;
                // increase the line count
                lineCount++;
                // stop rendering if we're off screen
                if (lastLineRenderedAtY > heightScaled - 10) {
                    break;
                }
            }

        }
        totalLines += lineCount;
        // add a buffer between parts
        lastLineRenderedAtY += 10;
        GL11.glPopMatrix();
    }
}
