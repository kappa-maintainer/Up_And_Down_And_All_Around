package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(value = Entity.class, priority = 0)
public abstract class MixinEntity{
}
