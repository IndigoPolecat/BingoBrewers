package com.github.indigopolecat.bingobrewers.Hud;

import dev.deftu.omnicore.client.render.OmniMatrixStack;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.github.indigopolecat.bingobrewers.BingoBrewersConfig.textAlignmentCHHud;
import static com.github.indigopolecat.bingobrewers.BingoBrewersConfig.justifySeparation;

public class CrystalHollowsHud extends LegacyHud {
    float lastLineRenderedAtY = 0;
    int totalLines = 0;
    long renderCounter = 0;
    float totalHeight = 0;
    float longestWidth = 0;
    public static float TOP_BOTTOM_PADDING = 3;
    public static ConcurrentLinkedDeque<CrystalHollowsItemTotal> filteredItems = new ConcurrentLinkedDeque<>();

//    @Override
//    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
//        if (!BingoBrewersConfig.crystalHollowsWaypointsToggle) return;
//        renderCounter++;
//        ConcurrentLinkedDeque<CrystalHollowsItemTotal> items = new ConcurrentLinkedDeque<>();
//        if (example && filteredItems.isEmpty()) {
//            CrystalHollowsItemTotal item1 = new CrystalHollowsItemTotal();
//            item1.itemCount = "80-4800";
//            item1.countColor = 0x55FFFF;
//            item1.itemName = "Mithril Powder";
//            item1.itemColor = 0x00AA00;
//            items.add(item1);
//
//            CrystalHollowsItemTotal item2 = new CrystalHollowsItemTotal();
//            item2.itemCount = "2";
//            item2.itemName = "Robotron Reflector";
//            item2.countColor = 0xFFFFFF;
//            item2.itemColor = 0x5555FF;
//            items.add(item2);
//
//            CrystalHollowsItemTotal item3 = new CrystalHollowsItemTotal();
//            item3.itemCount = "1";
//            item3.itemName = "Prehistoric Egg";
//            item3.countColor = 0xFFFFFF;
//            item3.itemColor = 0xFFFFFF;
//            items.add(item3);
//
//            CrystalHollowsItemTotal item4 = new CrystalHollowsItemTotal();
//            item4.itemCount = "1";
//            item4.itemName = "Blue Goblin Egg";
//            item4.countColor = 0xFFFFFF;
//            item4.itemColor = 0x00AAAA;
//            items.add(item4);
//
//            CrystalHollowsItemTotal item5 = new CrystalHollowsItemTotal();
//            item5.itemCount = "1";
//            item5.itemName = "Flawless Sapphire Gemstone";
//            item5.countColor = 0xFFFFFF;
//            item5.itemColor = 0xAA00AA;
//            items.add(item5);
//
//        } else {
//            items = filteredItems;
//        }
//        renderCrystalHollowsHud(items, x, y, scale);
//
//        totalHeight = totalLines * 10 + 3;
//
//        // Reset at the end
//        lastLineRenderedAtY = y + 3;
//    }
//
//    @Override
//    protected float getWidth(float scale, boolean example) {
//        if (textAlignmentCHHud) {
//            return justifySeparation * scale;
//        }
//
//        if (scale == 0) {
//            scale = 1;
//        }
//        // the string wraps at 106
//        if (longestWidth > 300 * scale) {
//            longestWidth = 300;
//        }
//
//
//        return (longestWidth * scale) + 3;
//    }
//
//    @Override
//    protected float getHeight(float scale, boolean example) {
//        if (scale == 0) {
//            scale = 1;
//        }
//        return totalHeight * scale;
//    }

    public void renderCrystalHollowsHud(ConcurrentLinkedDeque<CrystalHollowsItemTotal> items, float x, float y, float scaleX, float scaleY, OmniMatrixStack omniMatrixStack) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int heightScaled = scaledResolution.getScaledHeight();

        longestWidth = 0;
        totalLines = 0;

        omniMatrixStack.scale(scaleX, scaleY, 1f);
        x = (x / scaleX);
        y = (y / scaleY);
        y = Math.min(y, heightScaled); // supposed to be heightScaled - getHeight()

        float heightToRenderAt = ((y + TOP_BOTTOM_PADDING) * scaleY); // advances each line drawn



        // set color of text
        int lineCount = 0;

        // loop through the hashmap of the splash
        for (CrystalHollowsItemTotal itemTotal : items) {

            String itemCount = itemTotal.itemCount;
            String itemName = itemTotal.itemName;

            // wrap width
            float maxWidth = 200 * scaleX;

            // Render item count
            fontRenderer.drawStringWithShadow(itemCount, (x), (heightToRenderAt), itemTotal.countColor);
            float nextStart = fontRenderer.getStringWidth(itemCount + " ");

            List<String> wrappedLines = fontRenderer.listFormattedStringToWidth(itemName, (int) maxWidth);

            for (int l = 0; l < wrappedLines.size(); l++) {
                String line = wrappedLines.get(l);
                // reset the offset if there is more than one line
                if (l == 1) nextStart = 0;

                if (textAlignmentCHHud == BingoBrewersConfig.TextAlignment.TABLE) {
                    maxWidth = justifySeparation * scaleX;
                    nextStart = (maxWidth / scaleX) - (fontRenderer.getStringWidth(line));
                } else if (fontRenderer.getStringWidth(line) + nextStart > longestWidth) {
                    longestWidth = fontRenderer.getStringWidth(line) + nextStart;
                }
                // render the string
                fontRenderer.drawStringWithShadow(line, (x + nextStart), (heightToRenderAt), itemTotal.itemColor);
                // mark the last y value we rendered a string at
                heightToRenderAt += 10;
                // increase the line count
                lineCount++;
                // stop rendering if we're off screen
                if (heightToRenderAt > heightScaled - 10) {
                    break;
                }
            }

        }
        totalLines += lineCount;
    }

    @Override
    public float getWidth() {
        return 100;
    }

    @Override
    public void setWidth(float v) {

    }

    @Override
    public float getHeight() {
        return 100;
    }

    @Override
    public void setHeight(float v) {

    }

    @Override
    public void render(@NotNull OmniMatrixStack omniMatrixStack, float x, float y, float scaleX, float scaleY) {
        if (!BingoBrewersConfig.crystalHollowsWaypointsToggle) return;

        boolean example = !isReal();
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

        } else {
            items = filteredItems;
        }
        renderCrystalHollowsHud(items, x, y, scaleX, scaleY, omniMatrixStack);

        totalHeight = totalLines * 10 + 3;
    }

    @Override
    public @NotNull String title() {
        return "";
    }

    @Override
    public @NotNull Category category() {
        return null;
    }

    @Override
    public boolean update() {
        return false;
    }
}
