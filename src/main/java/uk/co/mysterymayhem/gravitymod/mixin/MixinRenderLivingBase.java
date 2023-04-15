package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.asm.Hooks;

@Mixin(RenderLivingBase.class)
public class MixinRenderLivingBase <T extends EntityLivingBase> {

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevRotationYawHead:F", opcode = Opcodes.GETFIELD))
    private float prevRelativeYaw(T entity) {
        return Hooks.getPrevRelativeYawHead(entity);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYawHead:F", opcode = Opcodes.GETFIELD))
    private float relativeYaw(T entity) {
        return Hooks.getRelativeYawHead(entity);
    }
    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevRotationPitch:F", opcode = Opcodes.GETFIELD))
    private float prevRelativePitch(T entity) {
        return Hooks.getRelativePrevPitch(entity);
    }
    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationPitch:F", opcode = Opcodes.GETFIELD))
    private float relativePitch(T entity) {
        return Hooks.getRelativePitch(entity);
    }
}
