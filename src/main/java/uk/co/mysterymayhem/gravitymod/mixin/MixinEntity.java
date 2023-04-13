package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandResultStats;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.asm.Hooks;

import java.util.*;


@Mixin(Entity.class)
public abstract class MixinEntity{

    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public boolean onGround;
    @Shadow
    public boolean collidedHorizontally;
    @Shadow
    public boolean collidedVertically;
    @Shadow
    public boolean collided;
    @Shadow
    protected boolean isInWeb;
    @Shadow
    public float distanceWalkedModified;
    @Shadow
    public float distanceWalkedOnStepModified;
    @Shadow
    private int nextStepDistance;
    @Shadow
    private float nextFlap;
    @Shadow
    public float stepHeight;
    @Shadow
    public boolean noClip;
    @Shadow
    private int fire;
    @Shadow
    protected Random rand;
    @Shadow
    private long pistonDeltasGameTime;
    @Shadow
    @Final
    private double[] pistonDeltas;
    @Shadow
    public World world;
    @Shadow
    public boolean isSneaking() {return false;}
    @Shadow
    public void setEntityBoundingBox(AxisAlignedBB bb){}
    @Shadow
    public AxisAlignedBB getEntityBoundingBox() {return null;}
    @Shadow
    public void resetPositionToBB() {}
    @Shadow
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {}
    @Shadow
    protected boolean canTriggerWalking() {return false;}
    @Shadow
    public boolean isRiding() {return false;}
    @Shadow
    public boolean isInWater() {return false;}
    @Shadow
    protected void playStepSound(BlockPos pos, Block blockIn) {}
    @Shadow
    public boolean isBeingRidden() {return false;}
    @Shadow
    public Entity getControllingPassenger() {
        return null;
    }
    @Shadow
    public void playSound(SoundEvent soundIn, float volume, float pitch) {}
    @Shadow
    protected SoundEvent getSwimSound() {return null;}
    @Shadow
    protected float playFlySound(float p_191954_1_) {
        return 0.0F;
    }
    @Shadow
    protected boolean makeFlySound() {return false;}
    @Shadow
    protected void doBlockCollisions() {}
    @Shadow
    public void addEntityCrashInfo(CrashReportCategory category) {}
    @Shadow
    public boolean isWet() {return false;}
    @Shadow
    protected void dealFireDamage(int amount) {}
    @Shadow
    public void setFire(int seconds) {}
    @Shadow
    protected int getFireImmuneTicks() {return 0;}
    @Shadow
    public boolean isBurning() {return false;}

    @Inject(method = "move", at = @At(value = "HEAD"), cancellable = true)
    private void trigger(MoverType type, double x, double y, double z, CallbackInfo ci) {
        if (this.noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
            this.resetPositionToBB();
        } else {
            if (type == MoverType.PISTON) {
                long i = this.world.getTotalWorldTime();
                if (i != this.pistonDeltasGameTime) {
                    Arrays.fill(this.pistonDeltas, 0.0);
                    this.pistonDeltasGameTime = i;
                }

                int i5;
                double d13;
                if (x != 0.0) {
                    i5 = EnumFacing.Axis.X.ordinal();
                    d13 = MathHelper.clamp(x + this.pistonDeltas[i5], -0.51, 0.51);
                    x = d13 - this.pistonDeltas[i5];
                    this.pistonDeltas[i5] = d13;
                    if (Math.abs(x) <= 9.999999747378752E-6) {
                        return;
                    }
                } else if (y != 0.0) {
                    i5 = EnumFacing.Axis.Y.ordinal();
                    d13 = MathHelper.clamp(y + this.pistonDeltas[i5], -0.51, 0.51);
                    y = d13 - this.pistonDeltas[i5];
                    this.pistonDeltas[i5] = d13;
                    if (Math.abs(y) <= 9.999999747378752E-6) {
                        return;
                    }
                } else {
                    if (z == 0.0) {
                        return;
                    }

                    i5 = EnumFacing.Axis.Z.ordinal();
                    d13 = MathHelper.clamp(z + this.pistonDeltas[i5], -0.51, 0.51);
                    z = d13 - this.pistonDeltas[i5];
                    this.pistonDeltas[i5] = d13;
                    if (Math.abs(z) <= 9.999999747378752E-6) {
                        return;
                    }
                }
            }

            this.world.profiler.startSection("move");
            double d10 = this.posX;
            double d11 = this.posY;
            double d1 = this.posZ;
            if (this.isInWeb) {
                this.isInWeb = false;
                x *= 0.25;
                y *= 0.05000000074505806;
                z *= 0.25;
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
            }

            double d2 = x;
            double d3 = y;
            double d4 = z;
            if ((type == MoverType.SELF || type == MoverType.PLAYER) && this.onGround && this.isSneaking() && (Object)this instanceof EntityPlayer) {
                for(double d5 = 0.05; x != 0.0 && this.world.getCollisionBoxes((Entity)(Object)this, this.getEntityBoundingBox().offset(x, (double)(-this.stepHeight), 0.0)).isEmpty(); d2 = x) {
                    if (x < 0.05 && x >= -0.05) {
                        x = 0.0;
                    } else if (x > 0.0) {
                        x -= 0.05;
                    } else {
                        x += 0.05;
                    }
                }

                for(; z != 0.0 && this.world.getCollisionBoxes((Entity)(Object)this, this.getEntityBoundingBox().offset(0.0, (double)(-this.stepHeight), z)).isEmpty(); d4 = z) {
                    if (z < 0.05 && z >= -0.05) {
                        z = 0.0;
                    } else if (z > 0.0) {
                        z -= 0.05;
                    } else {
                        z += 0.05;
                    }
                }

                for(; x != 0.0 && z != 0.0 && this.world.getCollisionBoxes((Entity)(Object)this, this.getEntityBoundingBox().offset(x, (double)(-this.stepHeight), z)).isEmpty(); d4 = z) {
                    if (x < 0.05 && x >= -0.05) {
                        x = 0.0;
                    } else if (x > 0.0) {
                        x -= 0.05;
                    } else {
                        x += 0.05;
                    }

                    d2 = x;
                    if (z < 0.05 && z >= -0.05) {
                        z = 0.0;
                    } else if (z > 0.0) {
                        z -= 0.05;
                    } else {
                        z += 0.05;
                    }
                }
            }

            List<AxisAlignedBB> list1 = this.world.getCollisionBoxes((Entity)(Object)this, this.getEntityBoundingBox().expand(x, y, z));
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
            int k5;
            int j6;
            if (y != 0.0) {
                k5 = 0;

                for(j6 = list1.size(); k5 < j6; ++k5) {
                    y = Hooks.reverseYOffset(list1.get(k5), getEntityBoundingBox(), y);
                }

                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
            }

            if (x != 0.0) {
                k5 = 0;

                for(j6 = list1.size(); k5 < j6; ++k5) {
                    x = Hooks.reverseXOffset(list1.get(k5), getEntityBoundingBox(), x);
                }

                if (x != 0.0) {
                    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0, 0.0));
                }
            }

