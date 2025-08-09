package com.example;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ClientInitBootstrap {
    private static KeyBinding togglePickBinding;

    private ClientInitBootstrap() {}

    public static void initialize() {
        togglePickBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.litematerials.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.litematerials"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ClientInitBootstrap::onClientTick);
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> LiteMaterialsOverlayRenderer.render(matrices));
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        boolean shiftDown = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean middleDown = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        if (MouseEdgeTracker.consumeEdge(shiftDown && middleDown)) {
            // Prefer Litematica hovered schematic block, else fallback to vanilla crosshair selection
            Identifier schematicId = LitematicaBridge.getHoveredSchematicBlock(client).orElse(null);
            if (schematicId != null) {
                SelectionController.toggleByBlockId(client, schematicId);
            } else {
                SelectionController.onShiftMiddleClick(client);
            }
        }

        SelectionController.onTick(client);
    }
}