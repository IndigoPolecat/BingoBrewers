package com.github.indigopolecat.bingobrewers;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.config.v1.core.OneColor;
import org.polyfrost.oneconfig.api.config.v1.data.InfoType;
import org.polyfrost.oneconfig.api.config.v1.data.OptionSize;
import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import com.github.indigopolecat.bingobrewers.Hud.SplashHud;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.kryo.KryoNetwork;
import net.minecraft.client.Minecraft;
import org.polyfrost.polyui.color.PolyColor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.github.indigopolecat.bingobrewers.CHWaypoints.filteredWaypoints;
import static com.github.indigopolecat.bingobrewers.CHWaypoints.itemCounts;
import static com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud.filteredItems;
import static com.github.indigopolecat.bingobrewers.ServerConnection.*;
import static org.polyfrost.polyui.color.ColorUtils.argb;

public class BingoBrewersConfig extends Config {
    public BingoBrewersConfig() {
        //super(new Mod("Bingo Brewers", ModType.SKYBLOCK), "bingobrewers.json");
        super("bingobrewers.json", "Bingo Brewers", Category.HYPIXEL);

        List<String> crystalHollowsWaypoints = Arrays.asList("robotParts", "powder", "prehistoricEggs", "pickonimbus", "goblinEggs", "roughGemstones", "jasperGemstones", "junk", "CHHud", "waypointFate");
        for (String option : crystalHollowsWaypoints) {
            addDependency(option, "crystalHollowsWaypointsToggle");
        }
        addDependency("justifySeparation", "justifyAlignmentCHHud");
        addListener("robotParts", BingoBrewersConfig::robotPartsCall);
        addListener("powder", BingoBrewersConfig::powderCall);
        addListener("prehistoricEggs", BingoBrewersConfig::prehistoricEggsCall);
        addListener("pickonimbus", BingoBrewersConfig::pickonimbusCall);
        addListener("goblinEggs", BingoBrewersConfig::goblinEggsCall);
        addListener("roughGemstones", BingoBrewersConfig::roughCall);
        addListener("jasperGemstones", BingoBrewersConfig::roughCall);
        addListener("junk", BingoBrewersConfig::miscCall);
        addListener("crystalHollowsWaypointsToggle", BingoBrewersConfig::SubscribeToServer);

        addListener("showSplasher",  ServerConnection::setSplashHudItems);
        addListener("showParty",  ServerConnection::setSplashHudItems);
        addListener("showLocation",  ServerConnection::setSplashHudItems);
        addListener("showNote",  ServerConnection::setSplashHudItems);

    }

    /* -----------------------------------------------------------
         Splash Notifications
       ----------------------------------------------------------- */

    @Switch(
            title = "Splash Notifications",
            category = "Splash Notifications",
            description = "Enable or disable splash notifications"
    )
    public static boolean splashNotificationsEnabled = true;

    /*@Info(
            text = "Leeching splashes on high level profiles is not allowed!",
            type = InfoType.ERROR,
            category = "Splash Notifications",
            size = OptionSize.DUAL
    )
    public static boolean ignored;*/

    /*@Switch(
            name = "Show Splash Notifications on Non-Bingo Profiles",
            category = "Splash Notifications",
            description = "Whether to show splash notifications regardless of your last active profile."
    )
    public static boolean splashNotificationsInBingo = true;*/

    @Switch(
            title = "Show Splash Notifications outside of Skyblock",
            category = "Splash Notifications",
            description = "Whether to show splash notifications outside of Skyblock AND the Prototype Lobby or Limbo."
    )
    public static boolean splashNotificationsOutsideSkyblock = true;

    @HUD(
            name = "Splash Notification HUD",
            category = "Splash Notifications"

    )
    public SplashHud hud = new SplashHud();

    @Checkbox(
            title = "Show Splasher",
            category = "Splash Notifications",
            description = "Include the splasher's IGN in splash notifications"
    )
    public static boolean showSplasher = true;

    @Checkbox(
            title = "Show Party",
            category = "Splash Notifications",
            description = "Include the bingo party listed in the splash message in splash notifications"
    )
    public static boolean showParty = true;

    @Checkbox(
            title = "Show Location",
            category = "Splash Notifications",
            description = "Include the location in splash notifications"
    )
    public static boolean showLocation = true;

    @Checkbox(
            title = "Show Note",
            category = "Splash Notifications",
            description = "Show any extra information the splasher included in the splash notification"
    )
    public static boolean showNote = true;

