package com.github.indigopolecat.bingobrewers.Hud;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.indigopolecat.bingobrewers.CHWaypoints.collectedItemCounts;
import static com.github.indigopolecat.bingobrewers.CHWaypoints.itemCounts;
import static com.github.indigopolecat.bingobrewers.ServerConnection.*;

public class CrystalHollowsHud extends Hud {
    float lastLineRenderedAtY = 0;
    int totalLines = 0;
    long renderCounter = 0;
    float totalHeight = 0;
    float longestWidth = 0;
    float fontSize = 0.2F;

    public CrystalHollowsHud() {
        BingoBrewersConfig.crystalHollowsWaypointsToggle = enabled;
    }

    @Dropdown(
            name = "Loot Amount",
            options = {"All", "Remaining", "Fraction"},
            category = "Crystal Hollows Waypoints",
            description = "How to calculate the total of a type of loot in a lobby."
    )
    public static int lootCount = 0;

    @DualOption(
            name = "Alignment",
            left = "Left",
            right = "Justify",
            category = "Crystal Hollows Waypoints",
            description = "The alignment of the HUD text"
    )
    public static boolean justifyAlignmentCHHud = false;

    @Slider(
            name = "Justify Separation",
            min = 150,
            max = 300,
            step = 5,
            category = "Crystal Hollows Waypoints",
            description = "How far the separation is for the justified HUD"
    )
    public static Integer justifySeparation = 175;

    public static void updateFractionItems() {
        for (CrystalHollowsItemTotal itemTotal : filteredFractionalItems) {
            CrystalHollowsItemTotal itemCollected = collectedItemCounts.get(itemTotal.itemName);
            if (itemCollected == null) continue;
            itemTotal.itemCount = itemCollected.itemCount + "/" + itemTotal.itemCount;
        }
    }

    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        renderCounter++;
        ConcurrentLinkedDeque<CrystalHollowsItemTotal> items = new ConcurrentLinkedDeque<>();
        if (example && filteredItems.isEmpty()) {
            CrystalHollowsItemTotal item1 = new CrystalHollowsItemTotal();
            item1.itemCount = "80-4800";
            item1.countColor = 0x55FFFF;
            item1.itemName = "Mithril Powder";
            item1.itemColor = 0x00AA00;
            items.add(item1);

            CrystalHollowsItemTotal item2 = new CrystalHollowsItemTotal();
            item2.itemCount = "2";
            item2.itemName = "Robotron Reflector";
            item2.countColor = 0xFFFFFF;
            item2.itemColor = 0x5555FF;
            items.add(item2);

            CrystalHollowsItemTotal item3 = new CrystalHollowsItemTotal();
            item3.itemCount = "1";
            item3.itemName = "Prehistoric Egg";
            item3.countColor = 0xFFFFFF;
            item3.itemColor = 0xFFFFFF;
            items.add(item3);

            CrystalHollowsItemTotal item4 = new CrystalHollowsItemTotal();
            item4.itemCount = "1";
            item4.itemName = "Blue Goblin Egg";
            item4.countColor = 0xFFFFFF;
            item4.itemColor = 0x00AAAA;
            items.add(item4);

            CrystalHollowsItemTotal item5 = new CrystalHollowsItemTotal();
            item5.itemCount = "1";
            item5.itemName = "Flawless Sapphire Gemstone";
            item5.countColor = 0xFFFFFF;
            item5.itemColor = 0xAA00AA;
            items.add(item5);

            if (lootCount == 2) {
                item1.itemCount += "/160-6000";
                item2.itemCount += "/3";
                item3.itemCount += "/1";
                item4.itemCount += "/1";
                item5.itemCount += "/1";
            } else if (lootCount == 1) {
                item3.itemCount = "0";
                item5.itemCount = "0";
                item2.itemCount = "1";
            }

        } else if (lootCount == 0) {
            items = filteredItems;
        } else if (lootCount == 1) {
            items = filteredRemainingItems;
        } else if (lootCount == 2) {
            items = filteredFractionalItems;
        }
        renderCrystalHollowsHud(items, x, y, scale);

        totalHeight = totalLines * 10 + 3;

        // Reset at the end
        lastLineRenderedAtY = y + 3;
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (justifyAlignmentCHHud) {
            return justifySeparation * scale;
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

    public void renderCrystalHollowsHud(ConcurrentLinkedDeque<CrystalHollowsItemTotal> items,  float x, float y, float scale) {
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

        // loop through the hashmap of the splash
        for (CrystalHollowsItemTotal itemTotal : items) {

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

                if (justifyAlignmentCHHud) {
                    maxWidth = justifySeparation * scale;
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
