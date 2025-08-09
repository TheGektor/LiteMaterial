package com.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

/**
 * Best-effort integration with Litematica via reflection.
 * If the mod is not present or APIs change, this class returns empty.
 */
public final class LitematicaBridge {
    private static final boolean LITEMATICA_PRESENT;

    static {
        boolean present;
        try {
            Class.forName("fi.dy.masa.litematica.Litematica");
            present = true;
        } catch (Throwable t) {
            present = false;
        }
        LITEMATICA_PRESENT = present;
    }

    private LitematicaBridge() {}

    public static boolean isPresent() {
        return LITEMATICA_PRESENT;
    }

    /**
     * Attempts to retrieve hovered schematic block Identifier.
     */
    public static Optional<Identifier> getHoveredSchematicBlock(MinecraftClient client) {
        if (!LITEMATICA_PRESENT) return Optional.empty();
        try {
            // Common access path in Litematica: SchematicPlacementManager -> getHoveredBlock
            Class<?> spmCls = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager");
            Object mgr = MethodHandles.lookup()
                    .findStatic(spmCls, "getInstance", MethodType.methodType(spmCls))
                    .invoke();
            Object hovered = MethodHandles.lookup()
                    .findVirtual(spmCls, "getHoveredBlockId", MethodType.methodType(String.class))
                    .invoke(mgr);
            if (hovered instanceof String s && !s.isEmpty()) {
                return Optional.of(Identifier.of(s));
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    /**
     * Attempts to query Litematica for the required count from the currently active material list
     * for the given block id. Returns empty if not available.
     */
    public static Optional<Integer> getRequiredCountForHoveredBlock(MinecraftClient client, Identifier blockId) {
        if (!LITEMATICA_PRESENT) return Optional.empty();
        try {
            // NOTE: The following reflection targets may change between Litematica versions.
            // We try a couple of common places.
            // Attempt 1: fi.dy.masa.litematica.materials.MaterialList#getTotals()
            Class<?> materialListCls = Class.forName("fi.dy.masa.litematica.materials.MaterialList");
            Object listInstance = MethodHandles.lookup()
                    .findStatic(materialListCls, "getInstance", MethodType.methodType(materialListCls))
                    .invoke();
            Object totals = MethodHandles.lookup()
                    .findVirtual(materialListCls, "getTotals", MethodType.methodType(java.util.Map.class))
                    .invoke(listInstance);
            if (totals instanceof java.util.Map<?, ?> map) {
                Object key = blockId.toString();
                Object value = map.get(key);
                if (value instanceof Number n) {
                    return Optional.of(n.intValue());
                }
            }
        } catch (Throwable ignored) {
            // Fall through to attempt 2
        }
        try {
            // Attempt 2: fi.dy.masa.litematica.materials.MaterialCache#getRequiredForBlock(Identifier)
            Class<?> cacheCls = Class.forName("fi.dy.masa.litematica.materials.MaterialCache");
            Object cacheInstance = MethodHandles.lookup()
                    .findStatic(cacheCls, "getInstance", MethodType.methodType(cacheCls))
                    .invoke();
            Object count = MethodHandles.lookup()
                    .findVirtual(cacheCls, "getRequiredForBlock", MethodType.methodType(int.class, String.class))
                    .invoke(cacheInstance, blockId.toString());
            if (count instanceof Integer i) {
                return Optional.of(i);
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }
}