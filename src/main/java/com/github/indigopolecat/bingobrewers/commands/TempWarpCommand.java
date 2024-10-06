package com.github.indigopolecat.bingobrewers.commands;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.ServerConnection;
import com.github.indigopolecat.bingobrewers.Warping;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.List;

public class TempWarpCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "warptoserver";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Warping.requestWarp(args[0]);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;


    }
}