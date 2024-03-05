package com.github.indigopolecat.bingobrewers.gui;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.TitleHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class UpdateScreen extends GuiScreen {
    private GuiButton updateNowButton;
    private GuiButton updateLaterButton;

    public String changelog = BingoBrewers.autoUpdater.getChangelog();

    @Override
    public void initGui() {
        buttonList.clear();
        updateNowButton = new GuiButton(0, width / 2 - 100, height - 50, "Update and Restart");
        updateLaterButton = new GuiButton(0, width / 2 - 100, height - 25, "Update on Next Launch");
        buttonList.add(updateNowButton);
        buttonList.add(updateLaterButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == updateNowButton) {
            // Do something when myButton is pressed
            BingoBrewers.autoUpdater.checkUpdate().thenAccept(updateAvailable -> {
                if(updateAvailable) {
                    BingoBrewers.autoUpdater.update();
                    Minecraft.getMinecraft().shutdownMinecraftApplet();
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bingo Brewers is up to date!"));
                }
            });
        } else if(button == updateLaterButton) {
            BingoBrewers.autoUpdater.checkUpdate().thenAccept(updateAvailable -> {
                if(updateAvailable) {
                    BingoBrewers.autoUpdater.update();
                    BingoBrewers.activeTitle = new TitleHud("Update will be installed on game close.", BingoBrewersConfig.alertTextColor.getRGB(), 4000);
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bingo Brewers is up to date!"));
                }
            });
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        String text = EnumChatFormatting.GOLD + "" +  EnumChatFormatting.BOLD  + EnumChatFormatting.OBFUSCATED + "KK" + EnumChatFormatting.RESET + EnumChatFormatting.GOLD + "" +  EnumChatFormatting.BOLD + "A new version of Bingo Brewers is available! " + EnumChatFormatting.OBFUSCATED + "KK";
        int textWidth = fontRendererObj.getStringWidth(text);

            // Convert markdown to Minecraft formatting codes
            changelog = changelog.replaceAll("###", "§l"); // Bold
            changelog = changelog.replaceAll("\r", ""); // Remove carriage returns

            // Split by new lines
            String[] lines = changelog.split("\n");
            fontRendererObj.drawString(text, width / 2 - textWidth / 2, 10, 0xFFFFFF);

            // Draw each line separately
            for (int i = 0; i < lines.length; i++) {
                fontRendererObj.drawString(EnumChatFormatting.RESET + lines[i], 10, 10 + (i+1) * 10, 0xFFFFFF);
            }
    }
}
