package dev.su5ed.mffs.util.projector;

import dev.su5ed.mffs.api.Projector;
import dev.su5ed.mffs.api.module.ProjectorMode;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public final class PyramidProjectorMode implements ProjectorMode {
    PyramidProjectorMode() {}

    @Override
    public Set<Vec3d> getExteriorPoints(Projector projector) {
        Set<Vec3d> fieldBlocks = new HashSet<>();

        BlockPos posScale = projector.getPositiveScale();
        BlockPos negScale = projector.getNegativeScale();

        int xStretch = posScale.getX() + negScale.getX();
        int yStretch = posScale.getY() + negScale.getY();
        int zStretch = posScale.getZ() + negScale.getZ();
        Vec3d translation = new Vec3d(0, -negScale.getY(), 0);

        // Dense 0.5 sampling prevents holes after rotation, while generating only
        // the perimeter of each Y slice keeps the shell one block thick.
        if (xStretch <= 0 || yStretch <= 0 || zStretch <= 0) {
            fieldBlocks.add(translation);
            return fieldBlocks;
        }

        for (float y = 0; y <= yStretch; y += 0.5f) {
            double t = 1.0 - (double) y / yStretch;
            double xLimit = xStretch * t;
            double zLimit = zStretch * t;

            for (double z = -zLimit; z <= zLimit; z += 0.5) {
                fieldBlocks.add(new Vec3d(xLimit, y, z).add(translation));
                fieldBlocks.add(new Vec3d(-xLimit, y, z).add(translation));
            }

            for (double x = -xLimit; x <= xLimit; x += 0.5) {
                fieldBlocks.add(new Vec3d(x, y, zLimit).add(translation));
                fieldBlocks.add(new Vec3d(x, y, -zLimit).add(translation));
            }
        }

        for (double x = -xStretch; x <= xStretch; x += 0.5) {
            for (double z = -zStretch; z <= zStretch; z += 0.5) {
                fieldBlocks.add(new Vec3d(x, 0, z).add(translation));
            }
        }

        return fieldBlocks;
    }

    @Override
    public Set<Vec3d> getInteriorPoints(Projector projector) {
        Set<Vec3d> fieldBlocks = new HashSet<>();

        BlockPos posScale = projector.getPositiveScale();
        BlockPos negScale = projector.getNegativeScale();
        BlockPos projectorPos = projector.be().getPos();

        int xStretch = posScale.getX() + negScale.getX();
        int yStretch = posScale.getY() + negScale.getY();
        int zStretch = posScale.getZ() + negScale.getZ();
        Vec3d translation = new Vec3d(0, -0.4, 0);

        for (float x = -xStretch; x <= xStretch; x++) {
            for (float z = -zStretch; z <= zStretch; z++) {
                for (float y = 0; y <= yStretch; y++) {
                    Vec3d position = new Vec3d(x, y, z).add(translation);

                    if (isInField(projector, position.add(new Vec3d(projectorPos.getX(), projectorPos.getY(), projectorPos.getZ())))) {
                        fieldBlocks.add(position);
                    }
                }
            }
        }

        return fieldBlocks;
    }

    @Override
    public boolean isInField(Projector projector, Vec3d position) {
        BlockPos posScale = projector.getPositiveScale();
        BlockPos negScale = projector.getNegativeScale();

        int xStretch = posScale.getX() + negScale.getX();
        int yStretch = posScale.getY() + negScale.getY();
        int zStretch = posScale.getZ() + negScale.getZ();

        BlockPos projectorPos = projector.be().getPos()
            .add(projector.getTranslation())
            .add(0, -negScale.getY(), 0);

        Vec3d relativePosition = position.subtract(projectorPos.getX(), projectorPos.getY(), projectorPos.getZ());
        Vec3d relativeRotated = ModUtil.rotateByAngleExact(relativePosition, -projector.getRotationYaw(), -projector.getRotationPitch(), 0);

        // Replicate Vec3.atLowerCornerOf
        Vec3d min = new Vec3d(-negScale.getX(), -negScale.getY(), -negScale.getZ());
        Vec3d max = new Vec3d(posScale.getX(), posScale.getY(), posScale.getZ());

        return isIn(min, max, relativeRotated) && relativeRotated.y > 0
            && Math.max(Math.abs(relativeRotated.x) / xStretch, Math.abs(relativeRotated.z) / zStretch) < 1.0 - relativeRotated.y / yStretch;
    }

    private static boolean isIn(Vec3d min, Vec3d max, Vec3d vec) {
        return vec.x > min.x && vec.x < max.x
            && vec.y > min.y && vec.y < max.y
            && vec.z > min.z && vec.z < max.z;
    }
}
