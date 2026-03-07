package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.util.Log;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class TextHud implements Hud {
    private final String[] text;
    private final int defaultColor;
    private float scale = 1;
    protected int offsetX = 5;
    protected int offsetY = 8;
    
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
    
    public void setScale(float scale) {
        if(scale < 1 || scale > 15) throw new IllegalArgumentException("scale is <1 or too big");
        this.scale = scale;
    }
    
    @Override
    public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        if(expired()) {
            Log.warn("Tried to render expired TextHud", new Exception());
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || mc.player == null) return;
        
        // start with a fixed offset from the border
        int x = offsetX;
        int y = offsetY;
        
        Font font = mc.font;
        
        for (String line: text) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale);
            
            graphics.drawString(font, line, (int)(x / scale), (int)(y / scale), defaultColor, false);
            y += (int)(font.lineHeight + 2.1 * scale);
            
            graphics.pose().popMatrix();
        }
    }
    
    @Override
    public boolean expired() {
        return false;
    }
}
