package dev.su5ed.mffs.block;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.api.ForceFieldBlock;
import dev.su5ed.mffs.api.Projector;
import dev.su5ed.mffs.api.module.Module;
import dev.su5ed.mffs.api.security.BiometricIdentifier;
import dev.su5ed.mffs.api.security.FieldPermission;
import dev.su5ed.mffs.blockentity.ForceFieldBlockEntity;
import dev.su5ed.mffs.setup.ModObjects;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ForceFieldBlockImpl extends Block implements ForceFieldBlock, ITileEntityProvider {

    private static final AxisAlignedBB COLLIDABLE_BOX = new AxisAlignedBB(0.01, 0.01, 0.01, 0.99, 0.99, 0.99);

    public static final PropertyBool PROPAGATES_SKYLIGHT = PropertyBool.create("propagates_skylight");
    public static final PropertyBool SOLID = PropertyBool.create("solid");

    public ForceFieldBlockImpl() {
        super(Material.GLASS);
        setSoundType(SoundType.GLASS);
        setHardness(-1.0F);
        setResistance(720000000.0F);
        setDefaultState(this.blockState.getBaseState()
            .withProperty(PROPAGATES_SKYLIGHT, true)
            .withProperty(SOLID, true));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PROPAGATES_SKYLIGHT, SOLID);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
            .withProperty(PROPAGATES_SKYLIGHT, (meta & 1) == 0)
            .withProperty(SOLID, (meta & 2) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(PROPAGATES_SKYLIGHT) ? 0 : 1) | (state.getValue(SOLID) ? 2 : 0);
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(PROPAGATES_SKYLIGHT) ? 0 : 15;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }

    @Override
    public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        // Only a subset of force field blocks emit light based on configured spacing.
        // With spacing 3, roughly 1/3 of blocks emit light — reduces lighting BFS cost.
        int spacing = Math.max(1, MFFSConfig.forceFieldLightSpacing);
        if ((pos.getX() + pos.getY() + pos.getZ()) % spacing != 0) return 0;

        return Optional.ofNullable(world.getTileEntity(pos))
            .map(te -> te instanceof ForceFieldBlockEntity f ? f.getClientBlockLight() : null)
            .orElseGet(() -> super.getLightValue(state, world, pos));
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        // Delegate to the camouflage block when present.
        // Otherwise, return true only when the neighbor on this face is also a force field block so
        // that the shared internal face is culled (making adjacent force fields appear seamless).
        // All faces touching non-force-field blocks return false so those faces always render.
        return getCamouflageBlock(world, pos)
            .filter(this::preventStackOverflow)
            .map(s -> s.doesSideBlockRendering(world, pos, face))
            .orElseGet(() -> world.getBlockState(pos.offset(face)).getBlock() instanceof ForceFieldBlock);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getCamouflageBlock(source, pos)
            .filter(this::preventStackOverflow)
            .map(s -> s.getBoundingBox(source, pos))
            .orElse(FULL_BLOCK_AABB);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos,
                                      AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
                                      @Nullable Entity entityIn, boolean isActualState) {
        AxisAlignedBB box = getCollisionBoxForEntity(state, worldIn, pos, entityIn);
        if (box != null) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
        }
    }

    @Nullable
    private AxisAlignedBB getCollisionBoxForEntity(IBlockState state, IBlockAccess world,
                                                    BlockPos pos, @Nullable Entity entityIn) {
        Optional<Projector> projectorOpt = getProjector(world, pos);
        if (projectorOpt.isPresent() && entityIn instanceof EntityPlayer player) {
            BiometricIdentifier identifier = projectorOpt.get().getBiometricIdentifier();
            if (isAuthorized(identifier, player)) {
                boolean isAbove = player.posY >= pos.getY() + 0.99;
                if (!isAbove && MFFSConfig.allowWalkThroughForceFields) {
                    return null; // authorized walk-through
                } else if (!isAbove && player.isSneaking()) {
                    return null; // sneak through
                }
            }
        }
        return getCamouflageBlock(world, pos)
            .filter(this::preventStackOverflow)
            .map(s -> s.getCollisionBoundingBox(world, pos))
            .orElse(COLLIDABLE_BOX);
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        super.onEntityCollision(world, pos, state, entity);
        Optional<Projector> projectorOpt = getProjector(world, pos);
        if (!projectorOpt.isPresent()) return;
        Projector projector = projectorOpt.get();
        for (Module module : projector.getModuleInstances()) {
            if (module.onCollideWithForceField(world, pos, entity)) return;
        }
        if (!world.isRemote) {
            double cx = pos.getX() + 0.5, cy = pos.getY(), cz = pos.getZ() + 0.5;
            if (entity.getDistanceSq(cx, cy, cz) < 0.49) {
                BiometricIdentifier identifier = projector.getBiometricIdentifier();
                boolean isAuthorizedPlayer = entity instanceof EntityPlayer player
                    && isAuthorized(identifier, player);
                if (entity instanceof EntityLivingBase living) {
                    boolean applyEffects = !(entity instanceof EntityPlayer player)
                        || (!player.capabilities.isCreativeMode
                            && (!isAuthorizedPlayer || !MFFSConfig.disableForceFieldEffectsForAuthorizedPlayers));
                    if (applyEffects) {
                        living.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 4 * 20, 3));
                        living.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20, 1));
                    }
                }
                boolean applyDamage = !(entity instanceof EntityPlayer player)
                    || (!player.capabilities.isCreativeMode && !isAuthorizedPlayer)
                    || (!isSneaking(entity) && !MFFSConfig.allowWalkThroughForceFields
                        && !MFFSConfig.disableForceFieldDamageForAuthorizedPlayers);
                if (applyDamage) {
                    entity.attackEntityFrom(new DamageSource(ModObjects.FIELD_SHOCK_DAMAGE_TYPE), Integer.MAX_VALUE);
                }
            }
        }
    }

    @Override
    public Optional<Projector> getProjector(IBlockAccess world, BlockPos pos) {
        TileEntity te = world != null ? world.getTileEntity(pos) : null;
        if (te instanceof ForceFieldBlockEntity forceField) {
            return forceField.getProjector();
        }
        return Optional.empty();
    }

    private Optional<IBlockState> getCamouflageBlock(IBlockAccess world, BlockPos pos) {
        if (world == null || pos == null) return Optional.empty();
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ForceFieldBlockEntity forceField) {
            return Optional.ofNullable(forceField.getCamouflage());
        }
        return Optional.empty();
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target,
                                   World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public net.minecraft.item.Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) { return true; }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new ForceFieldBlockEntity();
    }

    private boolean isSneaking(Entity entity) { return entity.isSneaking(); }

    private boolean preventStackOverflow(IBlockState state) { return state.getBlock() != this; }

    private boolean isAuthorized(BiometricIdentifier identifier, EntityPlayer player) {
        return player.capabilities.isCreativeMode
            || (identifier != null && identifier.isAccessGranted(player, FieldPermission.WARP));
    }
}
