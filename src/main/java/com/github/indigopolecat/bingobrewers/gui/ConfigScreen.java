package com.github.indigopolecat.bingobrewers.gui;

import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class ConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(BingoBrewersConfig.class, parent).get();
    }
    
    
}
