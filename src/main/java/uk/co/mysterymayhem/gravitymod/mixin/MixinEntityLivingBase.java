package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityLivingBase.class, priority = 0)
public class MixinEntityLivingBase {
}
