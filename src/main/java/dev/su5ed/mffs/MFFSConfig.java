package dev.su5ed.mffs;

// =============================================================================
// 1.12.2 Backport: Configuration
// 1.21.x used NeoForge ModConfigSpec (data-driven, registered at build time).
// In 1.12.2 we use net.minecraftforge.common.config.Configuration, loaded
// during FMLPreInitializationEvent via event.getSuggestedConfigurationFile().
// All values are stored as plain fields; call MFFSConfig.load(configFile)
// from MFFSMod.preInit and MFFSConfig.save() when done.
// =============================================================================

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class MFFSConfig {

    private static Configuration configuration;

    // -------------------------------------------------------------------------
    // General
    // -------------------------------------------------------------------------
    /** Turning this to false will make MFFS run without electricity or energy systems required. */
    public static boolean enableElectricity            = true;
    /** Cache allows temporary data saving to decrease calculations required. */
    public static boolean useCache                     = true;
    /** How many force field blocks can be generated per tick? Less reduces lag. */
    public static int     maxFFGenPerTick              = 1_000_000;
    /** Allow server operators to bypass Force Field biometry. */
    public static boolean allowOpBiometryOverride      = true;
    /** Should the interdiction matrix interact with creative players? */
    public static boolean interactCreative             = true;
    /** Max custom mode field scale. */
    public static int     maxCustomModeScale           = 200;
    /** Give players a copy of the MFFS guidebook when they first join a world. */
    public static boolean giveGuidebookOnFirstJoin     = true;
    /** Disable Steel Ingot and Steel Compound item registration (other mods likely provide these). */
    public static boolean disableSteelItems            = true;

    // -------------------------------------------------------------------------
    // Coercion Deriver
    // -------------------------------------------------------------------------
    /** FE to convert into 1 Fortron. */
    public static int coercionDriverFePerFortron                  = 400;
    /** FE to subtract when converting Fortron to FE. */
    public static int coercionDriverFortronToFeLoss               = 1;
    /** Base limit of Fortron produced per tick. */
    public static int coercionDriverFortronPerTick                = 200;
    /** Production bonus per speed module. */
    public static int coercionDriverFortronPerTickSpeedModule     = 200;

    // -------------------------------------------------------------------------
    // Balance
    // -------------------------------------------------------------------------
    /** Fortron catalyst production multiplier. */
    public static double catalystMultiplier          = 2.0;
    /** The number of ticks a single catalyst item lasts. */
    public static int    catalystBurnTime            = 10 * 20;
    /** Energy to consume when the Interdiction Matrix kills a player. */
    public static int    interdictionMatrixKillEnergy = 0;

    // -------------------------------------------------------------------------
    // Force Field
    // -------------------------------------------------------------------------
    /** Prevent authorized players from taking damage when passing through force fields. */
    public static boolean disableForceFieldDamageForAuthorizedPlayers  = false;
    /** Remove confusion and slowness effects for authorized players passing through force fields. */
    public static boolean disableForceFieldEffectsForAuthorizedPlayers = false;
    /** Allow authorized players to walk through force fields without sneaking. */
    public static boolean allowWalkThroughForceFields                  = false;

    // -------------------------------------------------------------------------
    // Client
    // -------------------------------------------------------------------------
    /** Apply a fancy glitch effect on projector mode renders. */
    public static boolean enableProjectorModeGlitch = true;
    /** Spacing used for force field light sources: 1 = every block, 3 = ~1/3 of blocks emit light. */
    public static int forceFieldLightSpacing = 3;
    /** How many deferred world.checkLight() calls to process per client tick when the Glow Module is active. */
    public static int glowLightChecksPerTick = 50;

    // =========================================================================
    // Load / Save
    // =========================================================================

    /**
     * Load the configuration from disk. Call from {@link MFFSMod#preInit}:
     * <pre>MFFSConfig.load(event.getSuggestedConfigurationFile());</pre>
     */
    public static void load(File configFile) {
        configuration = new Configuration(configFile);
        configuration.load();

        // -- General --
        enableElectricity = configuration.getBoolean("enableElectricity", "general", enableElectricity,
            "Turning this to false will make MFFS run without electricity or energy systems required. Great for vanilla!");
        useCache = configuration.getBoolean("useCache", "general", useCache,
            "Cache allows temporary data saving to decrease calculations required");
        maxFFGenPerTick = configuration.getInt("maxFFGenPerTick", "general", maxFFGenPerTick, 0, Integer.MAX_VALUE,
            "How many force field blocks can be generated per tick? Less reduces lag.");
        allowOpBiometryOverride = configuration.getBoolean("allowOpBiometryOverride", "general", allowOpBiometryOverride,
            "Allow server operators to bypass Force Field biometry");
        interactCreative = configuration.getBoolean("interactCreative", "general", interactCreative,
            "Should the interdiction matrix interact with creative players?");
        maxCustomModeScale = configuration.getInt("maxCustomModeScale", "general", maxCustomModeScale, 0, Integer.MAX_VALUE,
            "Max custom mode field scale");
        giveGuidebookOnFirstJoin = configuration.getBoolean("giveGuidebookOnFirstJoin", "general", giveGuidebookOnFirstJoin,
            "Give players a copy of the MFFS guidebook when they first join a world");
        disableSteelItems = configuration.getBoolean("disableSteelItems", "general", disableSteelItems,
            "Disable Steel Ingot and Steel Compound item registration. Other mods likely provide these items, causing conflicts.");

        // -- Coercion Deriver --
        coercionDriverFePerFortron = configuration.getInt("feCostPerFortron", "coercion_deriver", coercionDriverFePerFortron, 1, Integer.MAX_VALUE,
            "FE to convert into 1 Fortron");
        coercionDriverFortronToFeLoss = configuration.getInt("fortronToFeLoss", "coercion_deriver", coercionDriverFortronToFeLoss, 0, Integer.MAX_VALUE,
            "FE to subtract when converting Fortron to FE");
        coercionDriverFortronPerTick = configuration.getInt("fortronPerTick", "coercion_deriver", coercionDriverFortronPerTick, 1, Integer.MAX_VALUE,
            "Base limit of fortron produced per tick (20 per second). Scales with speed modules and catalyst.");
        coercionDriverFortronPerTickSpeedModule = configuration.getInt("fortronPerTickSpeedModule", "coercion_deriver",
            coercionDriverFortronPerTickSpeedModule, 1, Integer.MAX_VALUE,
            "Production bonus per speed module.");

        // -- Balance --
        catalystMultiplier = configuration.getFloat("catalystMultiplier", "balance", (float) catalystMultiplier, 0f, 10_000f,
            "Fortron catalyst production multiplier");
        catalystBurnTime = configuration.getInt("catalystBurnTime", "balance", catalystBurnTime, 1, 10_000,
            "The amount of ticks a single catalyst item lasts for");
        interdictionMatrixKillEnergy = configuration.getInt("interdictionMatrixKillEnergy", "balance", interdictionMatrixKillEnergy, 0, Integer.MAX_VALUE,
            "Fortron to consume when the Interdiction Matrix kills a player");

        // -- Force Field --
        disableForceFieldDamageForAuthorizedPlayers = configuration.getBoolean("disableForceFieldDamageForAuthorizedPlayers", "force_field",
            disableForceFieldDamageForAuthorizedPlayers,
            "Prevent authorized players from taking damage when passing through force fields");
        disableForceFieldEffectsForAuthorizedPlayers = configuration.getBoolean("disableForceFieldEffectsForAuthorizedPlayers", "force_field",
            disableForceFieldEffectsForAuthorizedPlayers,
            "Remove confusion and slowness effects for authorized players passing through force fields");
        allowWalkThroughForceFields = configuration.getBoolean("allowWalkThroughForceFields", "force_field", allowWalkThroughForceFields,
            "Allow authorized players to walk through force fields without sneaking. WARNING: May cause occasional clipping issues on horizontal platforms.");

        // -- Client (best-effort; Configuration does not distinguish client/common in 1.12.2) --
        enableProjectorModeGlitch = configuration.getBoolean("enableProjectorModeGlitch", "client", enableProjectorModeGlitch,
            "Apply a fancy glitch effect on projector mode renders. Reload resources to apply change.");

        // -- Performance --
        forceFieldLightSpacing = configuration.getInt("forceFieldLightSpacing", "performance", forceFieldLightSpacing, 1, Integer.MAX_VALUE,
            "Controls spacing for force field light sources. 1 = every block emits, 3 = ~1/3 of blocks.");
        glowLightChecksPerTick = configuration.getInt("glowLightChecksPerTick", "performance", glowLightChecksPerTick, 1, Integer.MAX_VALUE,
            "How many deferred world.checkLight() calls to process per client tick when the Glow Module is active. Lower = less lighting stutter on chunk load, however too low may cause lighting issues and artifacts.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static void save() {
        if (configuration != null && configuration.hasChanged()) {
            configuration.save();
        }
    }

    private MFFSConfig() {}
}
