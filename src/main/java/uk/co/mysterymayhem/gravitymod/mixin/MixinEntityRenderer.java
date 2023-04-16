package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityRenderer.class, priority = 0)
public class MixinEntityRenderer {}
