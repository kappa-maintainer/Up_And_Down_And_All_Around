package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.asm.Hooks;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {
    @Redirect(method = "moveRelative", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYaw:F", opcode = Opcodes.GETFIELD))
    private float redirectSin(EntityLivingBase origin) {
        return Hooks.getRelativeYaw((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getLookVec()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d redirectGetLook(EntityLivingBase origin) {
        return Hooks.getRelativeLookVec((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "travel", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationPitch:F", opcode = Opcodes.GETFIELD))
    private float redirectPitch(EntityLivingBase instance) {
        return Hooks.getRelativePitch((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;retain(DDD)Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;"))
    private BlockPos.PooledMutableBlockPos redirectRetain(double xIn, double yIn, double zIn) {
        return Hooks.getBlockPosBelowEntity((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;setPos(DDD)Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;", ordinal = 0))
    private BlockPos.PooledMutableBlockPos redirectSetPos(BlockPos.PooledMutableBlockPos pos, double xIn, double yIn, double zIn) {
        return Hooks.setPooledMutableBlockPosToBelowEntity(pos, (EntityLivingBase)(Object)this);
    }

    @Redirect(method = "travel", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;posY:D", opcode = Opcodes.GETFIELD))
    private double redirectPosY(EntityLivingBase instance) {
        return Hooks.getRelativePosY((EntityLivingBase)(Object)this);
    }

    @Inject(method = "travel", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevLimbSwingAmount:F", ordinal = 0))
    private void makeRelative(float strafe, float vertical, float forward, CallbackInfo ci) {
        Hooks.makePositionRelative((EntityLivingBase)(Object)this);
    }
    @Inject(method = "travel", at = @At("RETURN"))
    private void makeAbsolute(float strafe, float vertical, float forward, CallbackInfo ci) {
        Hooks.makePositionAbsolute((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "updateDistance", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYaw:F", opcode = Opcodes.GETFIELD))
    private float redirectYaw(EntityLivingBase instance) {
        return Hooks.getRelativeYaw((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "onUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;posX:D", opcode = Opcodes.GETFIELD))
    private double redirectPosX(EntityLivingBase instance) {
        return Hooks.getRelativePosX((EntityLivingBase)(Object)this);
    }

    @Redirect(method = "onUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevPosX:D", opcode = Opcodes.GETFIELD))
    private double redirectPrevPosX(EntityLivingBase instance) {
        return Hooks.getRelativePrevPosX((EntityLivingBase)(Object)this);
    }
    @Redirect(method = "onUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;posZ:D", opcode = Opcodes.GETFIELD))
    private double redirectPosZ(EntityLivingBase instance) {
        return Hooks.getRelativePosZ((EntityLivingBase)(Object)this);
    }
    @Redirect(method = "onUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevPosZ:D", opcode = Opcodes.GETFIELD))
    private double redirectPrevPosZ(EntityLivingBase instance) {
        return Hooks.getRelativePrevPosZ((EntityLivingBase)(Object)this);
    }
    @Redirect(method = "onUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYaw:F", opcode = Opcodes.GETFIELD, ordinal = 0))
    private float redirectRotationYaw(EntityLivingBase instance) {
        return Hooks.getRelativeYaw((EntityLivingBase)(Object)this);
    }
}
