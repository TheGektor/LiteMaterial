package com.example;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks user selections and material requirements.
 * Integrates with Litematica (if present) to pull required counts from its Material List.
 */
public final class SelectionController {
    // Block registry id -> required count
    private static final Map<Identifier, Integer> blockIdToRequiredCount = new HashMap<>();
    private static final Set<Identifier> lowStockAnnounced = new HashSet<>();
    private static final int LOW_STOCK_THRESHOLD = 10;

    private SelectionController() {}

    public static void onShiftMiddleClick(MinecraftClient client) {
        if (client.crosshairTarget == null || client.player == null) return;
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult bhr = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = bhr.getBlockPos();
        Block block = client.world.getBlockState(pos).getBlock();
        Identifier id = Objects.requireNonNull(Registries.BLOCK.getId(block));
        toggleByBlockId(client, id);
    }

    public static void toggleByBlockId(MinecraftClient client, Identifier id) {
        if (blockIdToRequiredCount.containsKey(id)) {
            blockIdToRequiredCount.remove(id);
            lowStockAnnounced.remove(id);
            ExampleMod.LOGGER.info("Removed from list: {}", id);
            return;
        }
        int defaultRequired = 1;
        int required = LitematicaBridge.getRequiredCountForHoveredBlock(client, id).orElse(defaultRequired);
        blockIdToRequiredCount.put(id, required);
        ExampleMod.LOGGER.info("Added to list: {} x{}", id, required);
    }

    public static void onTick(MinecraftClient client) {
        if (client.player == null) return;
        for (Identifier id : blockIdToRequiredCount.keySet()) {
            int have = countItemsInInventory(client.player, id);
            boolean low = have <= LOW_STOCK_THRESHOLD;
            boolean announced = lowStockAnnounced.contains(id);
            if (low && !announced) {
                String name = Registries.BLOCK.get(id).getName().getString();
                client.inGameHud.getChatHud().addMessage(Text.literal("[LiteMaterials] Мало блока: " + name + " (" + have + ")"));
                lowStockAnnounced.add(id);
            } else if (!low && announced) {
                lowStockAnnounced.remove(id);
            }
        }
    }

    public static Map<Identifier, Integer> getSelections() {
        return blockIdToRequiredCount;
    }

    public static Set<Identifier> computeSatisfied(ClientPlayerEntity player) {
        Set<Identifier> satisfied = new HashSet<>();
        for (Map.Entry<Identifier, Integer> e : blockIdToRequiredCount.entrySet()) {
            Identifier blockId = e.getKey();
            int required = e.getValue();
            int have = countItemsInInventory(player, blockId);
            if (have >= required) {
                satisfied.add(blockId);
            }
        }
        return satisfied;
    }

    private static int countItemsInInventory(ClientPlayerEntity player, Identifier blockId) {
        int total = 0;
        Block block = Registries.BLOCK.get(blockId);
        Item blockItem = block.asItem();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            if (itemId.equals(blockId) || (blockItem != null && stack.getItem() == blockItem)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}