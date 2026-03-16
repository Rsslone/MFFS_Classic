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
import java.util.concurrent.atomic.AtomicReference;
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

    /**
     * Upgrade-slot modules that do NOT affect field geometry and therefore should not
     * trigger a force-field regeneration when inserted or removed.
     *
     * <p>Note: Glow Module is intentionally absent from this set.  Glow changes are
     * handled separately: they call {@link #refreshFieldLights()} to push updated
     * {@code clientBlockLight} values in-place without a field rebuild.
     *
     * @see #isGlowOrEmpty(ItemStack)
     */
    private static final Set<ModuleType<?>> REGEN_EXEMPT_MODULES;
    static {
        Set<ModuleType<?>> s = new HashSet<>();
        s.add(ModModules.SPEED);
        s.add(ModModules.CAPACITY);
        s.add(ModModules.SHOCK);
        s.add(ModModules.SPONGE);
        s.add(ModModules.COLLECTION);
        s.add(ModModules.SILENCE);
        REGEN_EXEMPT_MODULES = Collections.unmodifiableSet(s);
    }

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
    // LinkedHashSet so the drain iterator walks positions in insertion order; applyFieldDiff
    // shuffles the toRemove list before inserting, matching the random order used during build.
    private final Set<BlockPos> pendingRemoval = Collections.synchronizedSet(new LinkedHashSet<>());
    // Positions whose server-side camouflage was updated in applyFieldDiff() but whose client
    // notification packet hasn't been sent yet.  Drained N-per-cycle in tickServer() at
    // projection speed so large fields update gradually rather than all at once.
    private final Set<BlockPos> pendingCamoRefresh = Collections.synchronizedSet(new LinkedHashSet<>());
    // Positions projected in the previous session, loaded from NBT. Used once per load to diff
    // against the freshly-calculated field to find orphans (e.g. after a field-size code change).
    // Never synced/threaded — only accessed on the server tick thread.
    private final Set<BlockPos> savedProjectedBlocks = new HashSet<>();
    // Shadow set of currently-calculated field positions for O(1) lookup in the gap-fill sweep.
    // Rebuilt asynchronously in runCalculationTask; published via volatile for safe visibility.
    private volatile Set<BlockPos> calculatedFieldSet = Collections.emptySet();
    // Non-null when a diff-based field transition is in progress.  Holds a snapshot of the old
    // projectedBlocks taken at the moment softDestroyField() was called.  The field stays visible
    // while the async recalculation runs; once CALCULATING completes, applyFieldDiff() diffs the
    // snapshot against the new geometry and only touches blocks that actually changed.
    private Set<BlockPos> pendingDiffSnapshot = null;
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
        // Each upgrade slot gets its own closure so we can compare old vs new content.
        // Three-way decision per slot change:
        //   1. Both old and new are regen-exempt (Speed/Capacity/etc.)  → no action
        //   2. Both old and new are glow-or-empty                        → refreshFieldLights()
        //   3. Either side is a geometry-affecting module                → softDestroyField()
        //
        // upgradeSlotListRef is populated after the stream completes so the capacity-provider
        // lambdas can safely reference all upgrade slots at insert time (not at construction time).
        AtomicReference<List<InventorySlot>> upgradeSlotListRef = new AtomicReference<>();
        this.upgradeSlots = IntStreamEx.range(6)
            .mapToObj(i -> {
                InventorySlot[] ref = new InventorySlot[1];
                ref[0] = addSlot("upgrade_" + i, InventorySlot.Mode.BOTH, this::isMatrixModuleOrPass,
                    current -> {
                        ItemStack prev = ref[0].getPreviousItem();
                        if (isExemptFromRegen(prev) && isExemptFromRegen(current)) {
                            // purely exempt modules (speed/capacity/etc.) — nothing changes
                        } else if (isGlowOrEmpty(prev) && isGlowOrEmpty(current)) {
                            // glow-only change: update light in-place, no field rebuild needed
                            refreshFieldLights();
                        } else {
                            softDestroyField();
                        }
                    },
                    stack -> {
                        // Limit speed modules across all upgrade slots to the configured maximum.
                        ModuleType<?> type = stack.getCapability(ModCapabilities.MODULE_TYPE, null);
                        if (type != ModModules.SPEED) return stack.getMaxStackSize();
                        List<InventorySlot> allUpgradeSlots = upgradeSlotListRef.get();
                        if (allUpgradeSlots == null) return stack.getMaxStackSize();
                        // Count speed modules already present in every OTHER upgrade slot.
                        int totalInOthers = allUpgradeSlots.stream()
                            .filter(slot -> slot != ref[0])
                            .mapToInt(slot -> {
                                ItemStack content = slot.getItem();
                                if (content.isEmpty()) return 0;
                                ModuleType<?> slotType = content.getCapability(ModCapabilities.MODULE_TYPE, null);
                                return slotType == ModModules.SPEED ? content.getCount() : 0;
                            })
                            .sum();
                        return Math.max(0, Math.min(MFFSConfig.maxSpeedModulesProjector - totalInOthers, stack.getMaxStackSize()));
                    });
                return ref[0];
            })
            .toList();
        upgradeSlotListRef.set(this.upgradeSlots);
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
        // Invalidate the projection cache for the changed position so the next gap-fill sweep
        // sees the current world state rather than a stale cached value.
        // 1.21.x: event.getLevel() == this.level
        if (event.getWorld() == this.world && event.getState().getBlock() != ModBlocks.FORCE_FIELD) {
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

    /**
     * Returns {@code true} if {@code stack} is empty or holds a module that is exempt from
     * triggering a force-field regeneration (i.e. it does not affect field geometry or makeup).
     * See {@link #REGEN_EXEMPT_MODULES} for the full list.
     */
    private static boolean isExemptFromRegen(ItemStack stack) {
        if (stack.isEmpty()) return true;
        ModuleType<?> type = stack.getCapability(ModCapabilities.MODULE_TYPE, null);
        return type != null && REGEN_EXEMPT_MODULES.contains(type);
    }

    /**
     * Returns {@code true} if {@code stack} is empty or is a Glow Module.
     * Used to detect upgrade-slot changes that only affect light level, allowing
     * an in-place {@link #refreshFieldLights()} instead of a full field rebuild.
     */
    private static boolean isGlowOrEmpty(ItemStack stack) {
        if (stack.isEmpty()) return true;
        ModuleType<?> type = stack.getCapability(ModCapabilities.MODULE_TYPE, null);
        return type == ModModules.GLOW;
    }

    public int computeAnimationSpeed() {
        int speed = 1;
        int fortronCost = getFortronCost();
        // Speed up the rotor only when:
        //   1. The projector is active with a mode present.
        //   2. The field is actually placed (projectedBlocks non-empty) — no speeding up
        //      while the field is being built or has been destroyed.
        //   3. The Fortron reserve meets the same sustained-state threshold as the placement
        //      guards: tank must hold at least one full billing burst after the current cycle,
        //      so the rotor tracks whether the projector is genuinely well-powered.
        if (isActive() && getMode().isPresent() && !this.projectedBlocks.isEmpty()
                && this.fortronStorage.getStoredFortron() >= fortronCost * MFFSConfig.FORTRON_TRANSFER_TICKS) {
            speed *= fortronCost / 8.0F;
        }
        return Math.min(300, speed);
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
    public int getBaseFortronTankCapacity() {
        return MFFSConfig.projectorInitialTankCapacity;
    }

    @Override
    protected int getCapacityBoostPerModule() {
        return MFFSConfig.projectorTankCapacityPerModule;
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
            // Bill maintenance cost as a burst, aligned with the network transfer window.
            // Same total drain as per-tick but in sync with when Fortron actually moves.
            if (getTicks() % MFFSConfig.FORTRON_TRANSFER_TICKS == 0) {
                this.fortronStorage.extractFortron(fortronCost * MFFSConfig.FORTRON_TRANSFER_TICKS, false);
            }

            if (getTicks() % MFFSConfig.projectionCycleTicks == 0) {
                // One-shot orphan detection: runs once per load after the async calculation first
                // completes. Diffs previously-projected positions against the new field to enqueue
                // only genuine orphans (e.g. after a field-size change between sessions).
                if (!this.savedProjectedBlocks.isEmpty() && this.semaphore.isComplete(ProjectionStage.CALCULATING)) {
                    Set<BlockPos> newField = StreamEx.of(getCalculatedFieldPositions())
                        .map(TargetPosPair::pos)
                        .toSet();
                    // Collect orphans into a list and shuffle before queuing so the drain order
                    // is scattered rather than following the hash-bucket order of the saved set.
                    List<BlockPos> orphansToQueue = new ArrayList<>();
                    for (BlockPos old : this.savedProjectedBlocks) {
                        if (!newField.contains(old)) {
                            orphansToQueue.add(old);
                        } else if (this.world.getBlockState(old).getBlock() == ModBlocks.FORCE_FIELD) {
                            // Block is still valid in the new field. Restore it to projectedBlocks
                            // so in-place operations (e.g. glow module changes via refreshFieldLights)
                            // can find it without waiting for a full softDestroy/rebuild cycle.
                            // projectField() won't add these itself because existing FF blocks
                            // (not in pendingRemoval) return canProject=false and are never selected.
                            this.projectedBlocks.add(old);
                        }
                    }
                    Collections.shuffle(orphansToQueue);
                    this.pendingRemoval.addAll(orphansToQueue);
                    this.savedProjectedBlocks.clear();
                }

                // Diff-based transition: the async CALCULATING stage has completed while a
                // soft-destroy snapshot is pending.  Apply the diff (on the server thread),
                // then chain into SELECTING so only genuinely-new positions are evaluated.
                if (this.pendingDiffSnapshot != null && this.semaphore.isComplete(ProjectionStage.CALCULATING)) {
                    applyFieldDiff();
                    runSelectionTask().exceptionally(throwable -> {
                        MFFSMod.LOGGER.error("Error selecting force field blocks after diff", throwable);
                        return null;
                    });
                } else if (this.semaphore.isInStage(ProjectionStage.STANDBY)) {
                    reCalculateForceField();
                } else if (this.semaphore.isReady() && this.semaphore.isComplete(ProjectionStage.SELECTING)
                        && this.fortronStorage.getStoredFortron() >= fortronCost * MFFSConfig.FORTRON_TRANSFER_TICKS) {
                    // Only project when the tank still holds a full billing burst's worth of Fortron
                    // after the current burst payment. This ensures the field can sustain through the
                    // next billing window even if the capacitor hasn't distributed yet.
                    projectField();
                }

                // Gap-fill sweep: walk every position in the calculated field geometry and fill
                // any that are now projectable (air/liquid/replaceable) but missing a force field
                // block. This handles positions that were blocked on initial projection (e.g. the
                // field formed half-submerged in terrain) and were later exposed by digging.
                // Gated behind MFFSConfig.enableFastFill (default false) — disabled by default
                // as it scans the full field list every cycle and is only beneficial in PvP.
                if (MFFSConfig.enableFastFill && !this.calculatedFieldSet.isEmpty()) {
                    fillGaps();
                }

            }

            if (getTicks() % (2 * 20) == 0 && !hasModule(ModModules.SILENCE)
                    && this.fortronStorage.getStoredFortron() >= fortronCost * MFFSConfig.FORTRON_TRANSFER_TICKS) {
                // Only play the field sound when in a fully sustained state (tank holds at least
                // one more billing burst). In low-power mode the projector runs silently so players
                // have an audio cue that power is marginal.
                // 1.21.x: SoundSource.BLOCKS → SoundCategory.BLOCKS
                this.world.playSound(null, this.pos, ModSounds.FIELD, SoundCategory.BLOCKS, 0.4F, 1 - this.world.rand.nextFloat() * 0.1F);
            }
        } else {
            // Soft power-off: on the first tick we notice power is gone, queue all projected
            // blocks for gradual removal via the drain loop below.  Subsequent ticks are no-ops
            // until all blocks drain.  Hard destroyField() is reserved for block removal only.
            if (!this.projectedBlocks.isEmpty()) {
                this.pendingDiffSnapshot = null;
                this.semaphore.reset();
                this.projectionCache.invalidateAll();
                this.calculatedFieldSet = Collections.emptySet();
                List<BlockPos> powerOffRemoval = new ArrayList<>(this.projectedBlocks);
                Collections.shuffle(powerOffRemoval);
                this.pendingRemoval.addAll(powerOffRemoval);
                this.projectedBlocks.clear();
                this.pendingCamoRefresh.clear();
            }
        }

        // Drain queued block removals regardless of power state — handles both module-change
        // soft-destroys (while powered) and gradual power-off removals (while unpowered).
        if (getTicks() % MFFSConfig.projectionCycleTicks == 0 && !this.pendingRemoval.isEmpty()) {
            int speed = MFFSConfig.baseProjectionSpeed + MFFSConfig.speedModuleFactor * (getModuleCount(ModModules.SPEED, getUpgradeSlots()) / MFFSConfig.drainSpeedFactor);
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

        // Drain queued camo visual updates at the same rate as block removals.
        // Gradual delivery avoids a single-tick packet burst when the field is large.
        if (getTicks() % MFFSConfig.projectionCycleTicks == 0 && !this.pendingCamoRefresh.isEmpty()) {
            int camoSpeed = getProjectionSpeed();
            int sent = 0;
            double camoRadius = computeFieldSendRadius();
            Iterator<BlockPos> camoIt = this.pendingCamoRefresh.iterator();
            while (camoIt.hasNext() && sent < camoSpeed) {
                BlockPos camoPos = camoIt.next();
                camoIt.remove();
                net.minecraft.tileentity.TileEntity camoTe = this.world.getTileEntity(camoPos);
                if (camoTe instanceof ForceFieldBlockEntity be) {
                    Network.sendToAllAround(new UpdateBlockEntityPacket(camoPos, be.getCustomUpdateTag()),
                        this.world, this.pos, camoRadius);
                }
                sent++;
            }
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
        // Re-check the projector block's own light level (e.g. projector emits level 10 when
        // active with a mode present).  Glow Module changes on already-projected field blocks are
        // handled in-place via refreshFieldLights() triggered from the upgrade-slot callback.
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
                // Stop placing when Fortron would drop below the full-cycle reserve threshold.
                // This ensures that after this projection sweep, enough Fortron remains to cover
                // every maintenance tick until the next projection cycle fires, preventing the
                // place-then-destroy oscillation seen with marginal power supplies.
                // Reclaim paths spend no Fortron and are always safe to continue.
                if (this.fortronStorage.getStoredFortron() <= getFortronCost() * Math.max(MFFSConfig.projectionCycleTicks, 11)) break fieldLoop;
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
                    // Send from projector position so players near the projector receive updates
                    // for all FF blocks regardless of each block's distance from the player.
                    Network.sendToAllAround(new UpdateBlockEntityPacket(pos, be.getCustomUpdateTag()),
                        this.world, this.pos, computeFieldSendRadius());
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
        // This method is called from the background projection-cache loader thread via
        // selectProjectablePositions(). Only test set membership here (synchronized set,
        // thread-safe). The real ownership check (getProjectorPos) happens in projectField()
        // on the server thread before any block is actually reclaimed or replaced.
        boolean isOwnField = state.getBlock() == ModBlocks.FORCE_FIELD && this.pendingRemoval.contains(pos);
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
     * Called when a face-slot field module changes (translation, rotation, scale, etc.).
     * Face slots only accept {@link dev.su5ed.mffs.api.module.Module.Category#FIELD} modules,
     * all of which affect field geometry.  The diff-based rebuild handles camouflage and
     * light updates for unchanged blocks, so no immediate visual refresh is needed.
     */
    private void onFieldModuleChanged() {
        softDestroyField();
    }

    /**
     * Pushes a fresh {@link UpdateBlockEntityPacket} (clientBlockLight + camouflage)
     * to every currently-projected block without rebuilding the field.
     * Updates camouflage on each block entity first so the packet reflects the
     * current state (e.g. null when the camo module was just removed).
     */
    private void refreshFieldVisuals() {
        if (this.world == null || this.world.isRemote) return;
        double radius = computeFieldSendRadius();
        for (BlockPos pos : new HashSet<>(this.projectedBlocks)) {
            net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
            if (te instanceof ForceFieldBlockEntity be) {
                // Recompute camouflage — returns null when camo module is absent
                IBlockState newCamo = getCamoBlock(new net.minecraft.util.math.Vec3d(pos.getX(), pos.getY(), pos.getZ()));
                be.setCamouflage(newCamo);
                Network.sendToAllAround(new UpdateBlockEntityPacket(pos, be.getCustomUpdateTag()),
                    this.world, this.pos, radius);
            }
        }
    }

    /**
     * Pushes a fresh {@link UpdateBlockEntityPacket} containing the current
     * {@code clientBlockLight} value to every currently-projected block without
     * touching camouflage or rebuilding the field.  Called when only the Glow
     * Module count has changed (Case 2: in-place light update).
     *
     * <p>If the projector is off ({@code projectedBlocks} is empty) this is a no-op;
     * correct light values will be sent during the next {@code projectField()} pass.
     */
    private void refreshFieldLights() {
        if (this.world == null || this.world.isRemote) return;
        double radius = computeFieldSendRadius();
        for (BlockPos pos : new HashSet<>(this.projectedBlocks)) {
            net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
            if (te instanceof ForceFieldBlockEntity be) {
                Network.sendToAllAround(new UpdateBlockEntityPacket(pos, be.getCustomUpdateTag()),
                    this.world, this.pos, radius);
            }
        }
    }

    /**
     * Returns the radius to use for {@link Network#sendToAllAround} when pushing
     * per-block TE updates ({@code clientBlockLight}, camouflage).  Uses the
     * projector's own position as the center so a single radius value covers ALL
     * projected blocks, regardless of how far they are from the player.
     *
     * <p>The radius is: max distance from projector to any currently-projected
     * block + 64 (a generous player-interaction buffer).  Falls back to 128 when
     * {@code projectedBlocks} is empty (e.g. called during initial field build).
     */
    private double computeFieldSendRadius() {
        double maxDistSq = this.projectedBlocks.stream()
            .mapToDouble(this.pos::distanceSq)
            .max()
            .orElse(0);
        return Math.sqrt(maxDistSq) + 64;
    }

    private void onModeChanged(ItemStack stack) {
        // 1.21.x: this.level.getLightEngine().checkBlock(this.worldPosition)
        if (this.world != null) {
            this.world.checkLight(this.pos);
        }
        softDestroyField();
    }

    /**
     * Soft destroy: starts a diff-based field transition.  The current projected field stays
     * visible while a new field geometry is calculated asynchronously.  When the calculation
     * completes, {@link #applyFieldDiff()} compares old vs new positions and only touches
     * blocks that actually changed:
     * <ul>
     *   <li><b>toRemove</b> (old − new): queued to {@code pendingRemoval} for gradual drain</li>
     *   <li><b>unchanged</b> (old ∩ new): kept in place; camouflage refreshed if needed</li>
     *   <li><b>toAdd</b> (new − old): handled by the normal SELECTING → PROJECTING pipeline</li>
     * </ul>
     */
    private void softDestroyField() {
        if (this.world != null && !this.world.isRemote) {
            // Snapshot the current field for diffing after the async recalculation.  Include any
            // savedProjectedBlocks that haven't been diffed yet (e.g. a module change right after
            // load before the one-shot orphan detection ran).
            // Always set pendingDiffSnapshot (even when empty) so the tick-loop condition
            // "pendingDiffSnapshot != null && CALCULATING done" fires correctly when starting
            // from an empty-field state (e.g. adding a shape module from scratch, or adjusting
            // scale while the field has not yet been projected).  applyFieldDiff() handles an
            // empty old-field safely: no removals, no camo refresh, then runSelectionTask().
            Set<BlockPos> snapshot = new HashSet<>(this.projectedBlocks);
            if (!this.savedProjectedBlocks.isEmpty()) {
                snapshot.addAll(this.savedProjectedBlocks);
                this.savedProjectedBlocks.clear();
            }
            this.pendingDiffSnapshot = snapshot;
        }
        this.projectionCache.invalidateAll();
        this.semaphore.reset();
        // Start async calculation of the new field geometry.  Do NOT clear projectedBlocks —
        // the field stays visible while we wait.  applyFieldDiff() will reconcile later.
        if (getMode().isPresent()) {
            if (getModeStack().getItem() instanceof ObjectCache cache) {
                cache.clearCache();
            }
            runCalculationTask().exceptionally(throwable -> {
                MFFSMod.LOGGER.error("Error calculating force field during soft destroy", throwable);
                return null;
            });
        }
    }

    /**
     * Applies the diff between the old projected field (snapshot) and the newly-calculated
     * field geometry.  Called on the server thread once the async CALCULATING stage completes
     * and {@link #pendingDiffSnapshot} is non-null.
     *
     * <ul>
     *   <li><b>toRemove</b>: lights zeroed (if glow active), queued to {@code pendingRemoval},
     *       removed from {@code projectedBlocks}.</li>
     *   <li><b>unchanged</b>: camouflage updated if it differs from what {@link #getCamoBlock}
     *       now returns; otherwise left completely untouched.</li>
     *   <li><b>toAdd</b>: handled by the subsequent SELECTING → PROJECTING pipeline.</li>
     * </ul>
     */
    private void applyFieldDiff() {
        Set<BlockPos> oldField = this.pendingDiffSnapshot;
        this.pendingDiffSnapshot = null;

        // Build lookup from the new calculated field: pos → original Vec3d (for camo lookups).
        Collection<TargetPosPair> newFieldPairs = getCalculatedFieldPositions();
        Map<BlockPos, Vec3d> newFieldMap = new HashMap<>(newFieldPairs.size() * 2, 0.5f);
        for (TargetPosPair pair : newFieldPairs) {
            newFieldMap.put(pair.pos(), pair.original());
        }

        // Partition the old field into toRemove and unchanged.
        Set<BlockPos> toRemove = new HashSet<>();
        Set<BlockPos> unchanged = new HashSet<>();
        for (BlockPos pos : oldField) {
            if (newFieldMap.containsKey(pos)) {
                unchanged.add(pos);
            } else {
                toRemove.add(pos);
            }
        }

        // Shuffle toRemove before queuing so the drain order is scattered, matching the
        // random placement order used during the build phase (runCalculationTask shuffles).
        List<BlockPos> toRemoveList = new ArrayList<>(toRemove);
        Collections.shuffle(toRemoveList);

        // Zero lights only on blocks being removed.
        if (!toRemoveList.isEmpty() && getModuleCount(ModModules.GLOW) > 0) {
            zeroFieldBlockLights(toRemove);
        }

        // Queue removals and update projectedBlocks.
        this.pendingRemoval.addAll(toRemoveList);
        this.projectedBlocks.removeAll(toRemove);

        // Invalidate cache for removed positions so gap-fill doesn't see stale state.
        for (BlockPos pos : toRemove) {
            this.projectionCache.invalidate(pos);
        }

        // Refresh camouflage on unchanged blocks (handles camo slot changes, custom mode
        // position-dependent camo, and glow module changes that accompany geometry changes).
        // Server state is updated immediately; client packets are queued for rate-limited delivery.
        // Collect into a list and shuffle before queuing so the drain order is scattered,
        // matching the random placement order used during the build phase — prevents the
        // visible "slice" artifact that appears when HashSet iteration order is used directly.
        if (!unchanged.isEmpty()) {
            List<BlockPos> camoToRefresh = new ArrayList<>();
            for (BlockPos pos : unchanged) {
                net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
                if (te instanceof ForceFieldBlockEntity be) {
                    Vec3d original = newFieldMap.get(pos);
                    IBlockState newCamo = getCamoBlock(original);
                    IBlockState oldCamo = be.getCamouflage();
                    if (!Objects.equals(oldCamo, newCamo)) {
                        be.setCamouflage(newCamo);
                        camoToRefresh.add(pos);
                    }
                }
            }
            Collections.shuffle(camoToRefresh);
            this.pendingCamoRefresh.addAll(camoToRefresh);
        }
    }

    /**
     * Sends {@code clientBlockLight=0} to every force field block in {@code positions}.
     * Called before a soft-destroy to prevent orphan glow on blocks sitting in the
     * pending-removal queue.  Reclaimed blocks receive a corrected value during the
     * next {@code projectField()} pass.
     */
    private void zeroFieldBlockLights(Set<BlockPos> positions) {
        if (this.world == null || this.world.isRemote || positions.isEmpty()) return;
        double radius = computeFieldSendRadius();
        for (BlockPos pos : new HashSet<>(positions)) {
            net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
            if (te instanceof ForceFieldBlockEntity be) {
                NBTTagCompound zeroTag = be.getCustomUpdateTag();
                zeroTag.setInteger("clientBlockLight", 0);
                Network.sendToAllAround(new UpdateBlockEntityPacket(pos, zeroTag), this.world, this.pos, radius);
            }
        }
    }

    @Override
    public void destroyField() {
        // Hard destroy: immediately remove everything (Fortron loss, block break, etc.).
        // Also flush any blocks pending from a prior soft destroy and cancel any in-flight diff.
        this.pendingDiffSnapshot = null;
        Set<BlockPos> alsoRemove = new HashSet<>(this.pendingRemoval);
        // Include actually-placed blocks so breaking a projector mid-transition doesn't orphan them.
        alsoRemove.addAll(this.projectedBlocks);
        // Include NBT-restored blocks that haven't been reconciled with the new field yet.
        alsoRemove.addAll(this.savedProjectedBlocks);
        this.pendingRemoval.clear();
        this.pendingCamoRefresh.clear();
        this.savedProjectedBlocks.clear();
        Collection<TargetPosPair> fieldPositions = getCalculatedFieldPositions();
        this.calculatedFieldSet = Collections.emptySet();
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
        NBTTagList projectedList = new NBTTagList();
        for (BlockPos pos : this.projectedBlocks) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", pos.getX());
            entry.setInteger("y", pos.getY());
            entry.setInteger("z", pos.getZ());
            projectedList.appendTag(entry);
        }
        compound.setTag("projectedBlocks", projectedList);
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
        // Restore previously-projected positions for the one-shot orphan diff in tickServer.
        // These are NOT put into pendingRemoval here — tickServer diffs them against the
        // freshly-calculated field and only enqueues genuine orphans.
        this.savedProjectedBlocks.clear();
        if (compound.hasKey("projectedBlocks")) {
            NBTTagList projectedList = compound.getTagList("projectedBlocks", 10);
            for (int i = 0; i < projectedList.tagCount(); i++) {
                NBTTagCompound entry = projectedList.getCompoundTagAt(i);
                this.savedProjectedBlocks.add(new BlockPos(entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z")));
            }
        }
    }

    @Override
    public int getProjectionSpeed() {
        return MFFSConfig.baseProjectionSpeed + MFFSConfig.speedModuleFactor * getModuleCount(ModModules.SPEED, getUpgradeSlots());
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
            .findFirst();
    }

    @Nullable
    private IBlockState getWeightedCamoBlockFromNeighbors() {
        Map<IBlockState, Integer> neighborsInventory = checkNeighbors();
        if (neighborsInventory.isEmpty()) {
            return null;
        }

        List<IBlockState> weightedList = neighborsInventory.entrySet()
            .stream()
            .flatMap(e -> Collections.nCopies(e.getValue(), e.getKey()).stream())
            .collect(Collectors.toList());

        int random = ThreadLocalRandom.current().nextInt(weightedList.size());
        return weightedList.get(random);
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
                    // Rebuild the fast-lookup set and publish atomically so onSetBlock can do
                    // O(1) membership checks without touching the full TargetPosPair list.
                    HashSet<BlockPos> newSet = new HashSet<>(list.size() * 2, 0.5f);
                    for (TargetPosPair pair : list) newSet.add(pair.pos());
                    this.calculatedFieldSet = Collections.unmodifiableSet(newSet);
                }
            })
            .exceptionally(throwable -> {
                MFFSMod.LOGGER.error("Error calculating force field", throwable);
                return Collections.emptyList();
            });
    }

    /**
     * Gap-fill sweep: iterates the full calculated field geometry and projects any position
     * that is currently projectable (air / liquid / replaceable) but not yet occupied by our
     * force field. This is the counterpart to the initial construction pass — it catches
     * positions that were blocked when the field first formed (e.g. terrain half-submerged)
     * and fills them as soon as they are dug out.
     *
     * Unlike the async selection path this runs entirely on the main thread, so it reads
     * current world state directly rather than going through the projection cache. The cache
     * is still invalidated for each placed block so the async selection stays coherent.
     *
     * Respects the projector speed limit so speed upgrade modules remain meaningful.
     */
    private void fillGaps() {
        IBlockState ffState = ModBlocks.FORCE_FIELD.getDefaultState();
        Set<Module> modules = getModuleInstances();
        for (Module module : modules) {
            module.beforeProject(this);
        }
        int speed = getProjectionSpeed();
        int placed = 0;
        fieldLoop:
        for (TargetPosPair pair : getCalculatedFieldPositions()) {
            if (placed >= speed) break;
            BlockPos pos = pair.pos();
            if (!this.world.isBlockLoaded(pos)) continue;
            // Read current world state directly — always current, no cache race.
            IBlockState current = this.world.getBlockState(pos);
            // Skip positions already holding our force field.
            if (current.getBlock() == ModBlocks.FORCE_FIELD) continue;
            // Also skip positions tracked as projected (prevents re-placing during soft destroy).
            if (this.projectedBlocks.contains(pos)) continue;
            boolean projectable = (current.getBlock().isAir(current, this.world, pos)
                || current.getMaterial().isLiquid()
                || ModTags.getForceFieldReplaceable().contains(current.getBlock())
                || (hasModule(ModModules.DISINTEGRATION) && current.getBlockHardness(this.world, pos) != -1))
                && !pos.equals(this.pos);
            if (!projectable) continue;
            for (Module m : modules) {
                Module.ProjectAction action = m.onProject(this, pos);
                if (action == Module.ProjectAction.SKIP) continue fieldLoop;
                if (action == Module.ProjectAction.INTERRUPT) return;
            }
            if (!canConsumeFieldCost(1)) return;
            this.world.setBlockState(pos, ffState, 0);
            net.minecraft.tileentity.TileEntity te = this.world.getTileEntity(pos);
            if (te instanceof ForceFieldBlockEntity be) {
                be.setProjector(this.pos);
                be.setCamouflage(getCamoBlock(pair.original()));
            }
            this.world.notifyBlockUpdate(pos, ffState, ffState, 3);
            this.fortronStorage.extractFortron(1, false);
            this.pendingRemoval.remove(pos);
            this.projectedBlocks.add(pos);
            this.projectionCache.invalidate(pos);
            placed++;
        }
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
        int constructionSpeed = getProjectionSpeed();
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

    // 1.21.x: getFilterBlock(ItemStack) returns Optional<BlockState> (preserving item metadata)
    // 1.21.x: BlockItem blockItem → ItemBlock blockItem (1.12.2 name)
    // 1.21.x: block.defaultBlockState().getRenderShape() != RenderShape.INVISIBLE
    //   → block.getRenderType(block.getDefaultState()) != EnumBlockRenderType.INVISIBLE
    public static Optional<IBlockState> getFilterBlock(ItemStack stack) {
        if (stack.getItem() instanceof ItemBlock blockItem) {
            Block block = blockItem.getBlock();
            IBlockState defaultState = block.getDefaultState();
            // Invisible blocks (technical/structural) are never valid camo
            if (block.getRenderType(defaultState) == net.minecraft.util.EnumBlockRenderType.INVISIBLE) {
                return Optional.empty();
            }
            // Tile entity blocks (chests, furnaces, etc.) are rejected — the TESR delegate
            // only handles a handful of known cases and non-cube models look broken.
            if (block.hasTileEntity(defaultState)) {
                return Optional.empty();
            }
            // Only allow geometrically full-cube blocks. Non-cube shapes (stairs, slabs, fences)
            // clip and z-fight badly when rendered as a force field face.
            try {
                if (!Block.FULL_BLOCK_AABB.equals(block.getBoundingBox(defaultState, null, BlockPos.ORIGIN))) {
                    return Optional.empty();
                }
            } catch (Exception ignored) {
                // If bounding box lookup crashes (world-dependent shape), conservatively reject.
                return Optional.empty();
            }
            // Preserve item damage value so colour-carrying meta (e.g. stained glass tint) is kept.
            return Optional.of(block.getStateFromMeta(stack.getMetadata()));
        }
        return Optional.empty();
    }

    public Map<IBlockState, Integer> checkNeighbors() {
        Map<IBlockState, Integer> countMap = new HashMap<>();
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
                            getFilterBlock(stack).ifPresent(state -> countMap.put(state, countMap.getOrDefault(state, 0) + count));
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
