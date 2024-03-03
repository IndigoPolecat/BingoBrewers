package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Info;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.data.OptionSize;

public class BingoBrewersConfig extends Config {
    public BingoBrewersConfig() {
        super(new Mod("Bingo Brewers", ModType.SKYBLOCK), "bingobrewers.json");
        initialize();
    }

    @Switch(
            name = "Splash Notifications",
            category = "Splash Notifications",
            description = "Enable or disable splash notifications",
            size = OptionSize.DUAL
    )
    public static boolean splashNotificationsEnabled = true;

    @Info(
            text = "Leeching splashes on high level profiles is not allowed!",
            type = InfoType.ERROR,
            category = "Splash Notifications",
            size = OptionSize.DUAL
    )
    public static boolean ignored;

    @Switch(
            name = "Show Splash Notifications on All Profiles",
            category = "Splash Notifications",
            description = "Whether to show splash notifications on all profiles or just bingo."
    )
    public static boolean splashNotificationsInBingo = true;

    @Switch(
            name = "Show Splash Notifications outside of Skyblock",
            category = "Splash Notifications",
            description = "Whether to show splash notifications outside of skyblock AND the prototype lobby."
    )
    public static boolean splashNotificationsOutsideSkyblock = true;


    @HUD(
            name = "Splash Notification HUD",
            category = "Splash Notifications"
    )
    public HudRendering hud = new HudRendering();

    @Slider(
            name = "Notification Volume (%)",
            category = "Splash Notifications",
            description = "Set the volume of the splash notification",
            min = 0f, max = 200f
    )
    public static float splashNotificationVolume = 100f;

    @Switch(
            name = "Show Coins/Bingo Point",
            category = "Misc",
            description = "Show coins per Bingo Point in the Bingo Shop."
    )
    public static boolean showCoinsPerBingoPoint = true;


}
