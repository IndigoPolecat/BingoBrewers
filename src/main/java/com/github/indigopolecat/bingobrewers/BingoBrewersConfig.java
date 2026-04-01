package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.kryo.KryoNetwork;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.world.InteractionResult;

import static com.github.indigopolecat.bingobrewers.CHWaypoints.filteredWaypoints;
import static com.github.indigopolecat.bingobrewers.CHWaypoints.itemCounts;
import static com.github.indigopolecat.bingobrewers.ServerConnection.*;

@Config(name = "bingobrewers")
public class BingoBrewersConfig implements ConfigData {
    public BingoBrewersConfig() {
    }

    public static InteractionResult onSave(ConfigHolder<BingoBrewersConfig> bingoBrewersConfigConfigHolder, BingoBrewersConfig bingoBrewersConfig) {
        // if there's a good way to figure out what changed, and only run those functions (or rather the wrapper functions ...Call()), that would be better
        filterPowder();
        filterGoblinEggs();
        filterRoughGemstones();
        filterJasperGemstones();
        filterRobotParts();
        filterPrehistoricEggs();
        filterPickonimbus();
        filterMisc();
        organizeWaypoints();
        // idk what failure would do, just need this to work and there are no errors here
        return InteractionResult.SUCCESS;
    }
    
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
        public int scale = 100; //This is scale*100, since autoconfig does not support sliders with floats/doubles
        
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
    
    @Comment(value = "Defaults to IndigoPolecat/BingoBrewers")
    @ConfigEntry.Category(value = "misc")
    public String updaterRepository = "IndigoPolecat/BingoBrewers";

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
            waypoints.clear();
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

    public static void robotPartsCall() {
        filterRobotParts();
        organizeWaypoints();
    }