            if (z != 0.0) {
                k5 = 0;

                for(j6 = list1.size(); k5 < j6; ++k5) {
                    z = Hooks.reverseZOffset(list1.get(k5), getEntityBoundingBox(), z);
                }

                if (z != 0.0) {
                    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, z));
                }
            }

            boolean flag = this.onGround || y != y && y < 0.0;
            double d8;
            if (this.stepHeight > 0.0F && flag && (d2 != x || d4 != z)) {
                double d14 = x;
                double d6 = y;
                double d7 = z;
                AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
                this.setEntityBoundingBox(axisalignedbb);
                y = this.stepHeight;
                List<AxisAlignedBB> list = this.world.getCollisionBoxes((Entity)(Object)this, this.getEntityBoundingBox().expand(d2, y, d4));
                AxisAlignedBB axisalignedbb2 = this.getEntityBoundingBox();
                AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0, d4);
                d8 = y;
                int j1 = 0;

                for(int k1 = list.size(); j1 < k1; ++j1) {
                    d8 = Hooks.reverseYOffset(list.get(j1), axisalignedbb3, d8);
                }

                axisalignedbb2 = axisalignedbb2.offset(0.0, d8, 0.0);
                double d18 = d2;
                int l1 = 0;

                for(int i2 = list.size(); l1 < i2; ++l1) {
                    d18 = Hooks.reverseXOffset(list.get(l1), axisalignedbb2, d18);
                }

                axisalignedbb2 = axisalignedbb2.offset(d18, 0.0, 0.0);
                double d19 = d4;
                int j2 = 0;

                for(int k2 = list.size(); j2 < k2; ++j2) {
                    d19 = Hooks.reverseZOffset(list.get(j2), axisalignedbb2, d19);
                }

                axisalignedbb2 = axisalignedbb2.offset(0.0, 0.0, d19);
                AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
                double d20 = y;
                int l2 = 0;

                for(int i3 = list.size(); l2 < i3; ++l2) {
                    d20 = Hooks.reverseYOffset(list.get(l2), axisalignedbb4, d20);
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0, d20, 0.0);
                double d21 = d2;
                int j3 = 0;

                for(int k3 = list.size(); j3 < k3; ++j3) {
                    d21 = Hooks.reverseXOffset(list.get(j3), axisalignedbb4, d21);
                }

                axisalignedbb4 = axisalignedbb4.offset(d21, 0.0, 0.0);
                double d22 = d4;
                int l3 = 0;

                for(int i4 = list.size(); l3 < i4; ++l3) {
                    d22 = Hooks.reverseZOffset(list.get(l3), axisalignedbb4, d22);
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0, 0.0, d22);
                double d23 = d18 * d18 + d19 * d19;
                double d9 = d21 * d21 + d22 * d22;
                if (d23 > d9) {
                    x = d18;
                    z = d19;
                    y = -d8;
                    this.setEntityBoundingBox(axisalignedbb2);
                } else {
                    x = d21;
                    z = d22;
                    y = -d20;
                    this.setEntityBoundingBox(axisalignedbb4);
                }

                int j4 = 0;

                for(int k4 = list.size(); j4 < k4; ++j4) {
                    y = Hooks.reverseYOffset(list.get(j4), getEntityBoundingBox(), y);
                }

                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
                if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                    x = d14;
                    y = d6;
                    z = d7;
                    this.setEntityBoundingBox(axisalignedbb1);
                }
            }

            this.world.profiler.endSection();
            this.world.profiler.startSection("rest");
            this.resetPositionToBB();
            this.collidedHorizontally = d2 != x || d4 != z;
            this.collidedVertically = y != y;
            this.onGround = this.collidedVertically && d3 < 0.0;
            this.collided = this.collidedHorizontally || this.collidedVertically;
            j6 = MathHelper.floor(this.posX);
            int i1 = MathHelper.floor(this.posY - 0.20000000298023224);
            int k6 = MathHelper.floor(this.posZ);
            BlockPos blockpos = Hooks.getImmutableBlockPosBelowEntity((Entity)(Object)this);
            IBlockState iblockstate = this.world.getBlockState(blockpos);
            if (iblockstate.getMaterial() == Material.AIR) {
                BlockPos blockpos1 = Hooks.getRelativeDownBlockPos(blockpos, (Entity)(Object)this);
                IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
                Block block1 = iblockstate1.getBlock();
                if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
                    iblockstate = iblockstate1;
                    blockpos = blockpos1;
                }
            }

            this.updateFallState(y, this.onGround, iblockstate, blockpos);
            if (d2 != x) {
                this.motionX = 0.0;
            }

            if (d4 != z) {
                this.motionZ = 0.0;
            }

            Block block = iblockstate.getBlock();
            if (d3 != y) {
                block.onLanded(this.world, (Entity)(Object)this);
            }

            if (this.canTriggerWalking() && (!this.onGround || !this.isSneaking() || !((Object)this instanceof EntityPlayer)) && !this.isRiding()) {
                double d15 = this.posX - d10;
                double d16 = this.posY - d11;
                double d17 = this.posZ - d1;
                d15 = Hooks.inverseAdjustXYZ((Entity)(Object)this, d15, d16, d17)[0];
                d16 = Hooks.inverseAdjustXYZ((Entity)(Object)this, d15, d16, d17)[1];
                d17 = Hooks.inverseAdjustXYZ((Entity)(Object)this, d15, d16, d17)[2];
                if (block != Blocks.LADDER) {
                    d16 = 0.0;
                }

                if (block != null && this.onGround) {
                    block.onEntityWalk(this.world, blockpos, (Entity)(Object)this);
                }

                this.distanceWalkedModified = (float)((double)this.distanceWalkedModified + (double)MathHelper.sqrt(d15 * d15 + d17 * d17) * 0.6);
                this.distanceWalkedOnStepModified = (float)((double)this.distanceWalkedOnStepModified + (double)MathHelper.sqrt(d15 * d15 + d16 * d16 + d17 * d17) * 0.6);
                if (this.distanceWalkedOnStepModified > (float)this.nextStepDistance && iblockstate.getMaterial() != Material.AIR) {
                    this.nextStepDistance = (int)this.distanceWalkedOnStepModified + 1;
                    if (!this.isInWater()) {
                        this.playStepSound(blockpos, block);
                    } else {
                        Entity entity = this.isBeingRidden() && this.getControllingPassenger() != null ? this.getControllingPassenger() : (Entity)(Object)this;
                        float f = entity == (Object)this ? 0.35F : 0.4F;
                        float f1 = MathHelper.sqrt(entity.motionX * entity.motionX * 0.20000000298023224 + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ * 0.20000000298023224) * f;
                        if (f1 > 1.0F) {
                            f1 = 1.0F;
                        }

                        this.playSound(this.getSwimSound(), f1, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                    }
                } else if (this.distanceWalkedOnStepModified > this.nextFlap && this.makeFlySound() && iblockstate.getMaterial() == Material.AIR) {
                    this.nextFlap = this.playFlySound(this.distanceWalkedOnStepModified);
                }
            }

            try {
                this.doBlockCollisions();
            } catch (Throwable var58) {
                CrashReport crashreport = CrashReport.makeCrashReport(var58, "Checking entity block collision");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
                this.addEntityCrashInfo(crashreportcategory);
                throw new ReportedException(crashreport);
            }

            boolean flag1 = this.isWet();
            if (this.world.isFlammableWithin(this.getEntityBoundingBox().shrink(0.001))) {
                this.dealFireDamage(1);
                if (!flag1) {
                    ++this.fire;
                    if (this.fire == 0) {
                        this.setFire(8);
                    }
                }
            } else if (this.fire <= 0) {
                this.fire = -this.getFireImmuneTicks();
            }

            if (flag1 && this.isBurning()) {
                this.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                this.fire = -this.getFireImmuneTicks();
            }

            this.world.profiler.endSection();
        }
        ci.cancel();
    }


}
