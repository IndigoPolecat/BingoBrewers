package com.github.indigopolecat.bingobrewers.mixin;

import com.github.indigopolecat.bingobrewers.PlayerInfo;
import com.github.indigopolecat.bingobrewers.Warping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class EntityPlayerSPMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage_bb(String message, CallbackInfo ci) {
        System.out.println("message: " + message);
        if (Warping.warpThread != null && System.currentTimeMillis() - Warping.warpThread.executionTimeBegan < 10000 && !Warping.whitelistedMessages.contains(message) && message.startsWith("/(p|party) \\w")) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§aBingo Brewers is currently warping players to your lobby (" + PlayerInfo.currentServer + "). Your command §9" + message + " §a will be sent momentarily"));
            Warping.messageQueue.add(message);
            ci.cancel();
        } else if (System.currentTimeMillis() - Warping.lastMessageSent < 150) {
            // fix "you are sending messages too fast" errors by delaying messages
            Warping.messageQueue.add(message);
            System.out.println("delaying: " + message);
            ci.cancel();
        } else Warping.whitelistedMessages.remove(message);

        Warping.lastMessageSent = System.currentTimeMillis();
    }
}
