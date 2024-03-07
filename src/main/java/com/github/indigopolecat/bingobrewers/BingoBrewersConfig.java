package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
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

    @Switch(
            name = "Display Missing Bingo Points",
            category = "Misc",
            description = "Display the amount of missing Bingo Points to buy the item."
    )
    public static boolean displayMissingBingoPoints = true;

    @Switch(
            name = "Display Missing Bingoes",
            category = "Misc",
            description = "Display how many Bingoes are required to buy the item."
    )
    public static boolean displayMissingBingoes = true;
}
