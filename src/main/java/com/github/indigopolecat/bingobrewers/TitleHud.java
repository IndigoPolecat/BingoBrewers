package com.github.indigopolecat.bingobrewers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class TitleHud {
    long startTime;
    String title;
    long displayTime;
    int color;

    public TitleHud(String title, int color, long displayTime) {
        this.startTime = System.currentTimeMillis();
        this.title = title;
        this.displayTime = displayTime;
        this.color = color;
    }

    public void drawTitle() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        FontRenderer fontRenderer = mc.fontRendererObj;
        float scaleFactor = 3.0f; // You can adjust this value to increase/decrease text size

        //fontRenderer.FONT_HEIGHT = (int) (fontRenderer.FONT_HEIGHT * scaleFactor);
        GL11.glPushMatrix(); //Start new matrix
        GL11.glScalef(scaleFactor, scaleFactor, scaleFactor);
        fontRenderer.drawStringWithShadow(title, (width - (scaleFactor * fontRenderer.getStringWidth(title))) / (scaleFactor * 2), (height / (scaleFactor * 2)) - 5, color);
        GL11.glPopMatrix();

    }

}
