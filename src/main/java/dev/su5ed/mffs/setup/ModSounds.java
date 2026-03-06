package dev.su5ed.mffs.setup;

// =============================================================================
// 1.12.2 Backport: Sound registration
// 1.21.x used DeferredRegister<SoundEvent> (NeoForge). In 1.12.2 SoundEvent
// instances are registered via RegistryEvent.Register<SoundEvent> on the Forge
// event bus. The existing sounds.json in assets/mffs/ remains valid in 1.12.2.
// =============================================================================

import dev.su5ed.mffs.MFFSMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = MFFSMod.MODID)
public final class ModSounds {

    public static SoundEvent FIELD;

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        IForgeRegistry<SoundEvent> registry = event.getRegistry();
        FIELD = registerSound(registry, "field");
    }

    private static SoundEvent registerSound(IForgeRegistry<SoundEvent> registry, String name) {
        ResourceLocation rl = new ResourceLocation(MFFSMod.MODID, name);
        SoundEvent sound = new SoundEvent(rl).setRegistryName(rl);
        registry.register(sound);
        return sound;
    }

    private ModSounds() {}
}
