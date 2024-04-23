package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.Map.Entry;

import static com.github.indigopolecat.bingobrewers.CHWaypoints.itemCounts;
import static com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud.filteredItems;

public class BingoBrewersConfig extends Config {
    public BingoBrewersConfig() {
        super(new Mod("Bingo Brewers", ModType.SKYBLOCK), "bingobrewers.json");
        List<String> crystalHollowsWaypoints = Arrays.asList("robotParts", "powder", "highPowder", "prehistoricEggs", "pickonimbus", "goblinEggs", "blueEggs", "roughGemstones", "fineGemstones", "jasperGemstones", "junk");
        for (String option : crystalHollowsWaypoints) {
            addDependency(option, "crystalHollowsWaypointsToggle");
        }
        addDependency("justifySeparation", "justifyAlignmentCHHud");
        addListener("robotParts", BingoBrewersConfig::filterRobotParts);
        addListener("powder", BingoBrewersConfig::filterPowder);
        addListener("prehistoricEggs", BingoBrewersConfig::filterPrehistoricEggs);
        addListener("pickonimbus", BingoBrewersConfig::filterPickonimbus);
        addListener("goblinEggs", BingoBrewersConfig::filterGoblinEggs);
        addListener("blueEggs", BingoBrewersConfig::filterBlueEggs);
        addListener("roughGemstones", BingoBrewersConfig::filterRoughGemstones);
        addListener("fineGemstones", BingoBrewersConfig::filterFineGemstones);
        addListener("jasperGemstones", BingoBrewersConfig::filterJasperGemstones);
        addListener("junk", BingoBrewersConfig::filterMisc);
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
    public SplashHud hud = new SplashHud();

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
            description = "Set the color of the alert text (e.g. \"Splash in Hub 14\")"
    )
    public static OneColor alertTextColor = new OneColor(0xFF8BAFE0);

    @Switch(
            name = "Crystal Hollows Waypoints",
            category = "Crystal Hollows Waypoints",
            description = "Toggle Crystal Hollows Waypoints"
    )
    public static boolean crystalHollowsWaypointsToggle = true;

    @HUD(
            name = "Crystal Hollows Loot HUD",
            category = "Crystal Hollows Waypoints"
    )
    public CrystalHollowsHud CHHud = new CrystalHollowsHud();

    @DualOption(
            name = "Alignment",
            left = "Left",
            right = "Justify",
            category = "Crystal Hollows Waypoints",
            description = "The alignment of the HUD text"
    )
    public static boolean justifyAlignmentCHHud = false;

    @Slider(
            name = "Justify Separation",
            min = 150,
            max = 300,
            step = 5,
            category = "Crystal Hollows Waypoints",
            description = "How far the separation is for the justified HUD"
    )
    public static Integer justifySeparation = 175;

    @Dropdown(
            name = "Waypoints After Opening",
            options = {"Strikethrough", "Remove", "Nothing"},
            category = "Crystal Hollows Waypoints",
            description = "How to display waypoints once you have opened the chest."
    )
    public static int waypointFate = 0;

    @Checkbox(
            name = "Robot Parts",
            category = "Crystal Hollows Waypoints",
            description = "Include all 6 Robot Parts.",
            size = OptionSize.DUAL
    )
    public static boolean robotParts = true;

    @Dropdown(
            name = "Powder",
            options = {"None", "Only 1200+ Powder", "All"},
            category = "Crystal Hollows Waypoints",
            description = "Include Mithril and Gemstone powder."
    )
    public static int powder = 2;


    @Checkbox(
            name = "Prehistoric Eggs",
            category = "Crystal Hollows Waypoints",
            description = "Include Prehistoric Eggs.",
            size = OptionSize.DUAL
    )
    public static boolean prehistoricEggs = true;

    @Checkbox(
            name = "Pickonimbus 2000",
            category = "Crystal Hollows Waypoints",
            description = "Include Pickonimbus 2000s.",
            size = OptionSize.DUAL
    )
    public static boolean pickonimbus = true;

