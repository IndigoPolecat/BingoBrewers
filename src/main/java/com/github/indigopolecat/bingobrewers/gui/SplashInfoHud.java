package com.github.indigopolecat.bingobrewers.gui;

import com.github.indigopolecat.bingobrewers.util.SplashNotificationInfo;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplashInfoHud {
    public static CopyOnWriteArrayList<SplashNotificationInfo> activeSplashes = new CopyOnWriteArrayList<>();
    public static SplashNotificationInfo exampleNotificationInfo = new SplashNotificationInfo(true);
    public static float unscaledHudHeight;
    public static int MAX_SPLASH_NOTIFICATIONS_VISIBLE = 2;
    public static int MAX_LINES_OF_INFO_BEFORE_CUTOFF = 6; // how many lines of info (non-prefix) are shown before cutting it off to preserve space
    public static int MAX_STRING_WIDTH = 300; // controls length of strings before they get wrapped to a new line
    public static String PREFIX_INFO_DIVIDER = ": "; // placed between the prefix line (e.g. "Hub") and the actual info (e.g. "14") as a separator
    public static Color COLOR_INFO = new Color(255, 255, 255); // White
    public static Color COLOR_DIVIDER = new Color(255, 255, 255); // White
    public static Color COLOR_PREFIX = new Color(255, 255, 85); // Yellow
    public static float LINE_GAP = 10; // gap between lines, multiplied by scaleY
    public static float TOP_BOTTOM_PADDING = 3; // padding between the edge of the HUD's box and where the text is rendered vertically, scaled

}
