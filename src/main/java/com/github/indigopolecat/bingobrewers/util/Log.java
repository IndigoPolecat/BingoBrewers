package com.github.indigopolecat.bingobrewers.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
    public static final Logger LOGGER = Logger.getLogger("bingobrewers");
    
    public static void info(String message) {
        LOGGER.info(message);
    }
    
    public static void info(String message, Throwable throwable) {
        LOGGER.log(Level.INFO, message, throwable);
    }
    
    public static void warn(String message) {
        LOGGER.warning(message);
    }
    
    public static void error(String message) {
        LOGGER.severe(message);
    }
    
    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }
    
    public static void debug(String message) {
        LOGGER.fine(message);
    }
}
