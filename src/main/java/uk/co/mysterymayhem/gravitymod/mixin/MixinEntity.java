package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.asm.Hooks;
import zone.rong.mixinextras.sugar.Local;
import zone.rong.mixinextras.sugar.Share;
import zone.rong.mixinextras.sugar.ref.LocalDoubleRef;


@Mixin(Entity.class)
public abstract class MixinEntity{
    @Shadow public double posY;

    @Shadow public double posZ;

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/AxisAlignedBB;calculateXOffset(Lnet/minecraft/util/math/AxisAlignedBB;D)D"))
    private double redirectXOffset(AxisAlignedBB instance, AxisAlignedBB other, double offset) {
        return Hooks.reverseXOffset(instance, other, offset);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/AxisAlignedBB;calculateYOffset(Lnet/minecraft/util/math/AxisAlignedBB;D)D"))
    private double redirectYOffset(AxisAlignedBB instance, AxisAlignedBB other, double offset) {
        return Hooks.reverseYOffset(instance, other, offset);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/AxisAlignedBB;calculateZOffset(Lnet/minecraft/util/math/AxisAlignedBB;D)D"))
    private double redirectZOffset(AxisAlignedBB instance, AxisAlignedBB other, double offset) {
        return Hooks.reverseZOffset(instance, other, offset);
    }

    @ModifyVariable(method = "move", at = @At(value = "STORE"), index = 71, ordinal = 0)
    private BlockPos redirectNewBlockPos(BlockPos pos) {
        return Hooks.getImmutableBlockPosBelowEntity((Entity)(Object)this);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos redirectDown(BlockPos instance) {
        return Hooks.getRelativeDownBlockPos(instance, (Entity)(Object)this);
    }

    @ModifyVariable(method = "move", at = @At("STORE"), index = 74)
    private double modifyd15(double original, @Local(index = 13) double d11, @Local(index = 15) double d1, @Share("d16") LocalDoubleRef d16r, @Share("d17") LocalDoubleRef d17r) {
        double d15 = original;
        double d16 = this.posY - d11;
        double d17 = this.posZ - d1;
        d15 = Hooks.inverseAdjustXYZ((Entity)(Object)this, d15, d16, d17)[0];
        d16 = Hooks.inverseAdjustXYZ((Entity)(Object)this, d15, d16, d17)[1];
        d17 = Hooks.inverseAdjustXYZ((Entity)(Object)this, d15, d16, d17)[2];
        d16r.set(d16);
        d17r.set(d17);
        return d15;
    }

    @ModifyVariable(method = "move", at = @At(value = "STORE", ordinal = 0), index = 76)
    private double modifyd16(double original, @Share("d16") LocalDoubleRef d16r) {
        return d16r.get();
    }

    @ModifyVariable(method = "move", at = @At(value = "STORE", ordinal = 2), index = 39)
    private double modifyd17(double original, @Share("d17") LocalDoubleRef d17r) {
        return d17r.get();
    }

    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;sin(F)F"))
    private float sinRelativeYaw(float angle) {
        return MathHelper.sin(Hooks.getRelativeYaw((Entity)(Object)this) * 0.017453292F);
    }

    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;cos(F)F"))
    private float cosRelativeYaw(float angle) {
        return MathHelper.cos(Hooks.getRelativeYaw((Entity)(Object)this) * 0.017453292F);
    }
}
