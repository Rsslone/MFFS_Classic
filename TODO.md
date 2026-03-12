## TODO/IDEAS
Add glow to the biometric block when active.
Add scaling to the custom shape mode.
    Investigate the maxCustomModeScale var.
Interdiction Matrix mode to use in-field instead of its own scale.
    Scale modules could add to blocks outside of field.
Implement updated patchulli book, or entirely remove.
Implement advancments, or entirely remove.
Expose useful values for balance.
    Include module enable settings, so server owners can disable modules they don't like.
Move the balance section from config to their respective zones.
Implement better tooltips.
    Modules with configurations, show the configuration value to user. i.e speed value / cost / ect.

## Optimization
The lighting not making contact with a physical block should have its own independent max value (~3-7).
    Lighting updates higher than this cause cascading light updates.
    Create a variable for this value and place in performance section.

Remove the projectionCycleTicks from the settings, this is not as useful of a setting.
    Look at reference files and hardcode it.
    Could move it to an advanced section.
    **New default of 1 then lower max speeds to smooth FF placements**
    Add a base FF gen rate config.
Investigate useCache var.
    We may have hard coded the cache in my accident... tehe :D


## Known Bugs
Fix Catalyst functionality
    Add redstone and lapis as an option.
    Scale with blocks.
