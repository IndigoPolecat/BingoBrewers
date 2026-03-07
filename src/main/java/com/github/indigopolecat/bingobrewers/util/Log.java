package com.github.indigopolecat.bingobrewers.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {
    public static final Logger LOG = LoggerFactory.getLogger("bingobrewers");
    
    public static void info(String message) {
        LOG.info(message);
    }
    
    public static void info(String message, Throwable throwable) {
        LOG.info(message, throwable);
    }
    
    public static void warn(String message) {
        LOG.warn(message);
    }
    
    public static void warn(String message, Throwable throwable) {
        LOG.warn(message, throwable);
    }
    
    public static void error(String message) {
        LOG.error(message);
    }
    
    public static void error(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }
    
    public static void debug(String message) {
        LOG.debug(message);
    }
}
