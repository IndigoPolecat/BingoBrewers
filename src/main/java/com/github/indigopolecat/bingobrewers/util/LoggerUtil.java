package com.github.indigopolecat.bingobrewers.util;

import java.util.logging.Logger;

public class LoggerUtil {

    private LoggerUtil() {
        throw new IllegalStateException("Utility class");
    }
    public static final Logger LOGGER = Logger.getLogger("bingobrewers");
}