package com.github.indigopolecat.bingobrewers.hud;

import lombok.*;

public class TimedTextHud extends TextHud implements TimedHud {
    @Getter private final long startTime = System.currentTimeMillis();
    @Getter private final long displayTime;
    
    public TimedTextHud(long displayTime, int defaultColor, String text) {
        super(defaultColor, text);
        this.displayTime = displayTime;
    }
    
    public TimedTextHud(long displayTime, int defaultColor, float scale, String text) {
        super(defaultColor, scale, text);
        this.displayTime = displayTime;
    }
    
    public TimedTextHud(long displayTime, int defaultColor, String... text) {
        super(defaultColor, text);
        this.displayTime = displayTime;
    }
    
    public TimedTextHud(long displayTime, int defaultColor, float scale, String... text) {
        super(defaultColor, scale, text);
        this.displayTime = displayTime;
    }
    
    @Override
    public boolean isExpired() {
        return TimedHud.super.isExpired();
    }
}
