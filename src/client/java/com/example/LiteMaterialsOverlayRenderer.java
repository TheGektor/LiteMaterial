package com.example;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

public final class LiteMaterialsOverlayRenderer {
    private LiteMaterialsOverlayRenderer() {}

    public static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        Map<Identifier, Integer> selections = SelectionController.getSelections();
        if (selections.isEmpty()) return; // do not render empty box
        Set<Identifier> satisfied = SelectionController.computeSatisfied(client.player);

        int x = 4;
        int y = 4;
        for (Map.Entry<Identifier, Integer> e : selections.entrySet()) {
            Identifier blockId = e.getKey();
            int required = e.getValue();
            boolean hasAll = satisfied.contains(blockId);

            int width = 160;
            int height = 18;

            int bg = 0x88000000; // translucent bg
            int border = hasAll ? 0xFF00FF00 : 0xFFFFFFFF; // green or white

            // background
            ctx.fill(x, y, x + width, y + height, bg);
            // border
            drawBorder(ctx, x, y, width, height, border);

            String display = Registries.BLOCK.get(blockId).getName().getString();
            ctx.drawText(client.textRenderer, Text.literal(display + " x" + required), x + 4, y + 5, 0xFFFFFF, false);

            y += height + 3;
        }
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x - 1, y - 1, x + w + 1, y, color);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        ctx.fill(x - 1, y, x, y + h, color);
        ctx.fill(x + w, y, x + w + 1, y + h, color);
    }
}