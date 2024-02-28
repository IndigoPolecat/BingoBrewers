package com.github.indigopolecat.bingobrewers.mixin;

import com.github.indigopolecat.bingobrewers.util.LoggerUtil;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    @Inject(method = "initGui", at = @At("HEAD"))
    public void onInitGui(CallbackInfo ci) {
        LoggerUtil.LOGGER.info("Hello from Main Menu!");
    }
}