    @Checkbox(
            name = "Goblin Eggs",
            category = "Crystal Hollows Waypoints",
            description = "Include all Goblin Eggs."
    )
    public static boolean goblinEggs = true;

    @Checkbox(
            name = "Blue Goblin Eggs",
            category = "Crystal Hollows Waypoints",
            description = "Separate toggle for only Blue Goblin Eggs."
    )
    public static boolean blueEggs = true;

    @Checkbox(
            name = "Rough/Flawed Gemstones",
            category = "Crystal Hollows Waypoints",
            description = "Include Rough and Flawed gemstones found in chests."
    )
    public static boolean roughGemstones = false;

    @Checkbox(
            name = "Fine/Flawless Gemstones",
            category = "Crystal Hollows Waypoints",
            description = "Include Fine and Flawless gemstones found in chests."
    )
    public static boolean fineGemstones = true;

    @Checkbox(
            name = "Jasper Gemstones",
            category = "Crystal Hollows Waypoints",
            description = "Separate toggle for all Jasper gemstones, they can reveal Fairy Grottos."
    )
    public static boolean jasperGemstones = true;

    @Checkbox(
            name = "Misc.",
            category = "Crystal Hollows Waypoints",
            description = "Wishing Compasses, Treasurite, Jungle Hearts, Oil Barrels, Sludge Juice, Ascension Ropes, Yoggies."
    )
    public static boolean junk = true;

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

    @Switch(
            name = "Chicken Head Reset Message",
            category = "Misc",
            description = "Display a message if the Chicken Head cooldown is reset."
    )
    public static boolean displayEggTimerReset = false;

    @Switch(
            name="Chicken Head Reset Sound",
            category = "Misc",
            description = "Play a sound if the Chicken Head cooldown is reset."
    )
    public static boolean playEggTimerResetSound = false;

