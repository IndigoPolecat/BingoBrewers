package com.github.indigopolecat.bingobrewers.tweaker;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

public class BingoBrewersTweaker implements ITweaker {

    private final ITweaker oneConfigTweaker = new cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker();
    private final ITweaker modAPITweaker = new net.hypixel.modapi.tweaker.HypixelModAPITweaker();
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        oneConfigTweaker.acceptOptions(args, gameDir, assetsDir, profile);
        modAPITweaker.acceptOptions(args, gameDir, assetsDir, profile);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        oneConfigTweaker.injectIntoClassLoader(classLoader);
        modAPITweaker.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return oneConfigTweaker.getLaunchTarget();
    }

    @Override
    public String[] getLaunchArguments() {
        String[] args1 = oneConfigTweaker.getLaunchArguments();
        String[] args2 = modAPITweaker.getLaunchArguments();
        String[] merged = new String[args1.length + args2.length];
        System.arraycopy(args1, 0, merged, 0, args1.length);
        System.arraycopy(args2, 0, merged, args1.length, args2.length);
        return merged;
    }
}