    public static void filterRobotParts() {
        filteredItems.removeIf(item -> "FTX 3070".equals(item.itemName) || "Robotron Reflector".equals(item.itemName) || "Control Switch".equals(item.itemName) || "Synthetic Heart".equals(item.itemName) || "Superlite Motor".equals(item.itemName) || "Electron Transmitter".equals(item.itemName));

        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if ("FTX 3070".equals(item.name) || "Robotron Reflector".equals(item.name) || "Control Switch".equals(item.name) || "Synthetic Heart".equals(item.name) || "Superlite Motor".equals(item.name) || "Electron Transmitter".equals(item.name)) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().robotParts) {
            for (String item : itemCounts.keySet()) {
                if ("FTX 3070".equals(item) || "Robotron Reflector".equals(item) || "Control Switch".equals(item) || "Synthetic Heart".equals(item) || "Superlite Motor".equals(item) || "Electron Transmitter".equals(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }
            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("FTX 3070".equals(item.name) || "Robotron Reflector".equals(item.name) || "Control Switch".equals(item.name) || "Synthetic Heart".equals(item.name) || "Superlite Motor".equals(item.name) || "Electron Transmitter".equals(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());
    }

    public static void powderCall() {
        filterPowder();
        organizeWaypoints();
    }
    public static void filterPowder() {
        filteredItems.removeIf(entry -> entry.itemName.contains(" Powder"));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains(" Powder")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().powder == Powder.ALL) {
            for (String item : itemCounts.keySet()) {
                if (item.contains(" Powder")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains(" Powder")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else if (getConfig().powder == Powder.MORE_THAN_1200) {
            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (!item.count.contains("-")) continue;
                    if (item.name.contains(" Powder") && Integer.parseInt(item.count.split("-")[1]) > 1200) {
                        boolean summed = false;
                        for (CrystalHollowsItemTotal itemTotal : filteredItems) {
                            if (itemTotal.itemName.equals(item.name)) {
                                String itemCountExisting = itemTotal.itemCount;
                                filteredItems.remove(itemTotal);
                                filteredItems.add(CrystalHollowsItemTotal.sumPowder(itemCountExisting, item, itemTotal));
                                summed = true;
                            }
                        }
                        // if it doesn't exist
                        if (!summed) {
                            CrystalHollowsItemTotal crystalHollowsItemTotal = new CrystalHollowsItemTotal();
                            crystalHollowsItemTotal.itemCount = item.count;
                            crystalHollowsItemTotal.itemName = item.name;
                            crystalHollowsItemTotal.itemColor = item.itemColor;
                            crystalHollowsItemTotal.countColor = item.numberColor;
                            filteredItems.add(crystalHollowsItemTotal);
                        }
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }

    public static void prehistoricEggsCall() {
        filterPrehistoricEggs();
        organizeWaypoints();
    }
    public static void filterPrehistoricEggs() {
        filteredItems.removeIf(entry -> "Prehistoric Egg".equals(entry.itemName));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if ("Prehistoric Egg".equals(item.name)) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().prehistoricEggs) {
            for (String item : itemCounts.keySet()) {
                if ("Prehistoric Egg".equals(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Prehistoric Egg".equals(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }

    public static void pickonimbusCall() {
        filterPickonimbus();
        organizeWaypoints();
    }
    public static void filterPickonimbus() {
        filteredItems.removeIf(entry -> entry.itemName.contains("Pickonimbus"));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Pickonimbus")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().pickonimbus) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Pickonimbus")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Pickonimbus")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }

    public static void goblinEggsCall() {
        filterGoblinEggs();
        organizeWaypoints();
    }
    public static void filterGoblinEggs() {
        filteredItems.removeIf(entry -> entry.itemName.contains("Goblin Egg"));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Goblin Egg")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().goblinEggs == GoblinEggs.ALL) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Goblin Egg")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Goblin Egg")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else if (getConfig().goblinEggs == GoblinEggs.BLUE_ONLY) {
            for (String item : itemCounts.keySet()) {
                if ("Blue Goblin Egg".equals(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Blue Goblin Egg".equals(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }

    public static void roughCall() {
        filterRoughGemstones();
        organizeWaypoints();
    }
    public static void filterRoughGemstones() {
        filteredItems.removeIf(entry -> entry.itemName.contains("Gemstone") && !entry.itemName.contains("Powder"));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Gemstone") && !item.name.contains("Powder")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        filterJasperGemstones();
        if (getConfig().roughGemstones == Gemstones.ALL) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Gemstone") && !item.contains("Powder")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Gemstone") && !item.name.contains("Powder")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else if (getConfig().roughGemstones == Gemstones.FINE_ONLY) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Fine") || item.contains("Flawless")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Fine") || item.name.contains("Flawless")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }

    public static void jasperCall() {
        filterJasperGemstones();
        organizeWaypoints();
    }
    public static void filterJasperGemstones() {
        filteredItems.removeIf(entry -> entry.itemName.contains("Jasper"));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Jasper")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().jasperGemstones && getConfig().roughGemstones != Gemstones.ALL) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Jasper")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Jasper")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }

    public static void miscCall() {
        filterMisc();
        organizeWaypoints();
    }
    public static void filterMisc() {
        filteredItems.removeIf(entry -> "Wishing Compass".equals(entry.itemName) || "Treasurite".equals(entry.itemName) || "Jungle Heart".equals(entry.itemName) || "Oil Barrel".equals(entry.itemName) || "Sludge Juice".equals(entry.itemName) || "Ascension Rope".equals(entry.itemName) || "Yoggie".equals(entry.itemName) || newMiscCHItems.contains(entry.itemName));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if ("Wishing Compass".equals(item.name) || "Treasurite".equals(item.name) || "Jungle Heart".equals(item.name) || "Oil Barrel".equals(item.name) || "Sludge Juice".equals(item.name) || "Ascension Rope".equals(item.name) || "Yoggie".equals(item.name) || newMiscCHItems.contains(item.name)) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (getConfig().junk) {
            for (String item : itemCounts.keySet()) {
                if ("Wishing Compass".equals(item) || "Treasurite".equals(item) || "Jungle Heart".equals(item) || "Oil Barrel".equals(item) || "Sludge Juice".equals(item) || "Ascension Rope".equals(item) || "Yoggie".equals(item) || newMiscCHItems.contains(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Wishing Compass".equals(item.name) || "Treasurite".equals(item.name) || "Jungle Heart".equals(item.name) || "Oil Barrel".equals(item.name) || "Sludge Juice".equals(item.name) || "Ascension Rope".equals(item.name) || "Yoggie".equals(item.name) || newMiscCHItems.contains(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!filteredWaypoints.contains(waypoint)) {
                            filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
}
