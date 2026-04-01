package com.github.indigopolecat.bingobrewers.hud;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;

public class SplashTitleHud extends TitleHud {
    private static SplashTitleHud previous;
    
    public SplashTitleHud(String hub) {
        super(1000L * BingoBrewersConfig.getConfig().splashConfig.alertDisplayTime, "Splash in HUB " + hub, BingoBrewersConfig.getConfig().alertTextColorHex);
        
        if(previous != null && !previous.isExpired()) HudManager.removeHud(previous);
        previous = this;
    }
    
    @Override
    public boolean isExpired() {
        return this == previous && super.isExpired();
    }
}
