package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.asm.Hooks;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;", ordinal = 0))
    private AxisAlignedBB redirectGetBB(Entity instance) {
        return Hooks.getVanillaEntityBoundingBox(instance);
    }

    @Inject(method = "drawNameplate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableLighting()V"))
    private static void drawNameplate(FontRenderer fontRendererIn, String str, float x, float y, float z, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal, boolean isSneaking, CallbackInfo ci) {
        Hooks.runNameplateCorrection(isThirdPersonFrontal);
    }
}