    @Slider(
            title = "Notification Volume",
            category = "Splash Notifications",
            description = "Set the volume of the splash notification",
            min = 0f, max = 200f
    )
    public static float splashNotificationVolume = 100f;

    @Color(
            title = "Alert Text Color",
            category = "Splash Notifications",
            description = "Set the color of the alert text (e.g. \"Splash in Hub 14\")",
            alpha = false
    )
    public static PolyColor alertTextColor = argb(0xFF8BAFE0);

        /* -----------------------------------------------------------
             Crystal Hollows
           ----------------------------------------------------------- */


    @Switch(
            title = "Crystal Hollows Waypoints",
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
            title = "Waypoints After Opening",
            options = {"Strikethrough", "Remove", "Do Nothing"},
            category = "Crystal Hollows Waypoints",
            description = "How to display waypoints once you have opened the chest."
    )
    public static int waypointFate = 0;

    @Dropdown(
            title = "Powder",
            options = {"All", "Only 1200+ Powder", "None"},
            category = "Crystal Hollows Waypoints",
            description = "Include Mithril and Gemstone powder."
    )
    public static int powder = 0;

    @Dropdown(
            title = "Goblin Eggs",
            options = {"All", "Blue Only", "None"},
            category = "Crystal Hollows Waypoints",
            description = "Include Goblin Eggs."
    )
    public static int goblinEggs = 0;

    @Dropdown(
            title = "Gemstones",
            options = {"All", "Fine/Flawless Only", "None"},
            category = "Crystal Hollows Waypoints",
            description = "Include gemstones found in chests.",
    )
    public static int roughGemstones = 1;

    @Checkbox(
            title = "Jasper Gemstones",
            category = "Crystal Hollows Waypoints",
            description = "Separate toggle for all Jasper gemstones, they can reveal Fairy Grottos."
    )
    public static boolean jasperGemstones = true;

    @Checkbox(
            title = "Robot Parts",
            category = "Crystal Hollows Waypoints",
            description = "Include all 6 Robot Parts."
    )
    public static boolean robotParts = true;


    @Checkbox(
            title = "Prehistoric Eggs",
            category = "Crystal Hollows Waypoints",
            description = "Include Prehistoric Eggs."
    )
    public static boolean prehistoricEggs = true;

    @Checkbox(
            title = "Pickonimbus 2000",
            category = "Crystal Hollows Waypoints",
            description = "Include Pickonimbus 2000s."
    )
    public static boolean pickonimbus = true;

    @Checkbox(
            title = "Misc.",
            category = "Crystal Hollows Waypoints",
            description = "Wishing Compasses, Treasurite, Jungle Hearts, Oil Barrels, Sludge Juice, Ascension Ropes, Yoggies."
    )
    public static boolean junk = true;

    /* -----------------------------------------------------------
         Bingo Shop
       ----------------------------------------------------------- */

    @Switch(
            title = "Show Coins/Bingo Point",
            category = "Bingo Shop",
            description = "Show coins per Bingo Point in the Bingo Shop."
    )
    public static boolean showCoinsPerBingoPoint = true;

    @Switch(
            title = "Display Missing Bingoes",
            category = "Bingo Shop",
            description = "Display how many Bingoes are required to buy the item."
    )
    public static boolean displayMissingBingoes = true;

    /* -----------------------------------------------------------
         Misc
       ----------------------------------------------------------- */

    @Dropdown(
            title = "Auto Updater Versions",
            category = "Misc",
            description = "Choose which updates should the auto-updater look for",
            options = {"Stable", "Beta", "None"}
    )
    public static int autoUpdaterType = 0;

    @Switch(
            title = "Auto Download",
            category = "Misc",
            description = "Auto download updates when available. Requires restart."
    )
    public static boolean autoDownload = false;

    @Button(
            title = "Check for Updates",
            category = "Misc",
            description = "Check for updates",
            text = "Click"
    )
    public void checkForUpdates() {
        Minecraft.getMinecraft().displayGuiScreen(new UpdateScreen());
    }

    @Info(
            title = "Running version " + BingoBrewers.version,
            type = InfoType.INFO,
            category = "Misc",
            size = OptionSize.DUAL
    )
    public static boolean ignoredL;

    @Switch(
            title = "Display Missing Bingo Points",
            category = "Misc",
            description = "Display the amount of missing Bingo Points to buy the item."
    )
    public static boolean displayMissingBingoPoints = true;
    \
    @Switch(
            title = "Chicken Head Reset Alert",
            category = "Misc",
            description = "Display a message if the Chicken Head cooldown is reset."
    )
    public static boolean displayEggTimerReset = false;

