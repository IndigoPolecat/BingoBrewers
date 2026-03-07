package com.github.indigopolecat.bingobrewers.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Represent a GUI/Hud that is active
 */
public interface Hud {
    /**
     * Method to render the Hud.
     * This is the same as {@link net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement#render(GuiGraphics,DeltaTracker)}
     *
     * @param graphics the {@link GuiGraphics} used for rendering
     * @param tickCounter the {@link DeltaTracker} providing timing information
     */
    void render(GuiGraphics graphics, DeltaTracker tickCounter);
    
    /**
     * Called every frame to determine if this HUD is valid and should be rendered via {@link #render(GuiGraphics, DeltaTracker)}
     *
     * @return {@code true} if this Hud instance should be rendered, {@code false} if this Hud should be destroyed
     */
    boolean expired();
}
