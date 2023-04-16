package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SoundManager.class, priority = 0)
public class MixinSoundManager {
}
