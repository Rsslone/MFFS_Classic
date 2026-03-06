package dev.su5ed.mffs.setup;

// =============================================================================
// 1.12.2 Backport: Item data storage
// DataComponentType<T> is a NeoForge/1.21.x concept and does NOT exist in 1.12.2.
// In 1.12.2 all item data is stored directly as NBT on ItemStack via:
//   - ItemStack.getTagCompound() / setTagCompound(NBTTagCompound)
//   - ItemStack.getOrCreateSubCompound("mffs")
// The fields below document the intended NBT keys for each former DataComponent.
//
// Former DataComponentType fields and their 1.12.2 NBT key equivalents:
//   REMOTE_LINK_POS   -> NBT key "linkX", "linkY", "linkZ" (or packed long)
//   ENERGY            -> NBT key "energy" (int)
//   CARD_FREQUENCY    -> NBT key "frequency" (int)
//   ID_CARD_PROFILE   -> NBT key "profile" (GameProfile serialised via NBT)
//   ID_CARD_PERMISSIONS -> NBT key "permissions" (NBTTagList of FieldPermission names)
//   PATTERN_ID        -> NBT key "patternId" (String)
//   STRUCTURE_COORDS  -> NBT key "structureCoords" (NBTTagCompound)
//   STRUCTURE_MODE    -> NBT key "structureMode" (String enum name)
// =============================================================================

/**
 * Data storage constants for MFFS items (1.12.2 NBT-based approach).
 * Replaces NeoForge DataComponentType registry from 1.21.x.
 */
public final class ModDataComponentTypes {

    // NBT sub-tag root for all MFFS item data
    public static final String ROOT_TAG = "mffs";

    // Remote Controller link position
    public static final String NBT_LINK_POS = "linkPos"; // stored as long (BlockPos.asLong)

    // Battery energy storage
    public static final String NBT_ENERGY = "energy";

    // Frequency Card
    public static final String NBT_CARD_FREQUENCY = "frequency";

    // Identification Card
    public static final String NBT_ID_CARD_PROFILE     = "profile";     // GameProfile NBT
    public static final String NBT_ID_CARD_PERMISSIONS = "permissions"; // NBTTagList of strings

    // Custom Projector Mode
    public static final String NBT_PATTERN_ID       = "patternId";
    public static final String NBT_STRUCTURE_COORDS = "structureCoords";
    public static final String NBT_STRUCTURE_MODE   = "structureMode";

    private ModDataComponentTypes() {}
}
