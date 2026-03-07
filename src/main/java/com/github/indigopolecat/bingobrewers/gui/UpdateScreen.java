package com.github.indigopolecat.bingobrewers.gui;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpdateScreen extends Screen {
    public String changelog = BingoBrewers.autoUpdater.getChangelog();
    
    public UpdateScreen() {
        super(Component.literal("Bingo Brewers Update"));
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);
        
        String gb = ChatFormatting.GOLD + "" + ChatFormatting.BOLD;
        String title = gb + ChatFormatting.OBFUSCATED + "KK" + ChatFormatting.RESET + gb + " A new version of Bingo Brewers is available! " + ChatFormatting.OBFUSCATED + "KK";
        int textWidth = font.width(title);
        
        //Remove carriage returns
        changelog = changelog.replaceAll("#+\\s+", "§l").replaceAll("\r", "");
        changelog = changelog.replaceAll("-\\s+", "• ").replaceAll("\\s+-\\s+", "○ ");
        
        // Split by new lines
        String[] lines = changelog.split("\n");
        
        guiGraphics.drawString(font, title, width / 2 - textWidth / 2, 10, 0xFFFFFFF, false);
        
        // Draw each line separately
        for (int n = 0; n < lines.length; n++) {
            guiGraphics.drawString(font, ChatFormatting.RESET + lines[n], 10, 10 + (n + 1) * 10, 0xFFFFFFF);
        }
    }
    
    @Override
    public void init() {
        Button updateNowButton = Button.builder(Component.literal("Update and Close Game"), b->{
            BingoBrewers.autoUpdater.update().thenRunAsync(()->Minecraft.getInstance().stop());
        }).pos(width / 2 - 100, height - 50).build();
        
        Button updateLaterButton = Button.builder(Component.literal("Update on Next Launch"), b->{
            BingoBrewers.autoUpdater.checkUpdate().thenAccept(updateAvailable->{
                if(updateAvailable) {
                    BingoBrewers.autoUpdater.update();
                    //BingoBrewers.activeTitle = new TitleHud("Bingo Brewers will update on game close.", 0x47EB62, 4000, false);//TODO: look at this
                } else {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Bingo Brewers is up to date!").withColor(0x00FF00), false);
                }
            });
            Minecraft.getInstance().setScreen(null);
        }).pos(width / 2 - 100, height - 25).build();
        
        addRenderableWidget(updateNowButton);
        addRenderableWidget(updateLaterButton);
    }
}
