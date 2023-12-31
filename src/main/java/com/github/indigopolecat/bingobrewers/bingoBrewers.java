package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "bingobrewers", useMetadata=true)
public class bingoBrewers {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        doneLoading doneLoading = new doneLoading();
        MinecraftForge.EVENT_BUS.register(new bingoShop());
        MinecraftForge.EVENT_BUS.register(doneLoading);
    }
}
