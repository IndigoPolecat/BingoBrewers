package com.github.indigopolecat.bingobrewers.commands;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import org.polyfrost.oneconfig.utils.v1.dsl.ScreensKt;

import java.util.Arrays;
import java.util.List;

public class ConfigCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "bingobrewers";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        //BingoBrewers.config.openGui();
        ScreensKt.openUI(BingoBrewers.config);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;


    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("bingobrewers", "bb");
    }
}
