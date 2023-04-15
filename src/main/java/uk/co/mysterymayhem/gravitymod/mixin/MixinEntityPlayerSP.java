package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.asm.EntityPlayerWithGravity;
import uk.co.mysterymayhem.gravitymod.asm.Hooks;
import uk.co.mysterymayhem.gravitymod.common.util.boundingboxes.GravityAxisAlignedBB;
import zone.rong.mixinextras.sugar.Local;
import zone.rong.mixinextras.sugar.Share;
import zone.rong.mixinextras.sugar.ref.LocalBooleanRef;
import zone.rong.mixinextras.sugar.ref.LocalDoubleRef;

import java.util.List;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    @Shadow private int autoJumpTime;

    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }


    @Shadow
    private boolean isOpenBlockSpace(BlockPos pos) {return false;};

    @Shadow protected abstract void updateAutoJump(float p_189810_1_, float p_189810_2_);

    @Shadow public abstract boolean isAutoJumpEnabled();

    @Shadow public MovementInput movementInput;

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/util/math/AxisAlignedBB;minY:D", opcode = Opcodes.GETFIELD))
    private double redirectMinY(AxisAlignedBB instance) {
        return this.posY;
    }

    @Inject(method = "isHeadspaceFree", at = @At("HEAD"), cancellable = true, remap = false)
    private void newIsHeadSpaceFree(BlockPos pos, int height, CallbackInfoReturnable<Boolean> cir) {
        for(int y = 0; y < height; ++y) {
            if (!this.isOpenBlockSpace(pos.add(0, y, 0))) {
                cir.setReturnValue(false);
            }
        }
        cir.setReturnValue(true);
    }

    @Redirect(method = "Lnet/minecraft/client/entity/EntityPlayerSP;pushOutOfBlocks(DDD)Z", at = @At(value = "NEW"))
    private BlockPos hijackBlockPos(double x, double y, double z) {
        return Hooks.makeRelativeBlockPos(new BlockPos(x, y, z), this);
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/PlayerSPPushOutOfBlocksEvent;<init>(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/AxisAlignedBB;)V"))
    private void callPushHooks(CallbackInfo ci, @Local AxisAlignedBB axisAlignedBB, @Share("isGravity")LocalBooleanRef isGravity) {
        boolean ig = axisAlignedBB instanceof GravityAxisAlignedBB;
        isGravity.set(ig);
        if (ig) {
            Hooks.pushEntityPlayerSPOutOfBlocks((EntityPlayerWithGravity)(Object)this, axisAlignedBB);
        }
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"))
    private boolean cancelPost(EventBus instance, Event event, @Share("isGravity")LocalBooleanRef isGravity) {
        if (isGravity.get()) {
            return false;
        }
        return instance.post(event);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;move(Lnet/minecraft/entity/MoverType;DDD)V"))
    private void beforeSuperMove(MoverType type, double x, double y, double z, CallbackInfo ci, @Share("d0")LocalDoubleRef d0, @Share("d1")LocalDoubleRef d1) {
        d0.set(Hooks.inverseAdjustXYZ(this, this.posX, this.posY, this.posZ)[0]);
        d1.set(Hooks.inverseAdjustXYZ(this, this.posX, this.posY, this.posZ)[2]);
    }
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;move(Lnet/minecraft/entity/MoverType;DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void afterSuperMove(MoverType type, double x, double y, double z, CallbackInfo ci, @Share("d0")LocalDoubleRef d0, @Share("d1")LocalDoubleRef d1) {
        double d2 = Hooks.inverseAdjustXYZ(this, this.posX, this.posY, this.posZ)[2] - d1.get();
        updateAutoJump((float)(Hooks.inverseAdjustXYZ(this, this.posX, this.posY, this.posZ)[0] - d0.get()), (float)d2);
        ci.cancel();
    }

    @Inject(method = "updateAutoJump", at = @At("HEAD"), cancellable = true)
    private void replaceAutoJump(float p_189810_1_, float p_189810_2_, CallbackInfo ci) {
        if (isAutoJumpEnabled())
            if (this.autoJumpTime <= 0 && this.onGround && !isSneaking() && !isRiding()) {
                Vec2f vec2f = this.movementInput.getMoveVector();
                if (vec2f.x != 0.0F || vec2f.y != 0.0F) {
                    Vec3d vec3d = Hooks.getBottomOfEntity(this);
                    double d0 = Hooks.getOriginRelativePosX(this) + p_189810_1_;
                    double d1 = Hooks.getOriginRelativePosZ(this) + p_189810_2_;
                    Vec3d vec3d1 = Hooks.adjustVec(new Vec3d(d0, Hooks.getOriginRelativePosY(this), d1), this);
                    Vec3d vec3d2 = new Vec3d(p_189810_1_, 0.0D, p_189810_2_);
                    float f = getAIMoveSpeed();
                    float f1 = (float)vec3d2.lengthSquared();
                    if (f1 <= 0.001F) {
                        float f2 = f * vec2f.x;
                        float f3 = f * vec2f.y;
                        float f4 = MathHelper.sin(Hooks.getRelativeYaw(this) * 0.017453292F);
                        float f5 = MathHelper.cos(Hooks.getRelativeYaw(this) * 0.017453292F);
                        vec3d2 = new Vec3d((f2 * f5 - f3 * f4), vec3d2.y, (f3 * f5 + f2 * f4));
                        f1 = (float)vec3d2.lengthSquared();
                        if (f1 <= 0.001F)
                            return;
                    }
                    float f12 = (float)MathHelper.fastInvSqrt(f1);
                    Vec3d vec3d12 = vec3d2.scale(f12);
                    Vec3d vec3d13 = Hooks.getRelativeLookVec((Entity)this);
                    float f13 = (float)(vec3d13.x * vec3d12.x + vec3d13.z * vec3d12.z);
                    if (f13 >= -0.15F) {
                        BlockPos blockPos = Hooks.getBlockPosAtTopOfPlayer((Entity)this);
                        vec3d12 = Hooks.adjustVec(vec3d12, (Entity)this);
                        IBlockState iblockstate = this.world.getBlockState(blockPos);
                        if (iblockstate.getCollisionBoundingBox((IBlockAccess)this.world, blockPos) == null) {
                            BlockPos blockpos = Hooks.getRelativeUpBlockPos(blockPos, (Entity)this);
                            IBlockState iblockstate1 = this.world.getBlockState(blockpos);
                            if (iblockstate1.getCollisionBoundingBox((IBlockAccess)this.world, blockpos) == null) {
                                float f6 = 7.0F;
                                float f7 = 1.2F;
                                if (isPotionActive(MobEffects.JUMP_BOOST))
                                    f7 += (getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
                                float f8 = Math.max(f * 7.0F, 1.0F / f12);
                                Vec3d vec3d4 = vec3d1.add(vec3d12.scale(f8));
                                float f9 = this.width;
                                float f10 = this.height;
                                AxisAlignedBB axisalignedbb = Hooks.constructNewGAABBFrom2Vec3d(vec3d, Hooks.addAdjustedVector(vec3d4, 0.0D, f10, 0.0D, (Entity)this), (Entity)this).grow(f9, 0.0D, f9);
                                Vec3d lvt_19_1_ = Hooks.addAdjustedVector(vec3d, 0.0D, 0.5099999904632568D, 0.0D, (Entity)this);
                                vec3d4 = Hooks.addAdjustedVector(vec3d4, 0.0D, 0.5099999904632568D, 0.0D, (Entity)this);
                                Vec3d vec3d5 = vec3d12.crossProduct(Hooks.addAdjustedVector(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D, 1.0D, 0.0D, (Entity)this));
                                Vec3d vec3d6 = vec3d5.scale((f9 * 0.5F));
                                Vec3d vec3d7 = lvt_19_1_.subtract(vec3d6);
                                Vec3d vec3d8 = vec3d4.subtract(vec3d6);
                                Vec3d vec3d9 = lvt_19_1_.add(vec3d6);
                                Vec3d vec3d10 = vec3d4.add(vec3d6);
                                List<AxisAlignedBB> list = this.world.getCollisionBoxes((Entity)this, axisalignedbb);
                                if (!list.isEmpty());
                                float f11 = Float.MIN_VALUE;
                                for (AxisAlignedBB axisalignedbb2 : list) {
                                    if (axisalignedbb2.intersects(vec3d7, vec3d8) || axisalignedbb2.intersects(vec3d9, vec3d10)) {
                                        f11 = (float)Hooks.getRelativeTopOfBB(axisalignedbb2, (Entity)this);
                                        Vec3d vec3d11 = axisalignedbb2.getCenter();
                                        BlockPos blockpos1 = new BlockPos(vec3d11);
                                        int i = 1;
                                        while (i < f7) {
                                            BlockPos blockpos2 = Hooks.getRelativeUpBlockPos(blockpos1, i, (Entity)this);
                                            IBlockState iblockstate2 = this.world.getBlockState(blockpos2);
                                            AxisAlignedBB axisalignedbb1;
                                            if ((axisalignedbb1 = iblockstate2.getCollisionBoundingBox((IBlockAccess)this.world, blockpos2)) != null) {
                                                f11 = (float)Hooks.getRelativeTopOfBB(axisalignedbb1, (Entity)this) + Hooks.getRelativeYOfBlockPos(blockpos2, (Entity)this);
                                                if (f11 - Hooks.getRelativeBottomOfBB(getEntityBoundingBox(), (Entity)this) > f7)
                                                    return;
                                            }
                                            if (i > 1) {
                                                blockpos = Hooks.getRelativeUpBlockPos(blockpos, (Entity)this);
                                                IBlockState iblockstate3 = this.world.getBlockState(blockpos);
                                                if (iblockstate3.getCollisionBoundingBox((IBlockAccess)this.world, blockpos) != null)
                                                    return;
                                            }
                                            i++;
                                        }
                                        break;
                                    }
                                }
                                if (f11 != Float.MIN_VALUE) {
                                    float f14 = (float)(f11 - Hooks.getRelativeBottomOfBB(getEntityBoundingBox(), (Entity)this));
                                    if (f14 > 0.5F && f14 <= f7)
                                        this.autoJumpTime = 1;
                                }
                            }
                        }
                    }
                }
            }
        ci.cancel();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
        super.readEntityFromNBT(nbtTagCompound);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
        super.writeEntityToNBT(nbtTagCompound);
    }
}
