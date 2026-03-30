package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.kryo.KryoNetwork.CHChestItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CHWaypoints {

    public int x;
    public int y;
    public int z;

    public BlockPos pos;

    public String shortName = "Crystal Hollows";
    public int shortNameColor = 0xAA00AA;

    public String id;

    public ArrayList<CHChestItem> expandedName;
    public CopyOnWriteArrayList<CHChestItem> filteredExpandedItems = new CopyOnWriteArrayList<>();

    public static HashMap<String, CrystalHollowsItemTotal> itemCounts = new HashMap<>();
    public static CopyOnWriteArrayList<CHWaypoints> filteredWaypoints = new CopyOnWriteArrayList<>();

    public static void initRendering() {
        WorldRenderEvents.AFTER_ENTITIES.register(CHWaypoints::renderAll);
    }

    private static void renderAll(WorldRenderContext context) {
        if (filteredWaypoints.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        Vec3 camPos = camera.getPosition();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        PoseStack poseStack = new PoseStack();
        float tickDelta = net.minecraft.client.Minecraft.getInstance().getFrameTimeNs();
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix(tickDelta);
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        for (CHWaypoints wp : filteredWaypoints) {
            wp.render(poseStack, buffer, font, camera, camPos, proj, screenWidth, screenHeight, centerX, centerY);
        }

        buffer.endBatch();
    }

    private void render(PoseStack poseStack, MultiBufferSource buffer, Font font, Camera camera, Vec3 camPos, Matrix4f proj, int screenWidth, int screenHeight, float centerX, float centerY) {
        double waypointX = x + 0.5;
        double waypointY = y;
        double waypointZ = z + 0.5;

        double dx = waypointX - camPos.x;
        double dy = waypointY - camPos.y;
        double dz = waypointZ - camPos.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Vector3f toWaypoint = new Vec3(dx, dy, dz).normalize().toVector3f();
        Vector3f look = camera.getLookVector();

        boolean nearCenter = toWaypoint.dot(look) > 0.99;

        int distColor;
        if (dist > 300) distColor = 0xFF5555;
        else if (dist > 100) distColor = 0xFFFF55;
        else distColor = 0x55FF55;

        String distStr = " (" + (int) dist + "m)";

        double rx, ry, rz;

        if (dist > 30) {
            double ratio = 30.0 / dist;
            rx = dx * ratio + 0.5;
            ry = dy * ratio + camera.getEntity().getEyeHeight();
            rz = dz * ratio + 0.5;
            dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
        } else {
            rx = dx + 0.5;
            ry = dy + camera.getEntity().getEyeHeight();
            rz = dz + 0.5;
        }

        double scale = (dist * 0.0266666688f) / 10.0;
        if (scale < 0.0266666688f) scale = 0.0266666688f;

        String full = shortName + distStr;

        int nameWidth = font.width(shortName);
        int totalWidth = font.width(full);

        poseStack.pushPose();
        poseStack.translate(rx, ry, rz);

        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-camera.getYRot())).rotateX((float) Math.toRadians(camera.getXRot())));

        float s = (float) scale;
        poseStack.scale(-s, -s, s);

        Matrix4f pose = poseStack.last().pose();
        int yOff = 0;

        font.drawInBatch(shortName, -(totalWidth / 2f), yOff, shortNameColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        font.drawInBatch(distStr, -(totalWidth / 2f) + nameWidth, yOff, distColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        if (nearCenter) {
            for (CHChestItem item : filteredExpandedItems) {
                yOff += 10;

                String countStr = item.count + " ";
                String line = countStr + item.name;

                int lineWidth = font.width(line);
                int countWidth = font.width(countStr);

                font.drawInBatch(countStr, -(lineWidth / 2f), yOff, item.numberColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

                font.drawInBatch(item.name, -(lineWidth / 2f) + countWidth, yOff, item.itemColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            }
        }

        poseStack.popPose();
    }

    public CHWaypoints(int x, int y, int z, ArrayList<CHChestItem> chest) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.pos = new BlockPos(x, y, z);
        this.id = "" + x + y + z;
        this.expandedName = chest;
        this.filteredExpandedItems.addAll(chest);

        for (CHChestItem item : chest) {
            System.out.println(item);
            if (item.name != null && item.name.toLowerCase().contains("jasper")) {
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
        }
    }
}