    public static void filterRobotParts() {
        if (robotParts) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("FTX 3070".equals(item) || "Robotron Reflector".equals(item) || "Control Switch".equals(item) || "Synthetic Heart".equals(item) || "Superlite Motor".equals(item) || "Electron Transmitter".equals(item)) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("FTX 3070".equals(item.name) || "Robotron Reflector".equals(item.name) || "Control Switch".equals(item.name) || "Synthetic Heart".equals(item.name) || "Superlite Motor".equals(item.name) || "Electron Transmitter".equals(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            System.out.println("Disabling");
            for (String item : filteredItems.keySet()) {
                if ("FTX 3070".equals(item) || "Robotron Reflector".equals(item) || "Control Switch".equals(item) || "Synthetic Heart".equals(item) || "Superlite Motor".equals(item) || "Electron Transmitter".equals(item)) {
                    filteredItems.remove(item);
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("FTX 3070".equals(item.name) || "Robotron Reflector".equals(item.name) || "Control Switch".equals(item.name) || "Synthetic Heart".equals(item.name) || "Superlite Motor".equals(item.name) || "Electron Transmitter".equals(item.name)) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterPowder() {
        if (powder == 2) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains(" Powder")) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains(" Powder")) {
                        System.out.println("adding " + item.name);
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else if (powder == 1) {
            System.out.println("Enabling pow");
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                System.out.println("waypointloop");
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    System.out.println("count: " + item.count);
                    if (!item.count.contains("-")) continue;
                    System.out.println("upper limit: " + Integer.parseInt(item.count.split("-")[1]));
                    if (item.name.contains(" Powder") && Integer.parseInt(item.count.split("-")[1]) > 1200) {
                        if (filteredItems.containsKey(item.name)) {
                            CrystalHollowsItemTotal itemTotal = filteredItems.get(item.name);
                            String itemCountExisting = itemTotal.itemCount;
                            filteredItems.put(item.name, CrystalHollowsItemTotal.sumPowder(itemCountExisting, item, itemTotal));
                        } else {
                            CrystalHollowsItemTotal itemTotal = new CrystalHollowsItemTotal();
                            itemTotal.itemCount = item.count;
                            itemTotal.itemName = item.name;
                            itemTotal.itemColor = item.itemColor;
                            itemTotal.countColor = item.numberColor;
                            filteredItems.put(item.name, itemTotal);
                        }
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }

                    }
                }
            }
        } else {
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains(" Powder")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains(" Powder")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }

            CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());
        }
    }
    public static void filterPrehistoricEggs() {
        if (prehistoricEggs) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("Prehistoric Egg".equals(item)) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Prehistoric Egg".equals(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            System.out.println("Disabling");
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if ("Prehistoric Egg".equals(entry.getKey())) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Prehistoric Egg".equals(item.name)) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterPickonimbus() {
        if (pickonimbus) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Pickonimbus")) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Pickonimbus")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains("Pickonimbus")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Pickonimbus")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterBlueEggs() {
        if (blueEggs) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("Blue Goblin Egg".equals(item)) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));

                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Blue Goblin Egg".equals(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            goblinEggs = false;
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains("Goblin Egg")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Goblin Egg")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterGoblinEggs() {
        if (goblinEggs) {
            blueEggs = true;
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Goblin Egg")) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Goblin Egg")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains("Goblin Egg")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Goblin Egg")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterRoughGemstones() {
        if (roughGemstones) {
            fineGemstones = true;
            jasperGemstones = true;
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Gemstone") && !item.contains("Powder")) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Gemstone") && !item.name.contains("Powder")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            System.out.println("disabling");
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains("Rough") || entry.getKey().contains("Flawed")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Rough") || item.name.contains("Flawed")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterFineGemstones() {
        if (fineGemstones) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Fine") || item.contains("Flawless")) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Fine") || item.name.contains("Flawless")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            roughGemstones = false;
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains("Rough") || entry.getKey().contains("Flawed") || entry.getKey().contains("Fine") || entry.getKey().contains("Flawless")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Rough") || item.name.contains("Flawed") || item.name.contains("Fine") || item.name.contains("Flawless")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterJasperGemstones() {
        if (jasperGemstones) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Jasper")) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Jasper")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            roughGemstones = false;
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if (entry.getKey().contains("Jasper") || entry.getKey().contains("Rough") || entry.getKey().contains("Flawed")) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains("Jasper") || item.name.contains("Rough") || item.name.contains("Flawed")) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }
    public static void filterMisc() {
        if (junk) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("Wishing Compass".equals(item) || "Treasurite".equals(item) || "Jungle Heart".equals(item) || "Oil Barrel".equals(item) || "Sludge Juice".equals(item) || "Ascension Rope".equals(item) || "Yoggie".equals(item) || ServerConnection.newMiscCHItems.contains(item)) {
                    filteredItems.put(item, CHWaypoints.itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Wishing Compass".equals(item.name) || "Treasurite".equals(item.name) || "Jungle Heart".equals(item.name) || "Oil Barrel".equals(item.name) || "Sludge Juice".equals(item.name) || "Ascension Rope".equals(item.name) || "Yoggie".equals(item.name) || ServerConnection.newMiscCHItems.contains(item.name)) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else {
            for (Entry<String, CrystalHollowsItemTotal> entry : filteredItems.entrySet()) {
                if ("Wishing Compass".equals(entry.getKey()) || "Treasurite".equals(entry.getKey()) || "Jungle Heart".equals(entry.getKey()) || "Oil Barrel".equals(entry.getKey()) || "Sludge Juice".equals(entry.getKey()) || "Ascension Rope".equals(entry.getKey()) || "Yoggie".equals(entry.getKey()) || ServerConnection.newMiscCHItems.contains(entry.getKey())) {
                    filteredItems.remove(entry.getKey());
                }
            }
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("Wishing Compass".equals(item.name) || "Treasurite".equals(item.name) || "Jungle Heart".equals(item.name) || "Oil Barrel".equals(item.name) || "Sludge Juice".equals(item.name) || "Ascension Rope".equals(item.name) || "Yoggie".equals(item.name) || ServerConnection.newMiscCHItems.contains(item.name)) {
                        waypoint.filteredExpandedItems.remove(item);
                    }
                }
            }
        }

    }

}
