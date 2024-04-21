package com.github.indigopolecat.bingobrewers.Hud;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
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
    boolean listTooLong = false;
    long renderCounter = 0;
    // For some reason, latestSplash becomes bloated because it is stored in a config class, don't know how to fix but it's not a massive issue immediately, though it will inflate file size.
    public static ArrayList<Long> latestSplash = new ArrayList<>(2);
    float totalHeight = 0;
    float longestWidth = 0;
    float fontSize = 0.2F;
    public static LinkedHashMap<String, CrystalHollowsItemTotal> filteredItems = new LinkedHashMap<>();


    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        renderCrystalHollowsHud(x, y, scale);
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

    public void renderCrystalHollowsHud(float x, float y, float scale) {
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
        y = (y / fontSize / (heightScaled + 2));
        y = Math.min(y, heightScaled - getHeight(scale, false));



        // set color of text
        int lineCount = 0;
        listTooLong = false;
        Set<String> keys = filteredItems.keySet();
        if (keys.isEmpty()) return;
        // loop through the hashmap of the splash
        for (String item : keys) {
            CrystalHollowsItemTotal itemTotal = filteredItems.get(item);
            String itemCount = itemTotal.itemCount;
            String itemName = itemTotal.itemName;
            Color itemColor = new Color(itemTotal.itemColor);
            Color countColor = new Color(itemTotal.countColor);

            // wrap width
            float maxWidth = 200 * scale;

            // Render item count
            float nextStart = fontRenderer.drawStringWithShadow(itemCount, (x), (y + lastLineRenderedAtY), countColor.getRGB()) - x;

            List<String> wrappedLines = fontRenderer.listFormattedStringToWidth(itemName, (int) maxWidth);

            for (int l = 0; l < wrappedLines.size(); l++) {
                String line = wrappedLines.get(l);

                if (fontRenderer.getStringWidth(line) + nextStart > longestWidth) {
                    longestWidth = fontRenderer.getStringWidth(line) + nextStart;
                }
                // reset the offset if there is more than one line
                if (l == 1) nextStart = 0;

                // render the string
                fontRenderer.drawStringWithShadow(line, (x + nextStart), (y + lastLineRenderedAtY), itemColor.getRGB());
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
        totalLines += lineCount + 1;
        // add a buffer between parts
        lastLineRenderedAtY += 10;
        GL11.glPopMatrix();
    }
}

