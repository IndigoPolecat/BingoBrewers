package com.github.indigopolecat.bingobrewers.hud;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import lombok.*;

@AllArgsConstructor
public class TitleHud implements Hud, TimedHud {
    @Getter private final long startTime = System.currentTimeMillis();
    @Getter private final long displayTime;
    @Getter private final String title;
    public int color;
    
    public TitleHud(TitleHud hud) {
        displayTime = hud.displayTime;
        title = hud.title;
        color = hud.color;
    }
    
    public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        if((color & 0xFF000000) == 0) color |= 0xFF000000;
        
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || mc.player == null) return;
        
        Window window = mc.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        
        Font font = mc.font;
        
        graphics.drawString(font, title, (width - font.width(title)) / 2, (height / 2) - font.lineHeight - 5, color, true);
    }
}
