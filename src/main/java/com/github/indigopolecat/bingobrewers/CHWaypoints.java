package com.github.indigopolecat.bingobrewers;

import com.github.indigopolecat.bingobrewers.util.CrystalHollowsItemTotal;
import com.github.indigopolecat.kryo.KryoNetwork;
import com.github.indigopolecat.kryo.KryoNetwork.CHChestItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.FormattedCharSequence;
//?}
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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
        LevelRenderEvents.COLLECT_SUBMITS.register(CHWaypoints::renderAll);
    }

    private static void renderAll(LevelRenderContext context) {
        if (filteredWaypoints.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.mainCamera();

        Vec3 camPos = camera.position();

        Font font = mc.font;

        //? if <26.2 {
        /*PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (CHWaypoints wp : filteredWaypoints) {
            wp.render(poseStack, buffer, font, camera, camPos);
        }

        buffer.endBatch();
        *///?} else {
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        for (CHWaypoints wp : filteredWaypoints) {
            wp.render(poseStack, collector, font, camera, camPos);
        }
        //?}
    }

    //? if <26.2 {
    /*private void render(PoseStack poseStack, MultiBufferSource buffer, Font font, Camera camera, Vec3 camPos) {
    *///?} else {
    private void render(PoseStack poseStack, SubmitNodeCollector buffer, Font font, Camera camera, Vec3 camPos) {
    //?}
        double waypointX = x + 0.5;
        double waypointY = y;
        double waypointZ = z + 0.5;

        double dx = waypointX - camPos.x;
        double dy = waypointY - camPos.y;
        double dz = waypointZ - camPos.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Vector3f toWaypoint = new Vec3(dx, dy + 1, dz).normalize().toVector3f();
        Vector3fc look = camera.forwardVector();

        boolean nearCenter = toWaypoint.dot(look) > 0.99;

        int distColor;
        if (dist > 300) distColor = 0xFF5555;
        else if (dist > 100) distColor = 0xFFFF55;
        else distColor = 0x55FF55;

        String distStr = " (" + (int) dist + "m)";

        double rx, ry, rz;

        if (dist > 30) {
            double ratio = 30.0 / dist;
            rx = dx * ratio;
            ry = dy * ratio + camera.entity().getEyeHeight();
            rz = dz * ratio;
            dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
        } else {
            rx = dx;
            ry = dy + camera.entity().getEyeHeight();
            rz = dz;
        }

        double scale = (dist * 0.0266666688f) / 10.0;
        if (scale < 0.0266666688f) scale = 0.0266666688f;

        String full = shortName + distStr;

        int nameWidth = font.width(shortName);
        int totalWidth = font.width(full);

        poseStack.pushPose();
        poseStack.translate(rx, ry, rz);

        poseStack.rotate(new Quaternionf().rotateY((float) Math.toRadians(-camera.yRot())).rotateX((float) Math.toRadians(camera.xRot())));

        float s = (float) scale;
        poseStack.scale(-s, -s, s);

        Matrix4f pose = poseStack.last().pose();
        int yOff = 0;

        //? if <26.2 {
        /*font.drawInBatch(shortName, -(totalWidth / 2f), yOff, shortNameColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        font.drawInBatch(distStr, -(totalWidth / 2f) + nameWidth, yOff, distColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        *///?} else {
        buffer.submitText(poseStack, -(totalWidth / 2f), yOff, FormattedCharSequence.forward(shortName, net.minecraft.network.chat.Style.EMPTY), true, Font.DisplayMode.SEE_THROUGH, 0xF000F0, shortNameColor | 0xFF000000, 0, 0);

        buffer.submitText(poseStack, -(totalWidth / 2f) + nameWidth, yOff, FormattedCharSequence.forward(distStr, net.minecraft.network.chat.Style.EMPTY), true, Font.DisplayMode.SEE_THROUGH, 0xF000F0, distColor | 0xFF000000, 0, 0);
        //?}

        if (nearCenter) {
            for (CHChestItem item : filteredExpandedItems) {
                yOff += 10;

                String countStr = item.count + " ";
                String line = countStr + item.name;

                int lineWidth = font.width(line);
                int countWidth = font.width(countStr);

                //? if <26.2 {
                /*font.drawInBatch(countStr, -(lineWidth / 2f), yOff, item.numberColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

                font.drawInBatch(item.name, -(lineWidth / 2f) + countWidth, yOff, item.itemColor | 0xFF000000, true, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                *///?} else {
                buffer.submitText(poseStack, -(lineWidth / 2f), yOff, FormattedCharSequence.forward(countStr, net.minecraft.network.chat.Style.EMPTY), true, Font.DisplayMode.SEE_THROUGH, 0xF000F0, item.numberColor | 0xFF000000, 0, 0);

                buffer.submitText(poseStack, -(lineWidth / 2f) + countWidth, yOff, FormattedCharSequence.forward(item.name, net.minecraft.network.chat.Style.EMPTY), true, Font.DisplayMode.SEE_THROUGH, 0xF000F0, item.itemColor | 0xFF000000, 0, 0);
                //?}
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
        filteredExpandedItems.addAll(chest);

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
