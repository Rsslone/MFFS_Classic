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

        int inverseThickness = 8;

        for (float y = 0; y <= yStretch; y += 1f) {
            for (float x = -xStretch; x <= xStretch; x += 1f) {
                for (float z = -zStretch; z <= zStretch; z += 1f) {
                    double yTest = y / yStretch * inverseThickness;
                    double xzPositivePlane = (1 - x / xStretch - z / zStretch) * inverseThickness;
                    double xzNegativePlane = (1 + x / xStretch - z / zStretch) * inverseThickness;

                    // Positive Positive Plane
                    if (x >= 0 && z >= 0 && Math.round(xzPositivePlane) == Math.round(yTest)) {
                        fieldBlocks.add(new Vec3d(x, y, z).add(translation));
                        fieldBlocks.add(new Vec3d(x, y, -z).add(translation));
                    }

                    // Negative Positive Plane
                    if (x <= 0 && z >= 0 && Math.round(xzNegativePlane) == Math.round(yTest)) {
                        fieldBlocks.add(new Vec3d(x, y, -z).add(translation));
                        fieldBlocks.add(new Vec3d(x, y, z).add(translation));
                    }

                    // Ground Level Plane
                    if (y == 0 && Math.abs(x) + Math.abs(z) < (xStretch + yStretch) / 2.0) {
                        fieldBlocks.add(new Vec3d(x, y, z).add(translation));
                    }
                }
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
            && 1 - Math.abs(relativeRotated.x) / xStretch - Math.abs(relativeRotated.z) / zStretch > relativeRotated.y / yStretch;
    }

    private static boolean isIn(Vec3d min, Vec3d max, Vec3d vec) {
        return vec.x > min.x && vec.x < max.x
            && vec.y > min.y && vec.y < max.y
            && vec.z > min.z && vec.z < max.z;
    }
}