    @Text(
            title = "Chicken Head Alert Message",
            category = "Misc",
            description = "What to display when the Chicken cooldown is reset. (Use & for § in COLOR only codes)"
    )
    public static String eggTimerMessage = "&aCrouch";

    @Switch(
            title = "Chicken Head Reset Sound",
            category = "Misc",
            description = "Play a sound if the Chicken Head cooldown is reset."
    )
    public static boolean playEggTimerResetSound = false;

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
        if (robotParts) {
            for (String item : itemCounts.keySet()) {
                if ("FTX 3070".equals(item) || "Robotron Reflector".equals(item) || "Control Switch".equals(item) || "Synthetic Heart".equals(item) || "Superlite Motor".equals(item) || "Electron Transmitter".equals(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }
            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if ("FTX 3070".equals(item.name) || "Robotron Reflector".equals(item.name) || "Control Switch".equals(item.name) || "Synthetic Heart".equals(item.name) || "Superlite Motor".equals(item.name) || "Electron Transmitter".equals(item.name)) {
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
        if (powder == 0) {
            for (String item : itemCounts.keySet()) {
                if (item.contains(" Powder")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
                for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                    if (item.name.contains(" Powder")) {
                        waypoint.filteredExpandedItems.add(item);
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        } else if (powder == 1) {
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
                        if (!CHWaypoints.filteredWaypoints.contains(waypoint)) {
                            CHWaypoints.filteredWaypoints.add(waypoint);
                        }
                    }
                }
            }
        }
        CHWaypoints.filteredWaypoints.removeIf(waypoint -> waypoint.filteredExpandedItems.isEmpty());

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
        if (prehistoricEggs) {
            for (String item : itemCounts.keySet()) {
                if ("Prehistoric Egg".equals(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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
        if (pickonimbus) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Pickonimbus")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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
        if (goblinEggs == 0) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Goblin Egg")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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
            for (String item : itemCounts.keySet()) {
                if ("Blue Goblin Egg".equals(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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
        if (roughGemstones == 0) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Gemstone") && !item.contains("Powder")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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
            for (String item : itemCounts.keySet()) {
                if (item.contains("Fine") || item.contains("Flawless")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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
        if (jasperGemstones && roughGemstones != 0) {
            for (String item : itemCounts.keySet()) {
                if (item.contains("Jasper")) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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

    public static void miscCall() {
        filterMisc();
        organizeWaypoints();
    }
    public static void filterMisc() {
        filteredItems.removeIf(entry -> "Wishing Compass".equals(entry.itemName) || "Treasurite".equals(entry.itemName) || "Jungle Heart".equals(entry.itemName) || "Oil Barrel".equals(entry.itemName) || "Sludge Juice".equals(entry.itemName) || "Ascension Rope".equals(entry.itemName) || "Yoggie".equals(entry.itemName) || ServerConnection.newMiscCHItems.contains(entry.itemName));
        for (CHWaypoints waypoint : waypoints) {
            for (KryoNetwork.CHChestItem item : waypoint.expandedName) {
                if ("Wishing Compass".equals(item.name) || "Treasurite".equals(item.name) || "Jungle Heart".equals(item.name) || "Oil Barrel".equals(item.name) || "Sludge Juice".equals(item.name) || "Ascension Rope".equals(item.name) || "Yoggie".equals(item.name) || ServerConnection.newMiscCHItems.contains(item.name)) {
                    waypoint.filteredExpandedItems.remove(item);
                }
            }
        }
        if (junk) {
            for (String item : itemCounts.keySet()) {
                if ("Wishing Compass".equals(item) || "Treasurite".equals(item) || "Jungle Heart".equals(item) || "Oil Barrel".equals(item) || "Sludge Juice".equals(item) || "Ascension Rope".equals(item) || "Yoggie".equals(item) || ServerConnection.newMiscCHItems.contains(item)) {
                    filteredItems.add(itemCounts.get(item));
                }
            }

            for (CHWaypoints waypoint : waypoints) {
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

    public static void SubscribeToServer() {
        if (crystalHollowsWaypointsToggle && PlayerInfo.playerLocation.equalsIgnoreCase("crystal_hollows")) {
            KryoNetwork.SubscribeToCHServer CHRequest = new KryoNetwork.SubscribeToCHServer();
            CHRequest.server = PlayerInfo.currentServer;
            CHRequest.day = PlayerInfo.day;
            ServerConnection.SubscribeToCHServer(CHRequest);
        } else {
            waypoints.clear();
            itemCounts.clear();
            filteredItems.clear();
            filteredWaypoints.clear();
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
