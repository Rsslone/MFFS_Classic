package dev.su5ed.mffs.util;

// 1.12.2 Backport: removed NeoForge StreamCodec.

/**
 * The force field transfer mode.
 */
public enum TransferMode {
    EQUALIZE,
    DISTRIBUTE,
    DRAIN,
    FILL;

    private static final TransferMode[] VALUES = values();

    public TransferMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /* class_NeoForge_1_21_x (TransferMode):
    import net.minecraft.network.FriendlyByteBuf;
    import net.minecraft.network.codec.StreamCodec;
    import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
    public static final StreamCodec<FriendlyByteBuf, TransferMode> STREAM_CODEC =
        NeoForgeStreamCodecs.enumCodec(TransferMode.class);
    */
}
