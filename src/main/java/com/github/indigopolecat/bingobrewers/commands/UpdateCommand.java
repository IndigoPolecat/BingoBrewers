package com.github.indigopolecat.bingobrewers.commands;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

public class UpdateCommand  extends CommandBase {
    @Override
    public String getCommandName() {
        return "bingobrewersupdate";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Checking for updates..."));
        BingoBrewers.autoUpdater.checkUpdate().thenAccept(updateAvailable -> {
            if(updateAvailable) {
                BingoBrewers.autoUpdater.update();
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bingo Brewers is up to date!"));
            }
        });
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;


    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("bbupdate");
    }
}

