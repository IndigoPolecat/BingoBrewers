package com.github.indigopolecat.bingobrewers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
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
    public static ArrayList<CHWaypoints> waypoints = new ArrayList<>();
    public BlockPos pos;
    public String shortName;
    public HashMap<String, Integer> expandedName;

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        // Render the waypoints
        renderPointLabel("test", new BlockPos(244, 140, 811), event.partialTicks);

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

        double scale = (dist * 0.0366666688F) / 10;
        if (scale < 0.0366666688F) {
            scale = 0.0366666688F;
        }

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

        if (nearCenter) {
            label = "looking at (" + (int) dist + "M)";
        }

        int width = fontRenderer.getStringWidth(label) / 2;


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

