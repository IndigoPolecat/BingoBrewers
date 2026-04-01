package com.github.indigopolecat.bingobrewers.gui;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class ColorGuiProvider implements GuiProvider {
    
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        Color currentColor = Utils.getUnsafely(field, config);
        Color defaultColor = Utils.getUnsafely(field, defaults);
        
        // Use ARGB int value; Cloth Config's color entry uses int with alpha support
        int currentArgb = currentColor != null ? currentColor.getRGB() : 0xFF000000;
        int defaultArgb = defaultColor != null ? defaultColor.getRGB() : 0xFF000000;
        
        AbstractConfigListEntry entry = ConfigEntryBuilder.create()
                .startColorField(Component.translatable(i13n), currentArgb & 0x00FFFFFF)
                .setDefaultValue(defaultArgb & 0x00FFFFFF)
                .setAlphaMode(true)
                .setSaveConsumer(newValue -> {
                    Utils.setUnsafely(field, config, new Color(newValue, true));
                })
                .setTooltip(Component.translatable(i13n + ".@Tooltip"))
                .build();

        return Collections.singletonList(entry);
    }
}
