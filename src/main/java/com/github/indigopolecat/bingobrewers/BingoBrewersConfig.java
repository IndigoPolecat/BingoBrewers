package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.annotations.HUD;

public class BingoBrewersConfig extends Config {
    public BingoBrewersConfig() {
        super(new Mod("Bingo Brewers", ModType.SKYBLOCK), "bingobrewers.json");
        initialize();
    }

    @Switch(
            name = "Splash Notifications",
            category = "Splash Notifications",
            description = "Enable or disable splash notifications"
    )
    public static boolean splashNotificationsEnabled = true;

    @Switch(
            name = "Show Splash Notifications in All Profiles",
            category = "Splash Notifications",
            description = "Whether to show splash notifications in all profiles or just bingo."
    )
    public static boolean splashNotificationsInBingo = true;

    @Color(
            name = "Splash Notification Color",
            category = "Splash Notifications",
            description = "Set the color of the splash notification title"
    )
    public static OneColor splashNotificationColor = new OneColor(139,175,224);

    @HUD(
            name = "Splash HUD",
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
