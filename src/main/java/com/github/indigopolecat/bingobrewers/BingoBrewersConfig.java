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

import static com.github.indigopolecat.bingobrewers.ServerConnection.filteredItems;

public class BingoBrewersConfig extends Config {
    public BingoBrewersConfig() {
        super(new Mod("Bingo Brewers", ModType.SKYBLOCK), "bingobrewers.json");
        initialize();
        List<String> crystalHollowsWaypoints = Arrays.asList("robotParts", "powder", "prehistoricEggs", "pickonimbus", "goblinEggs", "roughGemstones", "jasperGemstones", "junk", "CHHud", "waypointFate");
        for (String option : crystalHollowsWaypoints) {
            addDependency(option, "crystalHollowsWaypointsToggle");
        }
        addDependency("justifySeparation", "justifyAlignmentCHHud");
        addListener("robotParts", BingoBrewersConfig::filterRobotParts);
        addListener("powder", BingoBrewersConfig::filterPowder);
        addListener("prehistoricEggs", BingoBrewersConfig::filterPrehistoricEggs);
        addListener("pickonimbus", BingoBrewersConfig::filterPickonimbus);
        addListener("goblinEggs", BingoBrewersConfig::filterGoblinEggs);
        addListener("roughGemstones", BingoBrewersConfig::filterRoughGemstones);
        addListener("jasperGemstones", BingoBrewersConfig::filterJasperGemstones);
        addListener("junk", BingoBrewersConfig::filterMisc);

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
            name = "Crystal Hollows Loot",
            category = "Crystal Hollows Waypoints"
    )
    public CrystalHollowsHud CHHud = new CrystalHollowsHud();

    @Dropdown(
            name = "Waypoints After Opening",
            options = {"Strikethrough", "Remove", "Do Nothing"},
            category = "Crystal Hollows Waypoints",
            description = "How to display waypoints once you have opened the chest.",
            size = OptionSize.DUAL
    )
    public static int waypointFate = 0;

    @Dropdown(
            name = "Powder",
            options = {"None", "Only 1200+ Powder", "All"},
            category = "Crystal Hollows Waypoints",
            description = "Include Mithril and Gemstone powder.",
            size = OptionSize.DUAL
    )
    public static int powder = 2;

    @Dropdown(
            name = "Goblin Eggs",
            options = {"All", "Blue Only", "None"},
            category = "Crystal Hollows Waypoints",
            description = "Include Goblin Eggs.",
            size = OptionSize.DUAL
    )
    public static int goblinEggs = 0;

    @Dropdown(
            name = "Gemstones",
            options = {"All", "Fine/Flawless Only", "None"},
            category = "Crystal Hollows Waypoints",
            description = "Include gemstones found in chests.",
            size = OptionSize.DUAL
    )
    public static int roughGemstones = 1;

    @Checkbox(
            name = "Jasper Gemstones",
            category = "Crystal Hollows Waypoints",
            description = "Separate toggle for all Jasper gemstones, they can reveal Fairy Grottos.",
            size = OptionSize.DUAL
    )
    public static boolean jasperGemstones = true;

