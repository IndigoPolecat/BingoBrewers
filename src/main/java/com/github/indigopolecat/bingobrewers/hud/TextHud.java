package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.util.Log;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class TextHud implements Hud {
    protected String[] text;
    private final int defaultColor;
    private float scale = 1;
    protected int offsetX = 5;
    protected int offsetY = 8;
    private static boolean notifiedScale = false;
    
    public TextHud(int defaultColor, float scale, String text) {
        this(defaultColor, text);
        setScale(scale);
    }
    
    public TextHud(int defaultColor, String text) {
        this.defaultColor = defaultColor;
        this.text = text.split("\n");
    }
    
    public TextHud(int defaultColor, float scale, String... text) {
        this(defaultColor, text);
        setScale(scale);
    }
    
    public TextHud(int defaultColor, String... text) {
        this.defaultColor = defaultColor;
        this.text = text;
    }
    
    protected TextHud(int defaultColor) {
        this.defaultColor = defaultColor;
        this.text = new String[] {""};
    }
    
    public void setScale(float scale) {
        if(scale < 0.1 || scale > 30) throw new IllegalArgumentException("scale is <0.1 or >30");
        this.scale = scale;
    }
    
    @Override
    public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        if(isExpired()) {
            Log.warn("Tried to render expired TextHud", new Exception());
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || mc.player == null) return;
        
        // start with a fixed offset from the border
        int x = offsetX;
        int y = offsetY;
        
        Font font = mc.font;
        
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale);

        BingoBrewersConfig config = BingoBrewersConfig.getConfig();
        final int maxLines = config.maxLines;
        final int maxLineWidth = (int) (config.maxPixels * scale);


        try {
            // this needs to be updated when the setting is updated
            this.setScale(config.splashConfig.scale / 100f);
        } catch (IllegalArgumentException e) {
            if(notifiedScale) return;
            notifiedScale = true;
            Log.warn("Config.hud.scale is set to an invalid value: " + config.splashConfig.scale + "(scaled: " + config.splashConfig.scale / 100f + ")");
            Log.info("Invalid scale", e);
        }


        // Prepare data, store as FormattedText so it remains mathematically editable
        List<FormattedText> wrappedLines = new ArrayList<>();
        for (String line : text) {
            Component componentLine = Component.literal(line);
            // Use the underlying StringSplitter directly instead of font.split() to get a FormattedText output
            wrappedLines.addAll(font.getSplitter().splitLines(componentLine, maxLineWidth, Style.EMPTY));
        }

        int linesToRender = Math.min(wrappedLines.size(), maxLines);

        // 2. Execute Rendering
        for (int i = 0; i < linesToRender; i++) {
            FormattedText currentLineText = wrappedLines.get(i);
            FormattedCharSequence textToRender;

            // 3. Check if we are on the final allowed line AND there is text overflowing
            if (i == linesToRender - 1 && wrappedLines.size() > maxLines) {

                // calculate elipsis width
                Component ellipsis = Component.literal("...");
                int ellipsisWidth = font.width(ellipsis);

                // Calculate exact remaining pixel space
                int availableWidth = Math.max(0, maxLineWidth - ellipsisWidth);

                // Truncate the line to the width left after the ellipsis
                FormattedText truncatedText = font.substrByWidth(currentLineText, availableWidth);

                // Composite the two logical text nodes together
                textToRender = Language.getInstance().getVisualOrder(FormattedText.composite(truncatedText, ellipsis));

            } else {
                // If it's a normal line, just bake it directly into a visual sequence
                textToRender = Language.getInstance().getVisualOrder(wrappedLines.get(i));
            }

            // Draw line
            graphics.drawString(font, textToRender, (int)(x / scale), (int)(y / scale), defaultColor, false);
            y += (int)((font.lineHeight + 1) * scale);
        }
        graphics.pose().popMatrix();
    }
    
    @Override
    public boolean isExpired() {
        return false;
    }
}
