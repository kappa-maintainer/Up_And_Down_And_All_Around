package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AbstractClientPlayer.class, priority = 0)
public class MixinAbstractClientPlayer {
}
