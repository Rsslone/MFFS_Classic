package dev.su5ed.mffs.util.projector;

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.network.SetStructureShapePacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persists custom projector mode structures via WorldSavedData.
 * In 1.21 this uses VoxelShape + Codec; in 1.12.2 we use Set&lt;BlockPos&gt; + NBT.
 */
public class CustomStructureSavedData extends WorldSavedData {
    public static final String NAME = MFFSMod.MODID + "_custom_structures";

    private final Map<String, Structure> structures = new HashMap<>();

    public CustomStructureSavedData() {
        super(NAME);
    }

    public CustomStructureSavedData(String name) {
        super(name);
    }

    @Nullable
    public Structure get(String id) {
        return this.structures.get(id);
    }

    public void clear(World world, EntityPlayerMP player, String id) {
        this.structures.remove(id);
        markDirty();
        sendToClient(world.provider.getDimension(), id, null, player);
    }

    private Structure getOrCreate(String id) {
        return this.structures.computeIfAbsent(id, s -> new Structure());
    }

    public void join(String id, World world, EntityPlayerMP player, BlockPos min, BlockPos max, boolean add) {
        Structure structure = getOrCreate(id);

        // Calculate the area bounds
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        // Add or remove block positions and record their states
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (add) {
                        structure.shape.add(pos);
                        IBlockState state = world.getBlockState(pos);
                        if (!state.getBlock().isAir(state, world, pos)) {
                            structure.blocks.put(pos, state);
                        }
                    } else {
                        structure.shape.remove(pos);
                        structure.blocks.remove(pos);
                    }
                }
            }
        }
        structure.invalidateCache();
        markDirty();
        sendToClient(world.provider.getDimension(), id, structure.shape, player);
    }

    public void remove(String id, BlockPos pos) {
        Structure structure = get(id);
        if (structure != null) {
            structure.blocks.remove(pos);
            structure.shape.remove(pos);
            structure.invalidateCache();
            markDirty();
        }
    }

    private static void sendToClient(int dimension, String id, @Nullable Set<BlockPos> shape, EntityPlayerMP player) {
        SetStructureShapePacket packet = new SetStructureShapePacket(dimension, id, shape != null ? shape : Collections.emptySet());
        Network.sendTo(packet, player);
    }

    // -----------------------------------------------------------------------
    // WorldSavedData NBT serialization
    // -----------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.structures.clear();
        NBTTagCompound structuresTag = nbt.getCompoundTag("structures");
        for (String key : structuresTag.getKeySet()) {
            NBTTagCompound structTag = structuresTag.getCompoundTag(key);
            Structure structure = new Structure();

            // Read shape positions
            NBTTagList shapeList = structTag.getTagList("shape", Constants.NBT.TAG_LONG);
            for (NBTBase tag : shapeList) {
                if (tag instanceof net.minecraft.nbt.NBTTagLong longTag) {
                    structure.shape.add(BlockPos.fromLong(longTag.getLong()));
                }
            }

            // Read blocks
            NBTTagList blocksList = structTag.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < blocksList.tagCount(); i++) {
                NBTTagCompound blockTag = blocksList.getCompoundTagAt(i);
                BlockPos pos = BlockPos.fromLong(blockTag.getLong("pos"));
                Block block = Block.REGISTRY.getObject(new ResourceLocation(blockTag.getString("block")));
                if (block != null) {
                    int meta = blockTag.getInteger("meta");
                    structure.blocks.put(pos, block.getStateFromMeta(meta));
                }
            }

            this.structures.put(key, structure);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound structuresTag = new NBTTagCompound();
        for (Map.Entry<String, Structure> entry : this.structures.entrySet()) {
            NBTTagCompound structTag = new NBTTagCompound();
            Structure structure = entry.getValue();

            // Write shape positions
            NBTTagList shapeList = new NBTTagList();
            for (BlockPos pos : structure.shape) {
                shapeList.appendTag(new net.minecraft.nbt.NBTTagLong(pos.toLong()));
            }
            structTag.setTag("shape", shapeList);

            // Write blocks
            NBTTagList blocksList = new NBTTagList();
            for (Map.Entry<BlockPos, IBlockState> blockEntry : structure.blocks.entrySet()) {
                NBTTagCompound blockTag = new NBTTagCompound();
                blockTag.setLong("pos", blockEntry.getKey().toLong());
                ResourceLocation regName = Block.REGISTRY.getNameForObject(blockEntry.getValue().getBlock());
                if (regName != null) {
                    blockTag.setString("block", regName.toString());
                    blockTag.setInteger("meta", blockEntry.getValue().getBlock().getMetaFromState(blockEntry.getValue()));
                    blocksList.appendTag(blockTag);
                }
            }
            structTag.setTag("blocks", blocksList);

            structuresTag.setTag(entry.getKey(), structTag);
        }
        nbt.setTag("structures", structuresTag);
        return nbt;
    }

    // -----------------------------------------------------------------------
    // Structure inner class
    // -----------------------------------------------------------------------

    public static class Structure {
        final Set<BlockPos> shape = new HashSet<>();
        final Map<BlockPos, IBlockState> blocks = new HashMap<>();
        @Nullable
        private Map<BlockPos, IBlockState> relativeBlocks;
        @Nullable
        private Map<Vec3d, IBlockState> realBlocks;

        public Set<BlockPos> shape() {
            return shape;
        }

        public Map<Vec3d, IBlockState> getRealBlocks() {
            if (this.realBlocks == null) {
                Map<Vec3d, IBlockState> map = new HashMap<>();
                for (Map.Entry<BlockPos, IBlockState> entry : getRelativeBlocks().entrySet()) {
                    map.put(new Vec3d(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()), entry.getValue());
                }
                this.realBlocks = map;
            }
            return this.realBlocks;
        }

        public Map<BlockPos, IBlockState> getRelativeBlocks() {
            if (this.relativeBlocks == null) {
                this.relativeBlocks = computeRelativeBlocks();
            }
            return this.relativeBlocks;
        }

        void invalidateCache() {
            this.relativeBlocks = null;
            this.realBlocks = null;
        }

        private Map<BlockPos, IBlockState> computeRelativeBlocks() {
            if (this.shape.isEmpty()) return Collections.emptyMap();

            // Find median of shape positions
            double sumX = 0, sumY = 0, sumZ = 0;
            for (BlockPos pos : this.shape) {
                sumX += pos.getX();
                sumY += pos.getY();
                sumZ += pos.getZ();
            }
            int size = this.shape.size();
            BlockPos median = new BlockPos(
                Math.round(sumX / size),
                Math.round(sumY / size),
                Math.round(sumZ / size)
            );

            Map<BlockPos, IBlockState> map = new HashMap<>();
            for (Map.Entry<BlockPos, IBlockState> entry : this.blocks.entrySet()) {
                map.put(entry.getKey().subtract(median), entry.getValue());
            }
            return map;
        }
    }
}
