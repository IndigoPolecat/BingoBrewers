plugins {
    id("dev.kikugie.stonecutter")
}

// Version used when the workspace is imported / when running a single-target IDE sync.
stonecutter active "26.3"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    // Minecraft and Fabric API renamed several classes when 26.1 dropped obfuscation.
    // These are plain 1:1 renames, so a text substitution is enough - no branching needed.
    replacements {
        string(current.parsed >= "26.1") {
            // GUI rendering was rewritten around a render-state extraction model.
            replace("import net.minecraft.client.gui.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;")
            replace("GuiGraphics graphics", "GuiGraphicsExtractor graphics")
            replace("public void render(GuiGraphics guiGraphics, int i, int j, float f) {", "public void extractRenderState(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {")
            replace("super.render(guiGraphics, i, j, f);", "super.extractRenderState(guiGraphics, i, j, f);")
            replace(".drawString(", ".text(")

            // Fabric API's client command entrypoint was renamed.
            replace("import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;", "import net.fabricmc.fabric.api.client.command.v2.ClientCommands;")
            replace("ClientCommandManager.literal(", "ClientCommands.literal(")

            // Fabric API renamed its World* events to Level* to match Minecraft's own terminology.
            replace("import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;", "import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;")
            replace("ServerWorldEvents.LOAD.register(", "ServerLevelEvents.LOAD.register(")
            replace("import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;", "import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;")

            // The world render event hooks moved from the .world to the .level package and were restructured
            // around the same render-state extraction model. COLLECT_SUBMITS is the documented hook for
            // adding to LevelRenderContext#submitNodeCollector() (fires once per frame at the end of
            // submitFeatures(), not per rendered chunk section like AFTER_TRANSLUCENT_FEATURES, which would
            // silently drop waypoints whose section falls outside the current frustum).
            replace("import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;", "import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;")
            replace("import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;", "import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;")
            replace("WorldRenderEvents.AFTER_ENTITIES.register(CHWaypoints::renderAll);", "LevelRenderEvents.COLLECT_SUBMITS.register(CHWaypoints::renderAll);")
            replace("private static void renderAll(WorldRenderContext context) {", "private static void renderAll(LevelRenderContext context) {")

            // Player.displayClientMessage(Component, boolean) was split into two dedicated methods.
            replace(
                ".displayClientMessage(Component.literal(\"Bingo Brewers has been updated to the latest version! Please restart your game to apply the update.\"), true);",
                ".sendOverlayMessage(Component.literal(\"Bingo Brewers has been updated to the latest version! Please restart your game to apply the update.\"));"
            )
            replace(
                ".displayClientMessage(Component.literal(\"Bingo Brewers is up to date!\").withColor(0x00FF00), false);",
                ".sendSystemMessage(Component.literal(\"Bingo Brewers is up to date!\").withColor(0x00FF00));"
            )
        }

        // 26.2 removed the old immediate-mode MultiBufferSource renderer alongside the OpenGL backend;
        // these are further 1:1 renames on top of the 26.1 set above.
        // 26.3 split quaternion rotation off mulPose(Matrix4fc) into rotate(Quaternionfc).
        string(current.parsed < "26.3") {
            replace("poseStack.rotate(new Quaternionf()", "poseStack.mulPose(new Quaternionf()")
        }

        string(current.parsed >= "26.2") {
            replace(".setScreen(", ".setScreenAndShow(")
            replace("mc.gameRenderer.getMainCamera();", "mc.gameRenderer.mainCamera();")

            // Minecraft no longer exposes the current Screen as a readable field; these were debug-only.
            replace(
                "Log.LOG.debug(\"parent screen={}\", Minecraft.getInstance().screen);",
                "Log.LOG.debug(\"parent screen=?\");"
            )
            replace(
                "Log.LOG.debug(\"configScreen present={}, current screen={}\", configScreen != null, Minecraft.getInstance().screen);",
                "Log.LOG.debug(\"configScreen present={}, current screen=?\", configScreen != null);"
            )
        }
    }
}
