package io.github.mermagudyan.idlecraft.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StructureBreakBlocker {

    private StructureBreakBlocker() {
    }

    private static final Set<BlockPos> PROTECTED = new HashSet<>();
    private static final List<BoundingBox> REGIONS = new ArrayList<>();

    public static void markProtected(BlockPos pos) {
        PROTECTED.add(pos);
    }

    public static void addRegions(List<BoundingBox> boxes) {
        for (BoundingBox box : boxes) {
            if (!REGIONS.contains(box)) {
                REGIONS.add(box);
            }
        }
    }

    public static boolean isProtected(BlockPos pos) {
        if (PROTECTED.contains(pos)) {
            return true;
        }
        for (BoundingBox box : REGIONS) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
