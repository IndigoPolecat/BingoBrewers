package com.github.indigopolecat.bingobrewers.tweaker;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

public class BingoBrewersTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses"); // always of type List<String>
        tweakClasses.add("net.hypixel.modapi.tweaker.HypixelModAPITweaker");
        tweakClasses.add("com.github.indigopolecat.bingobrewers.tweaker.ModLoadingTweaker");
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
    }

    @Override
    public String getLaunchTarget() {
        return null; // not called
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}