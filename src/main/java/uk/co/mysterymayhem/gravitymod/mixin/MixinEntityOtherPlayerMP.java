package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityOtherPlayerMP.class, priority = 0)
public class MixinEntityOtherPlayerMP {
}
