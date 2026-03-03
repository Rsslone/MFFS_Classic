package dev.su5ed.mffs.render;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client-side cache for custom projector mode structure shapes.
 * Shapes are stored per dimension + structure ID and updated via SetStructureShapePacket.
 */
public final class CustomProjectorModeClientHandler {
    // Map: dimension ID → (structure ID → shape positions)
    private static final Map<Integer, Map<String, Set<BlockPos>>> STRUCTURE_SHAPES = new HashMap<>();

    private CustomProjectorModeClientHandler() {}

    /** Called by SetStructureShapePacket to update client-side shape data. */
    public static void setShape(int dimension, String structId, @Nullable Set<BlockPos> shape) {
        Map<String, Set<BlockPos>> map = STRUCTURE_SHAPES.computeIfAbsent(dimension, d -> new HashMap<>());
        if (shape != null) {
            map.put(structId, shape);
        } else {
            map.remove(structId);
        }
    }

    /** Get a cached shape, or null if not yet received from server. */
    @Nullable
    public static Set<BlockPos> getShape(int dimension, String structId) {
        Map<String, Set<BlockPos>> map = STRUCTURE_SHAPES.get(dimension);
        return map != null ? map.get(structId) : null;
    }
}
