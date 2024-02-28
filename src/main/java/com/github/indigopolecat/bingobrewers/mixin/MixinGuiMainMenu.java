package com.github.indigopolecat.bingobrewers.mixin;

import com.github.indigopolecat.bingobrewers.Item;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Logger;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    Logger logger = Logger.getLogger(MixinGuiMainMenu.class.getName());

    @Inject(method = "initGui", at = @At("HEAD"))
    public void onInitGui(CallbackInfo ci) {
        logger.info("Hello from Main Menu!");
    }
}
