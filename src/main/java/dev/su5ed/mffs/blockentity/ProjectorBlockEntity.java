package dev.su5ed.mffs.blockentity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
// 1.21.x: import com.mojang.datafixers.util.Pair; — not available in 1.12.2
import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.api.Projector;
import dev.su5ed.mffs.api.TargetPosPair;
import dev.su5ed.mffs.api.module.Module;
import dev.su5ed.mffs.api.module.ModuleType;
import dev.su5ed.mffs.api.module.ProjectorMode;
import dev.su5ed.mffs.item.CustomProjectorModeItem;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.network.UpdateAnimationSpeed;
import dev.su5ed.mffs.network.UpdateBlockEntityPacket;
import dev.su5ed.mffs.setup.*;
import dev.su5ed.mffs.util.ModUtil;
import dev.su5ed.mffs.util.ObjectCache;
import dev.su5ed.mffs.util.SetBlockEvent;
import dev.su5ed.mffs.util.inventory.InventorySlot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.Nullable;

// 1.21.x imports (commented out):
// import com.mojang.datafixers.util.Pair;
// import net.minecraft.core.BlockPos;
// import net.minecraft.core.Direction;
// import net.minecraft.core.HolderLookup;
// import net.minecraft.nbt.CompoundTag;
// import net.minecraft.sounds.SoundSource;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.item.BlockItem;
// import net.minecraft.world.level.block.Block;
// import net.minecraft.world.level.block.RenderShape;
// import net.minecraft.world.level.block.entity.BlockEntity;
// import net.minecraft.world.level.block.state.BlockState;
// import net.minecraft.world.level.storage.ValueInput;
// import net.minecraft.world.phys.Vec3;
// import net.neoforged.bus.api.SubscribeEvent;
// import net.neoforged.neoforge.capabilities.Capabilities;
// import net.neoforged.neoforge.common.NeoForge;
// import net.neoforged.neoforge.transfer.ResourceHandler;
// import net.neoforged.neoforge.transfer.item.ItemResource;
// import net.neoforged.neoforge.transfer.transaction.Transaction;
// import dev.su5ed.mffs.setup.ModObjects;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ProjectorBlockEntity extends ModularBlockEntity implements Projector {
    private static final String TRANSLATION_CACHE_KEY = "getTranslation";
    private static final String POSITIVE_SCALE_CACHE_KEY = "getPositiveScale";
    private static final String NEGATIVE_SCALE_CACHE_KEY = "getNegativeScale";
    private static final String ROTATION_YAW_CACHE_KEY = "getRotationYaw";
    private static final String ROTATION_PITCH_CACHE_KEY = "getRotationPitch";
    private static final String ROTATION_ROLL_CACHE_KEY = "getRotationRoll";
    private static final String INTERIOR_POINTS_CACHE_KEY = "getInteriorPoints";

    private final List<ScheduledEvent> scheduledEvents = new ArrayList<>();
    public final InventorySlot secondaryCard;
    public final InventorySlot projectorModeSlot;
    // 1.21.x: ListMultimap<Direction, InventorySlot> → ListMultimap<EnumFacing, InventorySlot>
    public final ListMultimap<EnumFacing, InventorySlot> fieldModuleSlots;
    public final List<InventorySlot> upgradeSlots;

    private final Semaphore semaphore = new Semaphore();
    private final Set<BlockPos> projectedBlocks = Collections.synchronizedSet(new HashSet<>());
    // Orphan positions left over from a soft-destroy (resize/module change). Drained gradually;
    // a hard destroyField() removes them immediately instead.
    private final Set<BlockPos> pendingRemoval = Collections.synchronizedSet(new HashSet<>());
    // 1.21.x: Pair<BlockState, Boolean> (com.mojang.datafixers.util.Pair) — not in 1.12.2
    // Use AbstractMap.SimpleEntry<IBlockState, Boolean> as key=blockState, value=canProject
    private final LoadingCache<BlockPos, AbstractMap.SimpleEntry<IBlockState, Boolean>> projectionCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(new CacheLoader<>() {
            @Override
            public AbstractMap.SimpleEntry<IBlockState, Boolean> load(BlockPos key) {
                return canProjectPos(key);
            }
        });
    private int clientAnimationSpeed;

    public ProjectorBlockEntity() {
        super(50);

        this.secondaryCard = addSlot("secondaryCard", InventorySlot.Mode.BOTH, ModUtil::isCard);
        this.projectorModeSlot = addSlot("projectorMode", InventorySlot.Mode.BOTH, ModUtil::isProjectorMode, this::onModeChanged);
        // 1.21.x: StreamEx.of(Direction.values()) → StreamEx.of(EnumFacing.values())
        this.fieldModuleSlots = StreamEx.of(EnumFacing.values())
            .flatMap(side -> IntStreamEx.range(2)
                .mapToEntry(i -> side, i -> addSlot("field_module_" + side.getName() + "_" + i, InventorySlot.Mode.BOTH, stack -> ModUtil.isModule(stack, Module.Category.FIELD), stack -> onFieldModuleChanged())))
            .toListAndThen(ImmutableListMultimap::copyOf);
        this.upgradeSlots = createUpgradeSlots(6, this::isMatrixModuleOrPass, stack -> softDestroyField());
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == ModCapabilities.PROJECTOR) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == ModCapabilities.PROJECTOR) return (T) this;
        return super.getCapability(capability, facing);
    }

    // 1.21.x: @SubscribeEvent + NeoForge.EVENT_BUS → @SubscribeEvent + MinecraftForge.EVENT_BUS
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onSetBlock(SetBlockEvent event) {
        // 1.21.x: event.getLevel() == this.level
        if (event.getWorld() == this.world && !this.semaphore.isInStage(ProjectionStage.STANDBY) && event.getState().getBlock() != ModBlocks.FORCE_FIELD) {
            this.projectionCache.invalidate(event.getPos());
        }
    }

    private boolean isMatrixModuleOrPass(ItemStack stack) {
        // 1.21.x: stack.getCapability(ModCapabilities.MODULE_TYPE)
        return Optional.ofNullable(stack.getCapability(ModCapabilities.MODULE_TYPE, null))
            .map(ModuleType::getCategories)
            .map(categories -> categories.isEmpty() || categories.contains(Module.Category.MATRIX))
            .orElse(true);
    }

    public int computeAnimationSpeed() {
        int speed = 2;
        int fortronCost = getFortronCost();
        if (isActive() && getMode().isPresent() && canConsumeFieldCost(fortronCost)) {
            speed *= fortronCost / 8.0F;
        }
        return Math.min(120, speed);
    }

    public int getAnimationSpeed() {
        return this.clientAnimationSpeed;
    }

    public void setClientAnimationSpeed(int clientAnimationSpeed) {
        // 1.21.x: this.level.isClientSide()
        if (!this.world.isRemote) {
            throw new IllegalStateException("Must only be called on the client");
        }
        this.clientAnimationSpeed = clientAnimationSpeed;
    }

    // 1.21.x: be() returned BlockEntity; in 1.12.2 TileEntity
    @Override
    public BaseBlockEntity be() {
        return this;
    }

    // 1.21.x: getCachedBlockState returns BlockState → IBlockState in 1.12.2
    public IBlockState getCachedBlockState(BlockPos pos) {
        // 1.21.x: this.projectionCache.getUnchecked(pos).getFirst() → .getKey()
        return this.projectionCache.getUnchecked(pos).getKey();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 1.21.x: this.level.isClientSide()
        if (!this.world.isRemote) {
            // 1.21.x: NeoForge.EVENT_BUS.register(this)
            MinecraftForge.EVENT_BUS.register(this);
            reCalculateForceField();
        }
    }

    // 1.21.x: setRemoved() → invalidate()
    @Override
    public void invalidate() {
        if (!this.world.isRemote) {
            // 1.21.x: NeoForge.EVENT_BUS.unregister(this)
            MinecraftForge.EVENT_BUS.unregister(this);
        }
        super.invalidate();
    }

    @Override
    protected void addModuleSlots(List<? super InventorySlot> list) {
        super.addModuleSlots(list);
        list.addAll(this.upgradeSlots);
        list.addAll(this.fieldModuleSlots.values());
    }

    @Override
    public void tickServer() {
        super.tickServer();

        Iterator<ScheduledEvent> it = this.scheduledEvents.iterator();
        while (it.hasNext()) {
            ScheduledEvent event = it.next();

            if (event.countDown()) {
                event.runnable.run();
                it.remove();
            }
        }

        int fortronCost = getFortronCost();
        if (isActive() && getMode().isPresent() && canConsumeFieldCost(fortronCost)) {
            consumeCost();

            if (getTicks() % 10 == 0) {
                if (this.semaphore.isInStage(ProjectionStage.STANDBY)) {
                    reCalculateForceField();
                } else if (this.semaphore.isReady() && this.semaphore.isComplete(ProjectionStage.SELECTING)) {
                    projectField();
                }

                // Drain orphan blocks left over from a soft destroy (resize / module change)
                if (!this.pendingRemoval.isEmpty()) {
                    int speed = Math.min(getProjectionSpeed(), MFFSConfig.maxFFGenPerTick);
                    int drained = 0;
                    Iterator<BlockPos> orphanIt = this.pendingRemoval.iterator();
                    while (orphanIt.hasNext() && drained < speed) {
                        BlockPos orphan = orphanIt.next();
                        orphanIt.remove();
                        if (this.world.getBlockState(orphan).getBlock() == ModBlocks.FORCE_FIELD) {
                            net.minecraft.tileentity.TileEntity ote = this.world.getTileEntity(orphan);
                            if (ote instanceof ForceFieldBlockEntity offe && this.pos.equals(offe.getProjectorPos())) {
                                this.world.setBlockToAir(orphan);
                            }
                        }
                        drained++;
                    }
                }
            }

            if (getTicks() % (2 * 20) == 0 && !hasModule(ModModules.SILENCE)) {
                // 1.21.x: SoundSource.BLOCKS → SoundCategory.BLOCKS
                this.world.playSound(null, this.pos, ModSounds.FIELD, SoundCategory.BLOCKS, 0.4F, 1 - this.world.rand.nextFloat() * 0.1F);
            }
        } else {
            destroyField();
        }

        int speed = computeAnimationSpeed();
        if (speed != this.clientAnimationSpeed) {
            this.clientAnimationSpeed = speed;
            // 1.21.x: sendToChunk(new UpdateAnimationSpeed(worldPosition, speed))
            sendToChunk(new UpdateAnimationSpeed(this.pos, speed));
        }
    }

    private boolean canConsumeFieldCost(int fortronCost) {
        // 1.21.x: try (Transaction tx = Transaction.openRoot()) {
        //     return this.fortronStorage.extractFortron(fortronCost, tx) >= fortronCost;
        // }
        return this.fortronStorage.extractFortron(fortronCost, true) >= fortronCost;
    }

    // 1.21.x: preRemoveSideEffects(BlockPos pos, BlockState state)
    @Override
    public void preRemoveSideEffects(BlockPos pos) {
        destroyField();

        super.preRemoveSideEffects(pos);
    }

    // 1.21.x: createMenu(int containerId, Inventory inventory, Player player) → AbstractContainerMenu
    // In 1.12.2, GUI is handled via IGuiHandler. ModMenus.PROJECTOR = GUI ID.
    // TODO: Ensure IGuiHandler returns new ProjectorMenu for the matching GUI ID

    @Override
    protected int doGetFortronCost() {
        return super.doGetFortronCost() + 5;
    }

    @Override
    public float getAmplifier() {
        return Math.max(Math.min(getCalculatedFieldPositions().size() / 1000, 10), 1);
    }

    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();
        // Update mode light (matches 1.20.1 reference behaviour: only re-check projector's own
        // light level; glow changes on already-projected field blocks take effect on next field
        // regeneration or chunk reload rather than spamming O(N) packets per inventory change).
        if (this.world != null && !this.world.isRemote) {
            this.world.checkLight(this.pos);
        }
    }

    // 1.21.x: getUpdateTag(HolderLookup.Provider provider) → getUpdateTag()
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("animationSpeed", computeAnimationSpeed());
        return tag;
    }

    // 1.21.x: handleUpdateTag(ValueInput input) → handleUpdateTag(NBTTagCompound tag)
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (tag.hasKey("animationSpeed")) {
            this.clientAnimationSpeed = tag.getInteger("animationSpeed");
        }
    }

    @Override
    public Optional<ProjectorMode> getMode() {
        // 1.21.x: getModeStack().getCapability(ModCapabilities.PROJECTOR_MODE)
        return Optional.ofNullable(getModeStack().getCapability(ModCapabilities.PROJECTOR_MODE, null));
    }

    @Override
    public ItemStack getModeStack() {
        return this.projectorModeSlot.getItem();
    }

    // 1.21.x: getSlotsFromSide(Direction side) → getSlotsFromSide(EnumFacing side)
    @Override
    public Collection<InventorySlot> getSlotsFromSide(EnumFacing side) {
        return this.fieldModuleSlots.get(side);
    }

    @Override
    public Collection<InventorySlot> getUpgradeSlots() {
        return this.upgradeSlots;
    }

    @Override
    public BlockPos getTranslation() {
        return cached(TRANSLATION_CACHE_KEY, () -> {
            // 1.21.x: Direction.NORTH/SOUTH/WEST/EAST/UP/DOWN → EnumFacing
            int zTranslationNeg = getModuleCount(ModModules.TRANSLATION, getSlotsFromSide(EnumFacing.NORTH));
            int zTranslationPos = getModuleCount(ModModules.TRANSLATION, getSlotsFromSide(EnumFacing.SOUTH));

            int xTranslationNeg = getModuleCount(ModModules.TRANSLATION, getSlotsFromSide(EnumFacing.WEST));
            int xTranslationPos = getModuleCount(ModModules.TRANSLATION, getSlotsFromSide(EnumFacing.EAST));

            int yTranslationPos = getModuleCount(ModModules.TRANSLATION, getSlotsFromSide(EnumFacing.UP));
            int yTranslationNeg = getModuleCount(ModModules.TRANSLATION, getSlotsFromSide(EnumFacing.DOWN));

            return new BlockPos(xTranslationPos - xTranslationNeg, yTranslationPos - yTranslationNeg, zTranslationPos - zTranslationNeg);
        });
    }

    @Override
    public BlockPos getPositiveScale() {
        return cached(POSITIVE_SCALE_CACHE_KEY, () -> {
            int zScalePos = getModuleCount(ModModules.SCALE, getSlotsFromSide(EnumFacing.SOUTH));
            int xScalePos = getModuleCount(ModModules.SCALE, getSlotsFromSide(EnumFacing.EAST));
            int yScalePos = getModuleCount(ModModules.SCALE, getSlotsFromSide(EnumFacing.UP));

            int omnidirectionalScale = getModuleCount(ModModules.SCALE, getUpgradeSlots());

            zScalePos += omnidirectionalScale;
            xScalePos += omnidirectionalScale;
            yScalePos += omnidirectionalScale;

            return new BlockPos(xScalePos, yScalePos, zScalePos);
        });
    }

    @Override
    public BlockPos getNegativeScale() {
        return cached(NEGATIVE_SCALE_CACHE_KEY, () -> {
            int zScaleNeg = getModuleCount(ModModules.SCALE, getSlotsFromSide(EnumFacing.NORTH));
            int xScaleNeg = getModuleCount(ModModules.SCALE, getSlotsFromSide(EnumFacing.WEST));
            int yScaleNeg = getModuleCount(ModModules.SCALE, getSlotsFromSide(EnumFacing.DOWN));

            int omnidirectionalScale = getModuleCount(ModModules.SCALE, getUpgradeSlots());

            zScaleNeg += omnidirectionalScale;
            xScaleNeg += omnidirectionalScale;
            yScaleNeg += omnidirectionalScale;

            return new BlockPos(xScaleNeg, yScaleNeg, zScaleNeg);
        });
    }

    @Override
    public int getRotationYaw() {
        return cached(ROTATION_YAW_CACHE_KEY, () -> {
            int rotation = getModuleCount(ModModules.ROTATION, getSlotsFromSide(EnumFacing.EAST))
                - getModuleCount(ModModules.ROTATION, getSlotsFromSide(EnumFacing.WEST));
            return rotation * 2;
        });
    }

    @Override
    public int getRotationPitch() {
        return cached(ROTATION_PITCH_CACHE_KEY, () -> {
            int rotation = getModuleCount(ModModules.ROTATION, getSlotsFromSide(EnumFacing.UP))
                - getModuleCount(ModModules.ROTATION, getSlotsFromSide(EnumFacing.DOWN));
            return rotation * 2;
        });
    }

    @Override
    public int getRotationRoll() {
        return cached(ROTATION_ROLL_CACHE_KEY, () -> {
            int rotation = getModuleCount(ModModules.ROTATION, getSlotsFromSide(EnumFacing.SOUTH))
                - getModuleCount(ModModules.ROTATION, getSlotsFromSide(EnumFacing.NORTH));
            return rotation * 2;
        });
    }

    @Override
    public Collection<TargetPosPair> getCalculatedFieldPositions() {
        return this.semaphore.getOrDefault(ProjectionStage.CALCULATING, Collections.emptyList());
    }

    @Override
    public Set<BlockPos> getInteriorPoints() {
        return cached(INTERIOR_POINTS_CACHE_KEY, () -> {
            // 1.21.x: Set<Vec3> interiorPoints / Vec3 → Vec3d
            Set<Vec3d> interiorPoints = getMode().orElseThrow(NoSuchElementException::new).getInteriorPoints(this);
            BlockPos translation = this.pos.add(getTranslation());
            int rotationYaw = getRotationYaw();
            int rotationPitch = getRotationPitch();
            int rotationRoll = getRotationRoll();
            return StreamEx.of(interiorPoints)
                .map(pos -> rotationYaw != 0 || rotationPitch != 0 || rotationRoll != 0 ? ModUtil.rotateByAngleExact(pos, rotationYaw, rotationPitch, rotationRoll) : pos)
                // 1.21.x: BlockPos.containing(pos).offset(translation) → new BlockPos(pos).add(translation)
                .map(pos -> new BlockPos(pos).add(translation.getX(), translation.getY(), translation.getZ()))
                .toSet();
        });
    }

    public void projectField() {
        CompletableFuture<Void> task = this.semaphore.beginStage(ProjectionStage.PROJECTING);
        for (Module module : getModuleInstances()) {
            module.beforeProject(this);
        }
        // 1.21.x: ModBlocks.FORCE_FIELD.get().defaultBlockState() → ModBlocks.FORCE_FIELD.getDefaultState()
        IBlockState state = ModBlocks.FORCE_FIELD.getDefaultState();
        List<TargetPosPair> projectable = this.semaphore.getResult(ProjectionStage.SELECTING);
        fieldLoop:
        for (TargetPosPair pair : projectable) {
            BlockPos pos = pair.pos();
            for (Module module : getModuleInstances()) {
                Module.ProjectAction action = module.onProject(this, pos);

                if (action == Module.ProjectAction.SKIP) {
                    continue fieldLoop;
                } else if (action == Module.ProjectAction.INTERRUPT) {
                    break fieldLoop;
                }
            }

            // Check if this position is already occupied by our own FF block (soft-destroy transition).
            // If so, reclaim it without re-placing or spending Fortron.
            net.minecraft.tileentity.TileEntity existingTe = this.world.getTileEntity(pos);
            boolean isOwnField = existingTe instanceof ForceFieldBlockEntity ffe && this.pos.equals(ffe.getProjectorPos());

            if (!isOwnField) {
                // 1.21.x: level.setBlock(pos, state, Block.UPDATE_NONE) → world.setBlockState(pos, state, 0)
                this.world.setBlockState(pos, state, 0);
                // Set the controlling projector of the force field block to this one
                // 1.21.x: this.level.getBlockEntity(pos, ModObjects.FORCE_FIELD_BLOCK_ENTITY.get()).ifPresent(...)
                net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
                if (te instanceof ForceFieldBlockEntity be) {
                    be.setProjector(this.pos);
                    // 1.21.x: BlockState camouflage = getCamoBlock(pair.original())
                    IBlockState camouflage = getCamoBlock(pair.original());
                    be.setCamouflage(camouflage);
                }
                // Only update after the projector has been set
                // 1.21.x: level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL) → world.notifyBlockUpdate
                this.world.notifyBlockUpdate(pos, state, state, 3);
                // 1.21.x: try (Transaction tx = ...) { extractFortron(1, tx); tx.commit(); }
                this.fortronStorage.extractFortron(1, false);
            } else {
                // Reclaim existing block: update camouflage and push fresh clientBlockLight to clients
                if (existingTe instanceof ForceFieldBlockEntity be) {
                    IBlockState camouflage = getCamoBlock(pair.original());
                    be.setCamouflage(camouflage);
                    // Send fresh update tag so clientBlockLight reflects current glow module count
                    Network.sendToAllAround(new UpdateBlockEntityPacket(pos, be.getCustomUpdateTag()), this.world, pos, 64);
                }
            }
            // Mark as part of the new field; remove from pending-removal so it isn't deleted.
            this.pendingRemoval.remove(pos);
            this.projectedBlocks.add(pos);
            this.projectionCache.invalidate(pos);
        }
        task.complete(null);
        runSelectionTask();
    }

    @Override
    public void schedule(int delay, Runnable runnable) {
        this.scheduledEvents.add(new ScheduledEvent(delay, runnable));
    }

    // 1.21.x: canProjectPos returns Pair<BlockState, Boolean>, now AbstractMap.SimpleEntry<IBlockState, Boolean>
    private AbstractMap.SimpleEntry<IBlockState, Boolean> canProjectPos(BlockPos pos) {
        IBlockState state = this.world.getBlockState(pos);
        // Allow projecting over our own FF blocks that remain from a previous soft-destroy —
        // they will be "reclaimed" in projectField() without being re-placed or costing Fortron.
        // Only treat the block as reclaimable while it is still in pendingRemoval; once reclaimed
        // (removed from the set) this position must evaluate as non-projectable so SELECTING won't
        // keep cycling over it endlessly.
        boolean isOwnField = false;
        if (state.getBlock() == ModBlocks.FORCE_FIELD && this.pendingRemoval.contains(pos)) {
            net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
            isOwnField = te instanceof ForceFieldBlockEntity ffe && this.pos.equals(ffe.getProjectorPos());
        }
        // 1.21.x: state.isAir() → state.getBlock().isAir(state, world, pos)
        // 1.21.x: state.liquid() → state.getMaterial().isLiquid()
        // 1.21.x: state.is(ModTags.FORCEFIELD_REPLACEABLE) → TODO (1.12.2 tag/oredictionary check)
        // 1.21.x: state.getDestroySpeed(level, pos) != -1 → state.getBlockHardness(world, pos) != -1
        // 1.21.x: state.is(ModBlocks.FORCE_FIELD.get()) → state.getBlock() == ModBlocks.FORCE_FIELD
        boolean canProject = ((state.getBlock().isAir(state, this.world, pos)
            || state.getMaterial().isLiquid()
            || ModTags.getForceFieldReplaceable().contains(state.getBlock())
            || (hasModule(ModModules.DISINTEGRATION) && state.getBlockHardness(this.world, pos) != -1))
            && state.getBlock() != ModBlocks.FORCE_FIELD
            || isOwnField)
            && !pos.equals(this.pos);
        return new AbstractMap.SimpleEntry<>(state, canProject);
    }

    /**
     * Called when a face-slot field module changes (glow, camouflage, shock, etc.).
     * These modules never affect field geometry, so we push a fresh update to all
     * currently-projected blocks immediately — providing instant visual feedback —
     * before triggering the soft rebuild that re-evaluates behavioral modules.
     */
    private void onFieldModuleChanged() {
        refreshFieldVisuals();
        softDestroyField();
    }

    /**
     * Pushes a fresh {@link UpdateBlockEntityPacket} (clientBlockLight + camouflage)
     * to every currently projected block without rebuilding the field.
     * Updates camouflage on each block entity first so the packet reflects the
     * current state (e.g. null when the camo module was just removed).
     */
    private void refreshFieldVisuals() {
        if (this.world == null || this.world.isRemote) return;
        for (BlockPos pos : new HashSet<>(this.projectedBlocks)) {
            net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
            if (te instanceof ForceFieldBlockEntity be) {
                // Recompute camouflage — returns null when camo module is absent
                IBlockState newCamo = getCamoBlock(new net.minecraft.util.math.Vec3d(pos.getX(), pos.getY(), pos.getZ()));
                be.setCamouflage(newCamo);
                Network.sendToAllAround(new UpdateBlockEntityPacket(pos, be.getCustomUpdateTag()), this.world, pos, 64);
            }
        }
    }

    private void onModeChanged(ItemStack stack) {
        // 1.21.x: this.level.getLightEngine().checkBlock(this.worldPosition)
        if (this.world != null) {
            this.world.checkLight(this.pos);
        }
        softDestroyField();
    }

    /**
     * Soft destroy: keeps existing force field blocks in the world and queues them for
     * gradual removal. Blocks that are still valid in the newly-calculated field will be
     * reclaimed during the next projectField() pass, so only genuine orphans are removed.
     * Use this when the field shape is changing (module/mode change) to avoid flicker.
     */
    private void softDestroyField() {
        if (this.world != null && !this.world.isRemote) {
            StreamEx.of(getCalculatedFieldPositions())
                .map(TargetPosPair::pos)
                .filter(pos -> this.world.getBlockState(pos).getBlock() == ModBlocks.FORCE_FIELD)
                .forEach(this.pendingRemoval::add);
        }
        this.projectedBlocks.clear();
        this.projectionCache.invalidateAll();
        this.semaphore.reset();
    }

    @Override
    public void destroyField() {
        // Hard destroy: immediately remove everything (Fortron loss, block break, etc.).
        // Also flush any blocks pending from a prior soft destroy.
        Set<BlockPos> alsoRemove = new HashSet<>(this.pendingRemoval);
        this.pendingRemoval.clear();
        Collection<TargetPosPair> fieldPositions = getCalculatedFieldPositions();
        this.projectedBlocks.clear();
        this.projectionCache.invalidateAll();
        this.semaphore.reset();
        // 1.21.x: this.level.isClientSide() → this.world.isRemote
        if (!this.world.isRemote) {
            StreamEx.of(fieldPositions)
                .map(TargetPosPair::pos)
                // 1.21.x: state.is(ModBlocks.FORCE_FIELD.get()) → state.getBlock() == ModBlocks.FORCE_FIELD
                .filter(pos -> this.world.getBlockState(pos).getBlock() == ModBlocks.FORCE_FIELD)
                // 1.21.x: this.level.removeBlock(pos, false) → this.world.setBlockToAir(pos)
                .forEach(pos -> this.world.setBlockToAir(pos));
            alsoRemove.stream()
                .filter(pos -> this.world.getBlockState(pos).getBlock() == ModBlocks.FORCE_FIELD)
                .forEach(pos -> this.world.setBlockToAir(pos));
        }
    }

    @Override
    protected void saveTag(NBTTagCompound compound) {
        super.saveTag(compound);
        NBTTagList removalList = new NBTTagList();
        for (BlockPos pos : this.pendingRemoval) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", pos.getX());
            entry.setInteger("y", pos.getY());
            entry.setInteger("z", pos.getZ());
            removalList.appendTag(entry);
        }
        compound.setTag("pendingRemoval", removalList);
    }

    @Override
    protected void loadTag(NBTTagCompound compound) {
        super.loadTag(compound);
        this.pendingRemoval.clear();
        if (compound.hasKey("pendingRemoval")) {
            NBTTagList removalList = compound.getTagList("pendingRemoval", 10);
            for (int i = 0; i < removalList.tagCount(); i++) {
                NBTTagCompound entry = removalList.getCompoundTagAt(i);
                this.pendingRemoval.add(new BlockPos(entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z")));
            }
        }
    }

    @Override
    public int getProjectionSpeed() {
        return 28 + 28 * getModuleCount(ModModules.SPEED, getUpgradeSlots());
    }

    @Override
    public int getModuleCount(ModuleType<?> module, Collection<InventorySlot> slots) {
        // Disable scaling of custom mode fields
        return module == ModModules.SCALE && getModeStack().getItem() instanceof CustomProjectorModeItem ? 0 : super.getModuleCount(module, slots);
    }

    private void reCalculateForceField() {
        if (getMode().isPresent()) {
            if (getModeStack().getItem() instanceof ObjectCache cache) {
                cache.clearCache();
            }
            runCalculationTask()
                .thenCompose(v -> runSelectionTask())
                .exceptionally(throwable -> {
                    MFFSMod.LOGGER.error("Error calculating force field blocks", throwable);
                    return null;
                });
        }
    }

    // 1.21.x: BlockState getCamoBlock(Vec3 pos) → IBlockState getCamoBlock(Vec3d pos)
    public IBlockState getCamoBlock(Vec3d pos) {
        // 1.21.x: this.level.isClientSide() → this.world.isRemote
        if (!this.world.isRemote && hasModule(ModModules.CAMOUFLAGE)) {
            if (getModeStack().getItem() instanceof CustomProjectorModeItem custom) {
                // 1.21.x: Map<Vec3, BlockState> → Map<Vec3d, IBlockState>
                Map<Vec3d, IBlockState> map = custom.getFieldBlocks(this, getModeStack());
                IBlockState block = map.get(pos);
                if (block != null) {
                    return block;
                }
            }

            return getCamoBlockFromOwnInventory()
                .orElseGet(this::getWeightedCamoBlockFromNeighbors);
        }
        return null;
    }

    // Prioritize own inventory for backwards compatibility
    @Nullable
    private Optional<IBlockState> getCamoBlockFromOwnInventory() {
        return getAllModuleItemsStream()
            .mapPartial(ProjectorBlockEntity::getFilterBlock)
            // 1.21.x: Block::defaultBlockState → Block::getDefaultState
            .findFirst()
            .map(Block::getDefaultState);
    }

    @Nullable
    private IBlockState getWeightedCamoBlockFromNeighbors() {
        Map<Block, Integer> neighborsInventory = checkNeighbors();
        if (neighborsInventory.isEmpty()) {
            return null;
        }

        List<Block> weightedList = neighborsInventory.entrySet()
            .stream()
            .flatMap(e -> Collections.nCopies(e.getValue(), e.getKey()).stream())
            .collect(Collectors.toList());

        int random = ThreadLocalRandom.current().nextInt(weightedList.size());
        // 1.21.x: block.defaultBlockState() → block.getDefaultState()
        return weightedList.get(random).getDefaultState();
    }

    private CompletableFuture<?> runCalculationTask() {
        CompletableFuture<List<TargetPosPair>> future = this.semaphore.beginStage(ProjectionStage.CALCULATING);
        CompletableFuture.supplyAsync(this::calculateFieldPositions).whenComplete((result, ex) -> {
            if (ex != null) future.completeExceptionally(ex);
            else future.complete(result);
        });
        return future
            .whenComplete((list, ex) -> {
                if (list != null) {
                    for (Module module : getModuleInstances()) {
                        module.onCalculate(this, list);
                    }
                    Collections.shuffle(list);
                }
            })
            .exceptionally(throwable -> {
                MFFSMod.LOGGER.error("Error calculating force field", throwable);
                return Collections.emptyList();
            });
    }

    private List<TargetPosPair> calculateFieldPositions() {
        ProjectorMode mode = getMode().orElseThrow(NoSuchElementException::new);
        // 1.21.x: Set<Vec3> fieldPoints → Set<Vec3d> fieldPoints
        Set<Vec3d> fieldPoints = hasModule(ModModules.INVERTER) ? mode.getInteriorPoints(this) : mode.getExteriorPoints(this);
        BlockPos translation = getTranslation();
        int rotationYaw = getRotationYaw();
        int rotationPitch = getRotationPitch();
        int rotationRoll = getRotationRoll();

        return StreamEx.of(fieldPoints)
            .mapToEntry(pos -> rotationYaw != 0 || rotationPitch != 0 || rotationRoll != 0 ? ModUtil.rotateByAngleExact(pos, rotationYaw, rotationPitch, rotationRoll) : pos)
            // 1.21.x: pos.add(worldPosition.getX(), ...) → pos.addVector(worldPos.getX(), ...); pos.y() → pos.y
            .mapValues(pos -> pos.add(this.pos.getX(), this.pos.getY(), this.pos.getZ()).add(translation.getX(), translation.getY(), translation.getZ()))
            // 1.21.x: pos.y() → pos.y; this.level.getHeight() → this.world.getHeight()
            .filterValues((Vec3d pos) -> pos.y <= this.world.getHeight())
            .mapKeyValue((Vec3d original, Vec3d pos) -> new TargetPosPair(new BlockPos((int) Math.round(pos.x), (int) Math.round(pos.y), (int) Math.round(pos.z)), original))
            .toMutableList();
    }

    private CompletableFuture<?> runSelectionTask() {
        CompletableFuture<List<TargetPosPair>> future = this.semaphore.beginStage(ProjectionStage.SELECTING);
        CompletableFuture.supplyAsync(this::selectProjectablePositions).whenComplete((result, ex) -> {
            if (ex != null) future.completeExceptionally(ex);
            else future.complete(result);
        });
        return future
            .exceptionally(throwable -> {
                MFFSMod.LOGGER.error("Error selecting force field blocks", throwable);
                return Collections.emptyList();
            });
    }

    private List<TargetPosPair> selectProjectablePositions() {
        if (this.projectedBlocks.isEmpty() && getModeStack().getItem() instanceof ObjectCache cache) {
            cache.clearCache();
        }
        List<TargetPosPair> fieldToBeProjected = new ArrayList<>(getCalculatedFieldPositions());
        Set<Module> modules = getModuleInstances();
        for (Module module : modules) {
            module.beforeSelect(this, fieldToBeProjected);
        }
        int constructionSpeed = Math.min(getProjectionSpeed(), MFFSConfig.maxFFGenPerTick);
        List<TargetPosPair> projectable = new ArrayList<>();
        fieldLoop:
        for (int i = 0, constructionCount = 0; i < fieldToBeProjected.size() && constructionCount < constructionSpeed && !isInvalid() && this.semaphore.isInStage(ProjectionStage.SELECTING); i++) {
            TargetPosPair pair = fieldToBeProjected.get(i);
            BlockPos pos = pair.pos();
            for (Module module : modules) {
                Module.ProjectAction action = module.onSelect(this, pos);
                if (action == Module.ProjectAction.SKIP) {
                    continue fieldLoop;
                } else if (action == Module.ProjectAction.INTERRUPT) {
                    break fieldLoop;
                }
            }
            // 1.21.x: projectionCache.getUnchecked(pos).getSecond() → .getValue()
            // 1.21.x: this.level.isLoaded(pos) → this.world.isBlockLoaded(pos)
            if (this.projectionCache.getUnchecked(pos).getValue() && this.world.isBlockLoaded(pos)) {
                projectable.add(pair);
                constructionCount++;
            }
        }
        return projectable;
    }

    // 1.21.x: getFilterBlock(ItemStack) returns Optional<Block>
    // 1.21.x: BlockItem blockItem → ItemBlock blockItem (1.12.2 name)
    // 1.21.x: block.defaultBlockState().getRenderShape() != RenderShape.INVISIBLE
    //   → block.getRenderType(block.getDefaultState()) != EnumBlockRenderType.INVISIBLE
    public static Optional<Block> getFilterBlock(ItemStack stack) {
        if (stack.getItem() instanceof ItemBlock blockItem) {
            Block block = blockItem.getBlock();
            if (block.getRenderType(block.getDefaultState()) != net.minecraft.util.EnumBlockRenderType.INVISIBLE) {
                return Optional.of(block);
            }
        }
        return Optional.empty();
    }

    public Map<Block, Integer> checkNeighbors() {
        Map<Block, Integer> countMap = new HashMap<>();
        // 1.21.x: this.level.isClientSide() → this.world.isRemote
        if (!this.world.isRemote) {
            // 1.21.x: Direction side : Direction.values() → EnumFacing side : EnumFacing.values()
            for (EnumFacing side : EnumFacing.values()) {
                // 1.21.x: this.level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(side), side.getOpposite())
                // In 1.12.2: get TE and IItemHandler capability
                net.minecraft.tileentity.TileEntity neighbor = this.world.getTileEntity(this.pos.offset(side));
                if (neighbor != null && neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) {
                    IItemHandler handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
                    if (handler != null) {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            // 1.21.x: handler.getResource(i).toStack() → handler.getStackInSlot(i)
                            // 1.21.x: handler.getAmountAsInt(i) → handler.getStackInSlot(i).getCount()
                            ItemStack stack = handler.getStackInSlot(i);
                            int count = stack.getCount();
                            getFilterBlock(stack).ifPresent(block -> countMap.put(block, countMap.getOrDefault(block, 0) + count));
                        }
                    }
                }
            }
        }
        return countMap;
    }

    private static class ScheduledEvent {
        public final Runnable runnable;
        public int ticks;

        public ScheduledEvent(int ticks, Runnable runnable) {
            this.ticks = ticks;
            this.runnable = runnable;
        }

        public boolean countDown() {
            return --this.ticks <= 0;
        }
    }

    private enum ProjectionStage {
        STANDBY,
        CALCULATING,
        SELECTING,
        PROJECTING
    }

    private static class Semaphore {
        private ProjectionStage stage = ProjectionStage.STANDBY;
        private final Map<ProjectionStage, CompletableFuture<Object>> tasks = new HashMap<>();

        @SuppressWarnings("unchecked")
        public synchronized <T> CompletableFuture<T> beginStage(ProjectionStage stage) {
            if (isReady()) {
                this.stage = stage;
                CompletableFuture<T> task = new CompletableFuture<>();
                this.tasks.put(stage, (CompletableFuture<Object>) task);
                return task;
            } else {
                throw new RuntimeException("Attempted to switch stage before it was completed");
            }
        }

        public synchronized boolean isComplete(ProjectionStage stage) {
            return this.tasks.containsKey(stage) && this.tasks.get(stage).isDone();
        }

        public synchronized boolean isInStage(ProjectionStage stage) {
            return this.stage == stage;
        }

        public synchronized boolean isReady() {
            return this.stage == ProjectionStage.STANDBY || isComplete(this.stage);
        }

        @SuppressWarnings("unchecked")
        public synchronized <T> T getResult(ProjectionStage stage) {
            CompletableFuture<T> task = (CompletableFuture<T>) this.tasks.get(stage);
            if (!task.isDone()) {
                throw new RuntimeException("Stage " + stage + " hasn't completed yet!");
            }
            return task.join();
        }

        @SuppressWarnings("unchecked")
        public synchronized <T> T getOrDefault(ProjectionStage stage, T defaultValue) {
            return this.tasks.containsKey(stage) ? (T) this.tasks.get(stage).getNow(defaultValue) : defaultValue;
        }

        public synchronized void reset() {
            this.stage = ProjectionStage.STANDBY;
            this.tasks.clear();
        }
    }
}
