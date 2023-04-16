package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = RenderLivingBase.class, priority = 0)
public class MixinRenderLivingBase {}
