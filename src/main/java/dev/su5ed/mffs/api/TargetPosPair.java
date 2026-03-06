package dev.su5ed.mffs.api;

// 1.12.2 Backport: TargetPosPair
// Vec3 (1.21.x) -> Vec3d (1.12.2); BlockPos namespace change.
// Jabel supports record syntax -> compiles fine with use_modern_java_syntax=true.

import com.github.bsideup.jabel.Desugar;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Desugar
public record TargetPosPair(BlockPos pos, Vec3d original) {}
