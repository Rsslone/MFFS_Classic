## TODO/IDEAS
Add glow to the biometric block when active.
Change localized L/s to F/s, we don't use fluids, fortron is a simple int value.
Add scaling to the custom shape mode.
    Investigate the maxCustomModeScale var.
Interdiction Matrix mode to use in-field instead of its own scale.
    Scale modules could add to blocks outside of field.
    Min scale modules could be required.
    Option could be from center of force field projection.
        Might not apply to custom shapes well.
    Option to disable mob drops from interdiction kills without collection module.
Implement updated patchulli book, or entirely remove.
Implement advancments, or entirely remove.
Expose useful values for balance.
    Implement base capacity and F/s rates.
    Implement Capacity and Speed module modifier rates.
    Implement Module Cost (Warning : Could break the balance)
    Implement Shock Module Damage Configuration
    Include module enable settings, so server owners can disable modules they don't like.
Move the balance section from config to their respective zones.
Implement better tooltips.
    Modules with configurations, show the configuration value to user. i.e speed value / cost / ect.
Increase projector max rotor speed ceiling, it spins too fast too soon.
    Decrease the idle speed a bit more.
Add better on/off feedback
    Show tooltip or wording.
Tooltips rendered over HEI
Add green border to Arrows
Refreshing of shape from null fails.
Check collection and confiscation modules pair?

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
Camo modules probably shouldn't be instant updates. (queue is too fast actually)
    Renders in slices, previous issue.
    Check performance on larger objects.


## Known Bugs
Fix Catalyst functionality
    Add redstone and lapis as an option.
    Scale with blocks.
Interdiction warnings can stack upon each other, implement some form of limiter, or change how its rendered entirely.
Look into rendering of power cables not showing emittions on deriver.
    Draining cable too fast?
When you access the remote, the fortron particle points to the player.
    Increase range limit on remote
Pyramid on smaller sizes contains holes, scale should be increased
    Probably needs to be rotates by default 45 degrees
Holes on rotation module, totally broken.
Guard against tile entities.
Recalculation between low power states might have issues.
Calculation of Fortron between tick rates need to be double checked.
Items phase through bottoms of force fields
Check disentigration module.
    Particle needs to be addressed for culling similar to fortron particle.
    Between adding and removing module, recalculation does not occur.