## TODO/IDEAS
Add glow to the biometric block when active.
Add scaling to the custom shape mode.
Interdiction Matrix mode to use in-field instead of its own scale.
    Scale modules could add to blocks outside of field
Expand on the lighting optimizations by dividing the light spacing between is touching block vs force fields touching air
    1 in 3 touching the ground is hard to tell, air might be different, although obvious patterns emerge.
    The lighting updates depend on the blocks fully refreshing, we should find a performant way to improve this.
    Related to how the force field handles updates, we currently have two types, the instant break down, such as when you power down the projector, or the queued update such as when you change the configuration while it remains on. I believe both modes should be supported, however they should have settings when each is invoked.

## Known Bugs
Fix Catalyst functionality
    Add redstone as an option.
Lighting updates slow
    Sometimes it can take longer than expected for current lights to go away.