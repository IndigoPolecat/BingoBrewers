package com.github.indigopolecat.bingobrewers.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HudManager {
    public static final List<Hud> activeHuds = new CopyOnWriteArrayList<>();
    
    public static void initialize() {
        HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("bingobrewers","huds_all"), (graphics, delta) -> {
            for(Hud hud : activeHuds) {
                if(hud.isExpired()) {
                    activeHuds.remove(hud);
                } else hud.render(graphics, delta);
            }
        });
    }
    
    public static void addNewHud(Hud hud) {
        activeHuds.add(hud);
    }
    
    public static void removeHud(Hud hud) {
        activeHuds.remove(hud);
    }
}
