package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityPlayerSP.class, priority = 0)
public abstract class MixinEntityPlayerSP {}
