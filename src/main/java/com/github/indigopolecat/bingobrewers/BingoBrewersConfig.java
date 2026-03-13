package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.awt.Color;

@Config(name = "bingobrewers")
public class BingoBrewersConfig implements ConfigData {
    public BingoBrewersConfig() { }
    
    public static BingoBrewersConfig getConfig() {
        return AutoConfig.getConfigHolder(BingoBrewersConfig.class).getConfig();
    }

    @Comment(value = "Enable or disable splash notifications")
    public boolean splashNotificationsEnabled = true;

    @Comment(value = "Whether to show splash notifications outside of Skyblock")
    public boolean splashNotificationsOutsideSkyblock = false;

    @Comment(value = "Include the splash hub's current player count in splash notifications")
    public boolean showPlayerCount = true;

    @Comment(value = "Include the splasher's IGN in splash notifications")
    public boolean showSplasher = true;

    @Comment(value = "Include the bingo party listed in the splash message in splash notifications")
    public boolean showParty = true;

    @Comment(value = "Include the location in splash notifications")
    public boolean showLocation = true;
    
    @Comment(value = "Show any extra information the splasher included in the splash notification")
    public boolean showNote = true;
    
    @Comment(value = "Maximum number of lines displayed in the splash hud and the CH hud")
    public int maxLines = 15;

    @Comment(value = "Maximum number of pixels (scaled) for a single line before it is wrapped to another")
    public int maxPixels = 200;
    
    @ConfigEntry.Gui.TransitiveObject()
    public SplashHudSettings splashConfig = new SplashHudSettings();
    public static class SplashHudSettings {
        public int x = 10;
        public int y = 10;
        
        @ConfigEntry.BoundedDiscrete(min = 30, max = 300)
        public int scale = 100; //This is scale*100, since autoconfig does not support floats/doubles
        
        @Comment(value = "In seconds")
        public int displayTime = 120;
        
        @Comment(value = "In seconds")
        public int alertDisplayTime = 4;
    }

    @Comment(value = "Set the volume of the splash notification") @ConfigEntry.BoundedDiscrete(max = 200)
    public int splashNotificationVolume = 100;

    @ConfigEntry.ColorPicker(allowAlpha = true) //apparently it does work with ints
    @Comment(value = "ARGB, the firs 2 characters are the transparency")
    public int alertTextColorHex = 0xFF8BAFE0;
    
    @Comment(value = "Toggle Crystal Hollows Waypoints")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public boolean crystalHollowsWaypointsToggle = true;

    @Comment(value= "How to display waypoints once you have opened the chest.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public WaypointFate waypointFate = WaypointFate.STRIKETHROUGH;
    public enum WaypointFate { STRIKETHROUGH, REMOVE, DO_NOTHING }

    @Comment(value = "Include Mithril and Gemstone powder.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public Powder powder = Powder.ALL;
    public enum Powder { ALL, MORE_THAN_1200, NONE}

    @Comment(value = "Include Goblin Eggs.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public GoblinEggs goblinEggs = GoblinEggs.ALL;
    public enum GoblinEggs { ALL, BLUE_ONLY, NONE}

    @Comment(value = "Include gemstones found in chests.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public Gemstones roughGemstones = Gemstones.FINE_ONLY;
    public enum Gemstones { ALL, FINE_ONLY, NONE}

    @Comment(value = "Separate toggle for all Jasper gemstones, they can reveal Fairy Grottos.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public boolean jasperGemstones = true;

    @Comment(value = "Include all 6 Robot Parts.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public boolean robotParts = true;

    @Comment(value = "Include Prehistoric Eggs.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public boolean prehistoricEggs = true;

    @Comment(value = "Include Pickonimbus 2000s.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public boolean pickonimbus = true;

    @Comment(value = "Wishing Compasses, Treasurite, Jungle Hearts, Oil Barrels, Sludge Juice, Ascension Ropes, Yoggies.")
    @ConfigEntry.Category(value = "Crystal Hollows Waypoints")
    public boolean junk = true;

    @Comment(value = "Show coins per Bingo Point in the Bingo Shop.")
    @ConfigEntry.Category(value = "misc")
    public boolean showCoinsPerBingoPoint = true;

    @Comment(value = "Choose which updates should the auto-updater look for")
    @ConfigEntry.Category(value = "misc")
    public AutoUpdater autoUpdaterType = AutoUpdater.STABLE;
    public enum AutoUpdater { STABLE, BETA, NONE }

    @Comment(value = "Auto download updates when available. Requires restart.")
    @ConfigEntry.Category(value = "misc")
    public boolean autoDownload = false;
    
    @Comment(value = "Running version " + BingoBrewers.version)
    @ConfigEntry.Category(value = "misc")
    public boolean versionN;

    @Comment(value = "Display the amount of missing Bingo Points to buy the item.")
    @ConfigEntry.Category(value = "misc")
    public boolean displayMissingBingoPoints = true;

    @Comment(value = "Display how many Bingoes are required to buy the item.")
    @ConfigEntry.Category(value = "misc")
    public boolean displayMissingBingoes = true;

    @Comment(value = "Display a message if the Chicken Head cooldown is reset.")
    @ConfigEntry.Category(value = "misc")
    public boolean displayEggTimerReset = false;

    @Comment(value = "What to display when the Chicken cooldown is reset. (Use & for § in COLOR only codes)")
    @ConfigEntry.Category(value = "misc")
    public String eggTimerMessage = "&aCrouch";

    @Comment(value = "Play a sound if the Chicken Head cooldown is reset.")
    @ConfigEntry.Category(value = "misc")
    public boolean playEggTimerResetSound = false;
    
    public void subscribeToServer() {
        if (crystalHollowsWaypointsToggle && PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows")) {
            KryoNetwork.SubscribeToCHServer CHRequest = new KryoNetwork.SubscribeToCHServer();
            CHRequest.server = PlayerInfo.currentServer;
            CHRequest.day = PlayerInfo.day;
            ServerConnection.SubscribeToCHServer(CHRequest);
        } else {
            ServerConnection.waypoints.clear();
            CHWaypoints.itemCounts.clear();
            //filteredItems.clear(); //TODO (matita): this was for some reason part of the CH HUD
            CHWaypoints.filteredWaypoints.clear();
            if (!PlayerInfo.playerLocation.isEmpty()) {
                KryoNetwork.SubscribeToCHServer CHRequest = new KryoNetwork.SubscribeToCHServer();
                CHRequest.server = PlayerInfo.currentServer;
                CHRequest.day = PlayerInfo.day;
                CHRequest.unsubscribe = true;
                ServerConnection.SubscribeToCHServer(CHRequest);
                
                KryoNetwork.RegisterToWarpServer unregister = new KryoNetwork.RegisterToWarpServer();
                unregister.unregister = true;
                PlayerInfo.registeredToWarp = false;
                unregister.server = PlayerInfo.currentServer;
                ServerConnection.sendTCP(unregister);
            }
        }
        
        if (PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows")) {
            KryoNetwork.RegisterToWarpServer register = new KryoNetwork.RegisterToWarpServer();
            register.unregister = false;
            PlayerInfo.registeredToWarp = true;
            register.server = PlayerInfo.currentServer;
            ServerConnection.sendTCP(register);
        }
    }
}
