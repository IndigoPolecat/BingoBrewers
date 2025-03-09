package com.github.indigopolecat.bingobrewers.mixin;

import com.github.indigopolecat.bingobrewers.util.SplashUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.github.indigopolecat.bingobrewers.ChestInventories.removeFormatting;

@Mixin(ItemModelMesher.class)
public abstract class ItemModelMesherMixin {

    @Unique
    private static ItemStack bingoBrewers$lastItemProcessed;
    @Unique
    private static final int bingoBrewers$STAINED_HARDENED_CLAY_ID = 159;
    @Unique
    private static final int bingoBrewers$RED_DAMAGE = 14; // damage is the color of the item, 14 is red
    @Unique
    private static final int bingoBrewers$YELLOW_DAMAGE = 4; // damage is the color, 4 is yellow

    // chat gpt's method of calling the original method again
    @Invoker("getItemModel")
    protected abstract IBakedModel invokeGetItemModel(ItemStack stack);

    @Inject(method = "getItemModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/resources/model/IBakedModel;", at = @At("HEAD"), cancellable = true)
    private void getItemModel_bb(ItemStack item, CallbackInfoReturnable<IBakedModel> cir) {
        if (item == null || item.isItemEqual(bingoBrewers$lastItemProcessed)) return; // don't process the same item twice

        // Get the lore of the item
        List<String> itemLore = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        boolean hubSelectorItem = false;
        String server = null;

        // 14 is the color red, 159 is hardened clay, which represents the hub you are currently in
        if (Item.getIdFromItem(item.getItem()) == bingoBrewers$STAINED_HARDENED_CLAY_ID && item.getItemDamage() == bingoBrewers$RED_DAMAGE) return;
        // loop through the lore lines of the item
        for (String s : itemLore) {
            // look for the lore line that contains the hub number
            if (s.contains("Hub #")) {
                // Match the hub number and remove formatting codes
                hubSelectorItem = true;
            } else if (s.contains("Server:") && hubSelectorItem) { // Look for the lore line containing the server id, but if the hub number hasn't been set yet ignore
                // Match the server id and remove formatting codes
                server = s.replaceAll("Server: ((mini|mega)\\d{1,4}[a-zA-Z]{1,4})", "$1");
                server = removeFormatting(server);
            }


            if (server != null && SplashUtils.splashServerIDs.contains(server)) {
                ItemStack newItem = item.copy();
                newItem.setItem(Item.getItemById(bingoBrewers$STAINED_HARDENED_CLAY_ID));
                newItem.setItemDamage(bingoBrewers$YELLOW_DAMAGE);


                bingoBrewers$lastItemProcessed = newItem;
                IBakedModel model = invokeGetItemModel(newItem);
                cir.setReturnValue(model);

            }
        }
    }
}