    @Checkbox(
            name = "Robot Parts",
            category = "Crystal Hollows Waypoints",
            description = "Include all 6 Robot Parts.",
            size = OptionSize.DUAL
    )
    public static boolean robotParts = true;


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
            name = "Misc.",
            category = "Crystal Hollows Waypoints",
            description = "Wishing Compasses, Treasurite, Jungle Hearts, Oil Barrels, Sludge Juice, Ascension Ropes, Yoggies.",
            size = OptionSize.DUAL
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
            name = "Display Missing Bingo Events",
            category = "Misc",
            description = "Display how many Bingo Events are required to buy the item."
    )
    public static boolean displayMissingBingoes = true;

    @Switch(
            name = "Chicken Head Reset Alert",
            category = "Misc",
            description = "Display a message if the Chicken Head cooldown is reset."
    )
    public static boolean displayEggTimerReset = false;

    @Text(
            name = "Chicken Head Alert Message",
            category = "Misc",
            description = "What to display when the Chicken cooldown is reset. (Use & for § in COLOR only codes)"
    )
    public static String eggTimerMessage = "&aCrouch";

    @Switch(
            name="Chicken Head Reset Sound",
            category = "Misc",
            description = "Play a sound if the Chicken Head cooldown is reset."
    )
    public static boolean playEggTimerResetSound = false;

    public static void filterRobotParts() {
        if (robotParts) {
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("FTX 3070".equals(item) || "Robotron Reflector".equals(item) || "Control Switch".equals(item) || "Synthetic Heart".equals(item) || "Superlite Motor".equals(item) || "Electron Transmitter".equals(item)) {
                    if (CrystalHollowsHud.lootCount == 0) {
                        filteredItems.add(CHWaypoints.itemCounts.get(item));
                    } else if (CrystalHollowsHud.lootCount == 1) {

                    }
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
            filteredItems.removeIf(item -> "FTX 3070".equals(item.itemName) || "Robotron Reflector".equals(item.itemName) || "Control Switch".equals(item.itemName) || "Synthetic Heart".equals(item.itemName) || "Superlite Motor".equals(item.itemName) || "Electron Transmitter".equals(item.itemName));

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
        filteredItems.removeIf(entry -> entry.itemName.contains(" Powder"));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains(" Powder")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (powder == 2) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains(" Powder")) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
            for (CHWaypoints waypoint : ServerConnection.waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (!item.count.contains("-")) continue;
                    if (item.name.contains(" Powder") && Integer.parseInt(item.count.split("-")[1]) > 1200) {
                        boolean summed = false;
                        for (CrystalHollowsItemTotal itemTotal : filteredItems) {
                            if (itemTotal.itemName.equals(item.name)) {
                                String itemCountExisting = itemTotal.itemCount;
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
                            if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                                CHWaypoints.filteredWaypoints.add(waypoint);
                            }
                            if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                                CHWaypoints.filteredWaypoints.add(waypoint);
                            }
                        }

                    }
                }
            }
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
    public static void filterPrehistoricEggs() {
        filteredItems.removeIf(entry -> "Prehistoric Egg".equals(entry.itemName));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if ("Prehistoric Egg".equals(item.name)) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (prehistoricEggs) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("Prehistoric Egg".equals(item)) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
    public static void filterPickonimbus() {
        filteredItems.removeIf(entry -> entry.itemName.equals("Pickonimbus"));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Pickonimbus")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (pickonimbus) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Pickonimbus")) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
    public static void filterGoblinEggs() {
        filteredItems.removeIf(entry -> entry.itemName.equals("Goblin Egg"));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Goblin Egg")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (goblinEggs == 0) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Goblin Egg")) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        } else if (goblinEggs == 1) {
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("Blue Goblin Egg".equals(item)) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
    public static void filterRoughGemstones() {
        filteredItems.removeIf(entry -> entry.itemName.contains("Gemstone") && !entry.itemName.contains("Powder"));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Gemstone") && !item.name.contains("Powder")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (roughGemstones == 0) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Gemstone") && !item.contains("Powder")) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        } else if (roughGemstones == 1) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Fine") || item.contains("Flawless")) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
    public static void filterJasperGemstones() {
        filteredItems.removeIf(entry -> entry.itemName.contains("Jasper"));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if (item.name.contains("Jasper")) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (jasperGemstones && roughGemstones != 0) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if (item.contains("Jasper")) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }
    public static void filterMisc() {
        filteredItems.removeIf(entry -> "Wishing Compass".equals(entry.itemName) || "Treasurite".equals(entry.itemName) || "Jungle Heart".equals(entry.itemName) || "Oil Barrel".equals(entry.itemName) || "Sludge Juice".equals(entry.itemName) || "Ascension Rope".equals(entry.itemName) || "Yoggie".equals(entry.itemName) || ServerConnection.newMiscCHItems.contains(entry.itemName));
        for (CHWaypoints waypoint : ServerConnection.waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if ("Wishing Compass".equals(item.name) || "Treasurite".equals(item.name) || "Jungle Heart".equals(item.name) || "Oil Barrel".equals(item.name) || "Sludge Juice".equals(item.name) || "Ascension Rope".equals(item.name) || "Yoggie".equals(item.name) || ServerConnection.newMiscCHItems.contains(item.name)) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (junk) {
            System.out.println("Enabling");
            for (String item : CHWaypoints.itemCounts.keySet()) {
                if ("Wishing Compass".equals(item) || "Treasurite".equals(item) || "Jungle Heart".equals(item) || "Oil Barrel".equals(item) || "Sludge Juice".equals(item) || "Ascension Rope".equals(item) || "Yoggie".equals(item) || ServerConnection.newMiscCHItems.contains(item)) {
                    filteredItems.add(CHWaypoints.itemCounts.get(item));
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
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

    }


}
