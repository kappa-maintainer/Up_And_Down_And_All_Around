package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityPlayerMP.class, priority = 0)
public class MixinEntityPlayerMP {
}
