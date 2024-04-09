package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.CHChestItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Matrix4f;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class CHWaypoints {
    public BlockPos pos;
    public String shortName = "Crystal Hollows";
    public int shortNameColor = 0xAA00AA;
    public ArrayList<CHChestItem> expandedName;
    public CHWaypoints(int x, int y, int z, ArrayList<CHChestItem> chest) {
        this.pos = new BlockPos(x, y, z);
        this.expandedName = chest;

        for (KryoNetwork.CHChestItem chChestItem : chest) {
            String item = chChestItem.name;
            if (item.contains("Jasper")) {
                this.shortName = "Fairy Grotto";
                this.shortNameColor = 0xff55ff;
                break;
            }
        }
        if (this.shortName.equals("Crystal Hollows")) {
            if (y <= 63) {
                this.shortName = "Magma Fields";
                this.shortNameColor = 0xff5555;
            } else if (x >= 512 && z < 512) {
                this.shortName = "Mithril Deposits";
                this.shortNameColor = 0x00AA00;
            } else if (x < 512 && z < 512) {
                this.shortName = "Jungle";
                this.shortNameColor = 0x00AA00;
            } else if (x < 512 && z > 512) {
                this.shortName = "Goblin Holdout";
                this.shortNameColor = 0xFFAA00;
            } else if (x >= 512 && z > 512) {
                this.shortName = "Precursor Remnants";
                this.shortNameColor = 0x55FFFF;
            }
            // if none of these apply then it will be named Crystal Hollows (which is a valid region at z512 or so)
        }
    }

    public static void renderPointLabel(CHWaypoints label, BlockPos thisPoint, Float partialTicks) {
        System.out.println("rendering");
        // References to various instances
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        RenderManager rm = mc.getRenderManager();
        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        float fovY = 360f;
        float aspectRatio = (float) screenWidth / screenHeight;
        float nearPlane = 0.1f;
        float farPlane = 50000.0f;

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

        // Get the modelview and projection matrices
        FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);

// Combine modelview and projection matrices
        Matrix4f modelviewMatrix = new Matrix4f();
        modelviewMatrix.load(modelview);
        Matrix4f projectionMatrix = CHWaypoints.perspective(fovY, aspectRatio, nearPlane, farPlane);
        projectionMatrix.load(projection);

        // Construct a Vector4f with text position
        Vector4f textPosition = new Vector4f((float) x, (float) y, (float) z, 1.0f);

// Create a Matrix4f to hold the combined MVP matrix
        Matrix4f MVP = new Matrix4f();
        Matrix4f.mul(projectionMatrix, modelviewMatrix, MVP);

// Transform text position to clip space
        Matrix4f.transform(MVP, textPosition, textPosition);

// Normalize clip space coordinates
        float clipW = textPosition.w;
        textPosition.x /= clipW;
        textPosition.y /= clipW;
        textPosition.z /= clipW;

// Convert clip space coordinates to screen coordinates
        float screenX = (textPosition.x + 1) * screenWidth / 2;
        float screenY = (1 - textPosition.y) * screenHeight / 2;

// Check if the text is near the center of  the screen
        float centerThreshold = 30; // Adjust this value as needed
        float centerX = (float) screenWidth / 2;
        float centerY = (float) screenHeight / 2;
        boolean nearCenter = Math.abs(screenX - centerX) < centerThreshold && Math.abs(screenY - centerY) < centerThreshold;

        String distance = " (" + (int) dist + "M)";
        int distanceColor = 0xFFFFFF;

        if (dist > 300) {
            distanceColor = 0xFF5555;
        } else if (dist > 100) {
            distanceColor = 0xFFFF55;
        } else {
            distanceColor = 0x55FF55;
        }

        // adjust the position so it's actually around 30 blocks away so that it is always rendered
        // this means the actual position of the waypoint is around 30 blocks away while the waypoint appears to be several hundred
        if (dist > 30) {
            int waypointX = thisPoint.getX();
            int waypointY = thisPoint.getY();
            int waypointZ = thisPoint.getZ();

            // Math by FyreDrakon
            double vectorX = waypointX - viewerX;
            double vectorY = waypointY - viewerY;
            double vectorZ = waypointZ - viewerZ;
            double thirtyBlocksToTotalDistanceRatio = 30 / Math.sqrt(vectorX * vectorX + vectorY * vectorY + vectorZ * vectorZ);

            x = vectorX * thirtyBlocksToTotalDistanceRatio + 0.5;
            y = vectorY * thirtyBlocksToTotalDistanceRatio + viewer.getEyeHeight();
            z = vectorZ * thirtyBlocksToTotalDistanceRatio + 0.5;

            dist = Math.sqrt(x * x + y * y + z * z);

        }

        double scale = (dist * 0.0266666688F) / 10;
        if (scale < 0.0266666688F) {
            scale = 0.0266666688F;
        }

        // Set rendering location and environment, then draw the text
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int height = 0;
        int width = fontRenderer.getStringWidth(label.shortName);

        fontRenderer.drawStringWithShadow(label.shortName, -((float) width / 2), height, label.shortNameColor);
        fontRenderer.drawStringWithShadow(distance, -((float) width / 2) + width, height, distanceColor);
        if (nearCenter) {
            for (CHChestItem item : label.expandedName) {
                height += 10;
                width = fontRenderer.getStringWidth(item.count + " " + item.name);
                int countWidth = fontRenderer.getStringWidth(item.count + " ");
                fontRenderer.drawStringWithShadow(item.count + " ", ((float) -width / 2), height, item.numberColor);
                fontRenderer.drawStringWithShadow(item.name,  ((float) -width / 2) + countWidth, height, item.itemColor);
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

    }

    public static Matrix4f perspective(float fovY, float aspectRatio, float near, float far) {
        Matrix4f matrix = new Matrix4f();
        float f = 1.0f / (float) Math.tan(Math.toRadians(fovY / 2.0f));

        matrix.m00 = f / aspectRatio;
        matrix.m11 = f;
        matrix.m22 = (far + near) / (near - far);
        matrix.m23 = -1.0f;
        matrix.m32 = (2.0f * far * near) / (near - far);

        return matrix;
    }

}

