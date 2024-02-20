package com.github.indigopolecat.bingobrewers;

import cc.polyfrost.oneconfig.renderer.font.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

public class Fonts {

        public static final Font BOLD = new Font("inter-bold", "/assets/oneconfig/font/Bold.otf");
        public static final Font SEMIBOLD = new Font("inter-semibold", "/assets/oneconfig/font/SemiBold.otf");
        public static final Font MEDIUM = new Font("inter-medium", "/assets/oneconfig/font/Medium.otf");
        public static final Font REGULAR = new Font("inter-regular", "/assets/oneconfig/font/Regular.otf");
        public static final Font MINECRAFT_REGULAR = new Font("mc-regular", "/assets/oneconfig/font/Minecraft-Regular.otf");
        public static final Font MINECRAFT_BOLD = new Font("mc-bold", "/assets/oneconfig/font/Minecraft-Bold.otf");
        // doesn't work
        public static final FontRenderer fontRendererBold = new FontRenderer(Minecraft.getMinecraft().gameSettings, new ResourceLocation("bingobrewers","fonts/BoldMinecraft.otf"), Minecraft.getMinecraft().renderEngine, false);


}
