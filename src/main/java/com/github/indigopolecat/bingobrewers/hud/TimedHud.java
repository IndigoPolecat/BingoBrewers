package com.github.indigopolecat.bingobrewers.hud;

import lombok.*;

/**
 * Implementation of {@link Hud} that will be active for a set number of milliseconds
 */
public interface TimedHud extends Hud {
    long getStartTime();
    long getDisplayTime();
    
    @Override
    default boolean expired() {
        return getStartTime() + getDisplayTime() < System.currentTimeMillis();
    }
}
