package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import net.minecraft.client.Minecraft;

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
            name = "Show Splash Notifications on Non-Bingo Profiles",
            category = "Splash Notifications",
            description = "Whether to show splash notifications regardless of your last active profile."
    )
    public static boolean splashNotificationsInBingo = true;

    @Switch(
            name = "Show Splash Notifications outside of Skyblock",
            category = "Splash Notifications",
            description = "Whether to show splash notifications outside of Skyblock AND the Prototype Lobby."
    )
    public static boolean splashNotificationsOutsideSkyblock = true;


    @HUD(
            name = "Splash Notification HUD",
            category = "Splash Notifications"
    )
    public HudRendering hud = new HudRendering();

    @Slider(
            name = "Notification Volume",
            category = "Splash Notifications",
            description = "Set the volume of the splash notification",
            min = 0f, max = 200f
    )
    public static float splashNotificationVolume = 100f;

    @Color(
            name = "Alert Text Color",
            category = "Splash Notifications",
            description = "Set the color of the alert text (i.e. \"Splash in Hub 14\")"
    )
    public static OneColor alertTextColor = new OneColor(0xFF8BAFE0);

    @Switch(
            name = "Show Coins/Bingo Point",
            category = "Misc",
            description = "Show coins per Bingo Point in the Bingo Shop."
    )
    public static boolean showCoinsPerBingoPoint = true;

    @Dropdown(
            name = "Auto Updater Versions",
            category = "Misc",
            description = "Choose which updates should the auto-updater look for",
            options = {"Stable", "Beta", "None"}
    )
    public static int autoUpdaterType = 0;

    @Switch(
            name = "Auto Download",
            category = "Misc",
            description = "Auto download updates when available. Requires restart."
    )
    public static boolean autoDownload = false;

    @Button(
            name = "Check for Updates",
            category = "Misc",
            description = "Check for updates",
            text = "Click"
    )
    public void checkForUpdates() {
        Minecraft.getMinecraft().displayGuiScreen(new UpdateScreen());
    }

    @Info(
            text = "Running version " + BingoBrewers.version,
            type = InfoType.INFO,
            category = "Misc",
            size = OptionSize.DUAL
    )
    public static boolean ignoredL;


}
