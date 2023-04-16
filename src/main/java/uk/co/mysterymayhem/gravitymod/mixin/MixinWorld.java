package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = World.class, priority = 0)
public class MixinWorld {
}
