package com.github.indigopolecat.bingobrewers.mixin;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.HUD;
import org.polyfrost.oneconfig.api.config.v1.core.ConfigUtils;
import org.polyfrost.oneconfig.api.config.v1.elements.BasicOption;
import org.polyfrost.oneconfig.api.config.v1.elements.OptionPage;
import org.polyfrost.oneconfig.hud.HUDUtils;
import org.polyfrost.oneconfig.hud.Hud;
import com.github.indigopolecat.bingobrewers.util.HudCore;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.Hud.CrystalHollowsHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@SuppressWarnings("ALL")
@Mixin(value = HUDUtils.class, remap = false)
public class HUDUtilsMixin {

    @Inject(method = "addHudOptions", at = @At("TAIL"))
    private static void hudUtils$modifyOptions(OptionPage page, Field field, Object instance, Config config, CallbackInfo ci) {
        Hud hud = (Hud) ConfigUtils.getField(field, instance);
        if (!(hud instanceof CrystalHollowsHud)) return;
        HUD hudAnnotation = field.getAnnotation(HUD.class);
        HudCore.hudOptions.removeIf(HUDUtilsMixin::hudUtils$addDependency);
        ConfigUtils.getSubCategory(page, hudAnnotation.category(), hudAnnotation.subcategory()).options.removeIf(HUDUtilsMixin::hudUtils$addDependency);
    }

    @Unique
    private static boolean hudUtils$addDependency(BasicOption option) {
        if (!(option.getParent() instanceof CrystalHollowsHud)) return false;
        option.addDependency("crystalHollowsWaypointsToggle", () -> BingoBrewersConfig.crystalHollowsWaypointsToggle);
        return false;
    }

}