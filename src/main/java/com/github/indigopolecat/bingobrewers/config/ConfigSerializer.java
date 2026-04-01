package com.github.indigopolecat.bingobrewers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import java.awt.Color;

public class ConfigSerializer<T extends ConfigData> extends GsonConfigSerializer<T> {
    public ConfigSerializer(Config definition, Class<T> configClass) {
        super(definition, configClass, (new GsonBuilder()).setPrettyPrinting().registerTypeAdapter(Color.class, new ColorTypeAdapter()).create());
    }
}
