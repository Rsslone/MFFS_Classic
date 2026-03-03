package dev.su5ed.mffs.blockentity;

import dev.su5ed.mffs.api.Projector;
import dev.su5ed.mffs.block.ForceFieldBlockImpl;
import dev.su5ed.mffs.network.InitialDataRequestPacket;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.setup.ModCapabilities;
import dev.su5ed.mffs.setup.ModModules;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class ForceFieldBlockEntity extends BaseBlockEntity {

    private BlockPos projector;
    private IBlockState camouflage;
    private int clientBlockLight;

    public ForceFieldBlockEntity() {
        super();
    }

    public int getClientBlockLight() {
        return this.clientBlockLight;
    }

    public void setProjector(BlockPos position) {
        this.projector = position;
        markDirty();
    }

    public IBlockState getCamouflage() {
        return this.camouflage;
    }

    public void setCamouflage(IBlockState camouflage) {
        this.camouflage = camouflage;
        // Update ForceFieldBlock state properties to match camouflage block
        IBlockState current = this.world.getBlockState(this.pos);
        boolean propagatesSkylight = !camouflage.isFullCube();
        boolean solid = camouflage.isFullBlock();
        IBlockState updated = current
            .withProperty(ForceFieldBlockImpl.PROPAGATES_SKYLIGHT, propagatesSkylight)
            .withProperty(ForceFieldBlockImpl.SOLID, solid);
        this.world.setBlockState(this.pos, updated, 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (this.world.isRemote) {
            InitialDataRequestPacket packet = new InitialDataRequestPacket(this.pos);
            Network.sendToServer(packet);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    public Optional<Projector> getProjector() {
        if (this.projector == null) return Optional.empty();
        return Optional.ofNullable(this.world.getTileEntity(this.projector))
            .filter(te -> te.hasCapability(ModCapabilities.PROJECTOR, null))
            .map(te -> (Projector) te.getCapability(ModCapabilities.PROJECTOR, null));
    }

    /**
     * Called from UpdateBlockEntityPacket to apply server-sent NBT on the client.
     * Replaces the 1.21.x handleCustomUpdateTag(CompoundTag, HolderLookup.Provider).
     */
    public void handleCustomUpdateTag(NBTTagCompound tag) {
        if (tag.hasKey("projector")) {
            int[] coords = tag.getIntArray("projector");
            this.projector = new BlockPos(coords[0], coords[1], coords[2]);
        }
        this.clientBlockLight = tag.getInteger("clientBlockLight");

        // Deserialize camouflage from update tag
        if (tag.hasKey("camouflage")) {
            String blockName = tag.getString("camouflage");
            int meta = tag.getInteger("camouflageMeta");
            Block block = Block.REGISTRY.getObject(new ResourceLocation(blockName));
            if (block != null) {
                this.camouflage = block.getStateFromMeta(meta);
            }
        } else {
            this.camouflage = null;
        }

        this.world.checkLight(this.pos);
        updateRenderClient();
    }

    /**
     * Called from tickServer or projector to push update data to clients.
     * Replaces the 1.21.x getCustomUpdateTag(HolderLookup.Provider).
     */
    public NBTTagCompound getCustomUpdateTag() {
        NBTTagCompound tag = new NBTTagCompound();
        if (this.projector != null) {
            tag.setIntArray("projector", new int[]{this.projector.getX(), this.projector.getY(), this.projector.getZ()});
        }
        int light = getProjector()
            .map(projector -> Math.round((float) Math.min(projector.getModuleCount(ModModules.GLOW), 64) / 64 * 15))
            .orElse(0);
        tag.setInteger("clientBlockLight", light);
        // Include camouflage in update tag for client sync
        if (this.camouflage != null) {
            ResourceLocation regName = Block.REGISTRY.getNameForObject(this.camouflage.getBlock());
            if (regName != null) {
                tag.setString("camouflage", regName.toString());
                tag.setInteger("camouflageMeta", this.camouflage.getBlock().getMetaFromState(this.camouflage));
            }
        }
        return tag;
    }

    @Override
    protected void saveTag(NBTTagCompound compound) {
        super.saveTag(compound);

        if (this.projector != null) {
            compound.setIntArray("projector", new int[]{this.projector.getX(), this.projector.getY(), this.projector.getZ()});
        }
        if (this.camouflage != null) {
            ResourceLocation regName = Block.REGISTRY.getNameForObject(this.camouflage.getBlock());
            if (regName != null) {
                compound.setString("camouflage", regName.toString());
                compound.setInteger("camouflageMeta", this.camouflage.getBlock().getMetaFromState(this.camouflage));
            }
        }
    }

    @Override
    protected void loadTag(NBTTagCompound compound) {
        super.loadTag(compound);

        if (compound.hasKey("projector")) {
            int[] coords = compound.getIntArray("projector");
            this.projector = new BlockPos(coords[0], coords[1], coords[2]);
        }
        if (compound.hasKey("camouflage")) {
            String blockName = compound.getString("camouflage");
            int meta = compound.getInteger("camouflageMeta");
            Block block = Block.REGISTRY.getObject(new ResourceLocation(blockName));
            if (block != null) {
                this.camouflage = block.getStateFromMeta(meta);
            }
        }
    }

    public void updateRenderClient() {
        IBlockState state = this.world.getBlockState(this.pos);
        this.world.notifyBlockUpdate(this.pos, state, state, 3);
    }
}
