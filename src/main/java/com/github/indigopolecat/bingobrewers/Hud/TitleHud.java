package com.github.indigopolecat.bingobrewers.Hud;

import dev.deftu.eventbus.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleHud {
    long startTime;
    String title;
    long displayTime;
    int color;
    boolean allowColorsFromText;

    public TitleHud(String title, int color, long displayTime, boolean allowColorsFromText) {
        this.startTime = System.currentTimeMillis();
        this.title = title;
        this.displayTime = displayTime;
        this.color = color;
        this.allowColorsFromText = allowColorsFromText;
    }

    public TitleHud(TitleHud hud) {
        this.startTime = System.currentTimeMillis();
        this.title = hud.title;
        this.displayTime = hud.displayTime;
        this.color = hud.color;
        this.allowColorsFromText = hud.allowColorsFromText;
    }

    public void drawTitle() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        FontRenderer fontRenderer = mc.fontRendererObj;
        float scaleFactor = 3.0f; // You can adjust this value to increase/decrease text size

        if (allowColorsFromText) {

            Pattern colorPattern = Pattern.compile("(&(\\S))");
            String[] textArray = title.split("(&(\\S))");
            ArrayList<String> textList = new ArrayList<>(Arrays.asList(textArray));
            StringBuilder totaltext = new StringBuilder();
            Iterator<String> iterator = textList.iterator();
            boolean first = true;
            while (iterator.hasNext()) {
                String text = iterator.next();
                if (text.isEmpty() && first) {
                    iterator.remove(); // This will remove the current element safely
                }
                totaltext.append(text);
                first = false;
            }
            Matcher colorMatcher = colorPattern.matcher(title);
            float x = (width - (scaleFactor * fontRenderer.getStringWidth(totaltext.toString()))) / (scaleFactor * 2);

            int colorText = this.color;
            for (String text : textList) {
                try {
                    if (colorMatcher.find()) {
                    colorText = fontRenderer.getColorCode(colorMatcher.group(2).charAt(0));
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {}

                GL11.glPushMatrix(); //Start new matrix
                GL11.glScalef(scaleFactor, scaleFactor, scaleFactor);
                fontRenderer.drawStringWithShadow(text, x, (height / (scaleFactor * 2)) - 5, colorText);
                GL11.glPopMatrix();
                x += fontRenderer.getStringWidth(text);
            }
        } else {
            GL11.glPushMatrix(); //Start new matrix
            GL11.glScalef(scaleFactor, scaleFactor, scaleFactor);
            fontRenderer.drawStringWithShadow(title, (width - (scaleFactor * fontRenderer.getStringWidth(title))) / (scaleFactor * 2), (height / (scaleFactor * 2)) - 5, color);
            GL11.glPopMatrix();
        }

    }

    @SubscribeEvent
    public void onRender(EntityViewRenderEvent event) {

    }

}
