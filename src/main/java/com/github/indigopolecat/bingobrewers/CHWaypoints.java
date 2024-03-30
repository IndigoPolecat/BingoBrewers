package com.github.indigopolecat.bingobrewers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class CHWaypoints {
    public static ArrayList<CHWaypoints> waypoints = new ArrayList<>();
    public BlockPos pos;
    public String shortName;
    public HashMap<String, Integer> expandedName;

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        // Render the waypoints
        renderPointLabel("test", new BlockPos(512, 70, 512), event.partialTicks);

    }

    public static void renderPointLabel(String label, BlockPos thisPoint, Float partialTicks) {
        // References to various instances
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        RenderManager rm = mc.getRenderManager();
        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();
        int threshold = 50;

        int width = fontRenderer.getStringWidth(label) / 2;

        // Get viewer positions
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        // Calculate real position relative to world
        // Distances in 3d coordinate systems: sqrt((x2 - x1)^2 + (y2 - y1)^2 + (z2 - z1)^2)
        // https://www.math.usm.edu/lambers/mat169/fall09/lecture17.pdf this link doesn't exist anymore, dk where it went
        double x = thisPoint.getX() - viewerX + 0.5f;
        double y = thisPoint.getY() - viewerY + viewer.getEyeHeight(); // Adjust for player height and bottom of block
        double z = thisPoint.getZ() - viewerZ + 0.5f;
        double dist = Math.sqrt(x * x + y * y + z * z);

        double scale = (dist * 0.0466666688F) / 10;
        if (scale < 0.0266666688F) {
            scale = 0.0266666688F;
        }


            int color = 0x8BAFE0;

        // Set rendering location and environment, then draw the text
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        fontRenderer.drawStringWithShadow(label, -width, 0, color);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

    }
}

