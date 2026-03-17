package dev.su5ed.mffs.setup;

// =============================================================================
// 1.12.2 Backport: GUI / Container registration
//
// GUI ID constants are defined here for use by IGuiHandler and block interactions
// (player.openGui(modInstance, GUI_ID, world, x, y, z)).
// =============================================================================

public final class ModMenus {

    // GUI ID constants - used with player.openGui() and in IGuiHandler switch
    public static final int GUI_COERCION_DERIVER     = 0;
    public static final int GUI_FORTRON_CAPACITOR    = 1;
    public static final int GUI_PROJECTOR            = 2;
    public static final int GUI_BIOMETRIC_IDENTIFIER = 3;
    public static final int GUI_INTERDICTION_MATRIX  = 4;

    private ModMenus() {}
}
