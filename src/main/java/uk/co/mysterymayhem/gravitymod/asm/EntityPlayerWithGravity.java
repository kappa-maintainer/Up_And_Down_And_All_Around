package uk.co.mysterymayhem.gravitymod.asm;

import com.mojang.authlib.GameProfile;
import gnu.trove.stack.array.TDoubleArrayStack;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.api.API;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.common.capabilities.gravitydirection.IGravityDirectionCapability;
import uk.co.mysterymayhem.gravitymod.common.util.BlockStateHelper;
import uk.co.mysterymayhem.gravitymod.common.util.Vec3dHelper;
import uk.co.mysterymayhem.gravitymod.common.util.boundingboxes.GravityAxisAlignedBB;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

import static uk.co.mysterymayhem.gravitymod.common.util.Vec3dHelper.PITCH;
import static uk.co.mysterymayhem.gravitymod.common.util.Vec3dHelper.YAW;

/**
 * Class inserted into EntityPlayerMP and AbstractClientPlayer's hierarchy in order to reduce the amount of ASM needed
 * Created by Mysteryem on 2016-09-04.
 */
public abstract class EntityPlayerWithGravity extends EntityPlayer {

    private final Deque<FieldState> motionFieldStateStack = new ArrayDeque<>();
    private final TDoubleArrayStack motionXStore = new TDoubleArrayStack(TDoubleArrayStack.DEFAULT_CAPACITY, 0);
    private final TDoubleArrayStack motionYStore = new TDoubleArrayStack(TDoubleArrayStack.DEFAULT_CAPACITY, 0);
    private final TDoubleArrayStack motionZStore = new TDoubleArrayStack(TDoubleArrayStack.DEFAULT_CAPACITY, 0);
    private final Deque<FieldState> rotationFieldStateStack = new ArrayDeque<>();
    // Relative position doesn't exist, but this allows adjustments made in other methods such as 'this.posY+=3',
    // to correspond to whatever we think 'UP' is
    private boolean positionVarsAreRelative;
    private float rotationPitchStore;
    private float rotationYawStore;
    private boolean superConstructorsFinished = false;

    {
        // starting state is ABSOLUTE
        this.motionFieldStateStack.push(FieldState.ABSOLUTE);
    }

    {
        // starting state is ABSOLUTE
        this.rotationFieldStateStack.push(FieldState.ABSOLUTE);
    }

    public EntityPlayerWithGravity(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
        this.superConstructorsFinished = true;

        try {
            // Instead of checking if the capability is null every time we set a boundingbox
            IGravityDirectionCapability capability = API.getGravityDirectionCapability(this);
            if (capability == null) {
                //explode
                throw new RuntimeException("[UpAndDown] Some other mod has blocked UpAndDown from adding its GravityDirectionCapability" +
                        " to a player (or has straight up deleted it), this is very bad behaviour and whatever mod is causing this should " +
                        "be notified" +
                        " immediately");
            }
        } catch (ClassCastException ex) {
            throw new RuntimeException("[UpAndDown] Tried to get the gravity-direction-capability of a player, but another mod returned" +
                    " it's own capability when it shouldn't have. Please report this to the author of the mod that caused this." +
                    " UpAndDownAndAllAround is not at fault here", ex);
        }

        // We refuse to do these in setPosition called in the Entity constructor before the capability has been added
        // So we do them now, after the Entity constructor has finished and the GravityDirectionCapability has been added
        this.setEntityBoundingBox(Hooks.getGravityAdjustedHitbox(this));
        this.setSize(this.width, this.height);

        this.positionVarsAreRelative = false;
    }

    /**
     * Sets the width and height of the entity.
     */
    @Override
    protected void setSize(float width, float height) {
        if (width != this.width || height != this.height) {
            float oldWidth = this.width;
            float oldHeight = this.height;
            this.width = width;
            this.height = height;
            AxisAlignedBB oldAABB = this.getEntityBoundingBox();

            double heightExpansion = (this.height - oldHeight) / 2d;
            double widthExpansion = (this.width - oldWidth) / 2d;
            AxisAlignedBB newAABB = oldAABB.grow(widthExpansion, heightExpansion, widthExpansion).offset(0, heightExpansion, 0);

            this.setEntityBoundingBox(newAABB);

            if (this.width > oldWidth && !this.firstUpdate && !this.world.isRemote) {
                this.move(MoverType.SELF, (double)(oldWidth - this.width), 0, (double)(oldWidth - this.width));
            }
        }
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        AxisAlignedBB entityBoundingBox = super.getEntityBoundingBox();
        if (!(entityBoundingBox instanceof GravityAxisAlignedBB)) {
            entityBoundingBox = Hooks.replaceWithGravityAware(this, entityBoundingBox);
            this.setEntityBoundingBox(entityBoundingBox);
        }
        return entityBoundingBox;
    }

    @Override
    public void setEntityBoundingBox(@Nonnull AxisAlignedBB bb) {
        bb = Hooks.replaceWithGravityAware(this, bb);
        super.setEntityBoundingBox(bb);
    }

    @Override
    public void move(MoverType moverType, double x, double y, double z) {
        if (this.isMotionAbsolute()) {
            // If moveEntity is called outside of the player tick, then some external force is moving the player
            double[] doubles = Hooks.inverseAdjustXYZ(this, x, y, z);
            x = doubles[0];
            y = doubles[1];
            z = doubles[2];
            this.makeMotionRelative();
            super.move(moverType, x, y, z);
            this.popMotionStack();
        }
        else {
            super.move(moverType, x, y, z);
        }
    }

    public boolean isMotionAbsolute() {
        return !this.isMotionRelative();
    }

    @SuppressWarnings("deprecation")
    public void makeMotionRelative() {
        FieldState top = this.motionFieldStateStack.peek();
        if (top == FieldState.ABSOLUTE) {
            this.makeMotionFieldsRelative();
        }
        this.motionFieldStateStack.push(FieldState.RELATIVE);
    }

    @SuppressWarnings("deprecation")
    public void popMotionStack() {
        FieldState removed = this.motionFieldStateStack.pop();
        FieldState top = this.motionFieldStateStack.peek();
        if (top != removed) {
            switch (top) {
                case ABSOLUTE:
                    this.makeMotionFieldsAbsolute();
                    break;
                case RELATIVE:
                    this.makeMotionFieldsRelative();
                    break;
            }
        }
    }

    public boolean isMotionRelative() {
        return this.motionFieldStateStack.peek() == FieldState.RELATIVE;
    }

    // Should only ever be used within makeMotionRelative/Absolute and popMotionStack
    @Deprecated
    private void makeMotionFieldsRelative() {
        double[] doubles = API.getGravityDirection(this).getInverseAdjustmentFromDOWNDirection().adjustXYZValues(this.motionX, this.motionY, this.motionZ);
        this.motionX = doubles[0];
        this.motionY = doubles[1];
        this.motionZ = doubles[2];
    }

    // Should only ever be used within makeMotionRelative/Absolute and popMotionStack
    @Deprecated
    private void makeMotionFieldsAbsolute() {
        double[] doubles = API.getGravityDirection(this).adjustXYZValues(this.motionX, this.motionY, this.motionZ);
        this.motionX = doubles[0];
        this.motionY = doubles[1];
        this.motionZ = doubles[2];
    }

    @Override
    public float getEyeHeight() {
        return API.getGravityDirection(this).getEntityEyeHeight(this);
    }

    @Override
    public boolean handleWaterMovement() {
        this.makeMotionAbsolute();
        boolean toReturn = super.handleWaterMovement();
        this.popMotionStack();
        return toReturn;
    }

    @SuppressWarnings("deprecation")
    public void makeMotionAbsolute() {
        FieldState top = this.motionFieldStateStack.peek();
        if (top == FieldState.RELATIVE) {
            this.makeMotionFieldsAbsolute();
        }
        this.motionFieldStateStack.push(FieldState.ABSOLUTE);
    }

    //TODO: ASM?
    @Override
    public boolean isEntityInsideOpaqueBlock() {
        if (this.noClip || this.sleeping) {
            return false;
        }
        AxisAlignedBB bb = this.getEntityBoundingBox();
        if (bb instanceof GravityAxisAlignedBB) {
            GravityAxisAlignedBB gBB = (GravityAxisAlignedBB)bb;
            EnumGravityDirection direction = gBB.getDirection();
            Vec3d origin = gBB.getOrigin();

            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

            for (int i = 0; i < 8; ++i) {
                double yMovement = (double)(((float)((i >> 0) % 2) - 0.5F) * 0.1F) + (double)API.getStandardEyeHeight(this);
                double xMovement = (double)(((float)((i >> 1) % 2) - 0.5F) * this.width * 0.8F);
                double zMovement = (double)(((float)((i >> 2) % 2) - 0.5F) * this.width * 0.8F);
                double[] d = direction.adjustXYZValues(xMovement, yMovement, zMovement);

                int j = MathHelper.floor(origin.y + d[1]);
                int k = MathHelper.floor(origin.x + d[0]);
                int l = MathHelper.floor(origin.z + d[2]);

                if (blockpos$pooledmutableblockpos.getX() != k || blockpos$pooledmutableblockpos.getY() != j || blockpos$pooledmutableblockpos.getZ() != l) {
                    blockpos$pooledmutableblockpos.setPos(k, j, l);

                    if (this.world.getBlockState(blockpos$pooledmutableblockpos).causesSuffocation()) {
                        blockpos$pooledmutableblockpos.release();
                        return true;
                    }
                }
            }

            blockpos$pooledmutableblockpos.release();
            return false;

        }
        else {
            return super.isEntityInsideOpaqueBlock();
        }
    }

    /**
     * returns true if this entity is by a ladder, false otherwise
     */
    @Override
    public boolean isOnLadder() {
        if (this.isSpectator()) {
            return false;
        }

        boolean isMonkeyBars = false;

        AxisAlignedBB bb = this.getEntityBoundingBox();
        if (bb instanceof GravityAxisAlignedBB) {
            GravityAxisAlignedBB gBB = (GravityAxisAlignedBB)bb;
            BlockPos blockpos = new BlockPos(gBB.offset(0, 0.001, 0).getOrigin());
            IBlockState iblockstate = this.world.getBlockState(blockpos);

            boolean isOnLadder = net.minecraftforge.common.ForgeHooks.isLivingOnLadder(iblockstate, this.world, blockpos, this);

            //iblockstate will never be null (will be air or will throw an exception if stuff isn't right)
//            if (iblockstate != null) {
            if (iblockstate.getBlock().isLadder(iblockstate, this.world, blockpos, this)) {
                EnumFacing facing = BlockStateHelper.getFacingOfBlockState(iblockstate);
                if (facing != null) {
                    EnumGravityDirection direction = gBB.getDirection();
                    isOnLadder = direction.isValidLadderDirection(facing);
                }
            }
//            }
            if (!isOnLadder) {
                Vec3d topOfHead = gBB.offset(0, this.height, 0).getOrigin();
                blockpos = new BlockPos(topOfHead);
                iblockstate = this.world.getBlockState(blockpos);
                //As before, iblockstate will not be null
//                if (iblockstate != null) {
                if (iblockstate.getBlock().isLadder(iblockstate, this.world, blockpos, this)) {
                    EnumFacing facing = BlockStateHelper.getFacingOfBlockState(iblockstate);
                    if (facing != null) {
                        EnumGravityDirection direction = gBB.getDirection();
                        isOnLadder = facing == direction.getFacingEquivalent() && net.minecraftforge.common.ForgeHooks.isLivingOnLadder(iblockstate,
                                this.world, blockpos,
                                this);
                        isMonkeyBars = true;
                    }
                }
//                }
            }
            if (isOnLadder && isMonkeyBars) {
                if (this.distanceWalkedOnStepModified > (float)this.nextStepDistance) {
                    this.nextStepDistance = (int)this.distanceWalkedOnStepModified + 1;
                    this.playStepSound(blockpos, iblockstate.getBlock());
                }
            }
            return isOnLadder;
        }
        else {
            return super.isOnLadder();
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, Block blockIn) {
        AxisAlignedBB entityBoundingBox = this.getEntityBoundingBox();
        if (entityBoundingBox instanceof GravityAxisAlignedBB) {
            EnumGravityDirection direction = ((GravityAxisAlignedBB)entityBoundingBox).getDirection();
            if (direction == EnumGravityDirection.DOWN) {
                super.playStepSound(pos, blockIn);
            }
            else {
                // Bypass check for pos.up().getBlock() == Blocks.SNOW_LAYER
                SoundType soundtype = blockIn.getSoundType(this.world.getBlockState(pos), this.world, pos, this);

                if (!blockIn.getDefaultState().getMaterial().isLiquid()) {
                    this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15F, soundtype.getPitch());
                }
            }
        }
        else {
            super.playStepSound(pos, blockIn);
        }
    }

    // motion should always be relative when jump is called
    @Override
    public void jump() {
        this.makeMotionRelative();
        super.jump();
        this.popMotionStack();
    }

    /**
     * In vanilla code, knockBack is called with arguments usually calculated based off of the attacker's rotation
     * (knockback enchantment) or the difference in position between the attacker and the target (most entity attacks).
     * We try to work out which case has been called and adjust accordingly for this player's current gravity direction.
     *
     * @param attacker
     * @param strength
     * @param xRatio
     * @param zRatio
     */
    @Override
    public void knockBack(Entity attacker, float strength, double xRatio, double zRatio) {
//        FMLLog.info("x:%s, z:%s", xRatio, zRatio);
        this.makeMotionRelative();
        if (attacker != null) {
            // Standard mob attacks, also arrows, not sure about others (checked first
            if (xRatio == attacker.posX - this.posX && zRatio == attacker.posZ - this.posZ) {
//                FMLLog.info("Distance based attack");
                EnumGravityDirection direction = API.getGravityDirection(this).getInverseAdjustmentFromDOWNDirection();
                double[] d_attacker = direction.adjustXYZValues(attacker.posX, attacker.posY, attacker.posZ);
                double[] d_player = direction.adjustXYZValues(this.posX, this.posY, this.posZ);
                xRatio = d_attacker[0] - d_player[0];
                zRatio = d_attacker[2] - d_player[2];
                super.knockBack(attacker, strength, xRatio, zRatio);
            }
            // Usually the knockback enchantment
            else if (xRatio == (double)MathHelper.sin(attacker.rotationYaw * 0.017453292F) && zRatio == (double)-MathHelper.cos(attacker.rotationYaw *
                    0.017453292F)) {
//                FMLLog.info("Rotation based attack");
                Vec3d lookVec = attacker.getLookVec();
                EnumGravityDirection direction = API.getGravityDirection(this);
                Vec3d adjustedLook = direction.adjustLookVec(lookVec);
                xRatio = -adjustedLook.x;
                zRatio = -adjustedLook.z;
                super.knockBack(attacker, strength, xRatio, zRatio);
            }
            else {
//                FMLLog.info("Unknown attack");
                super.knockBack(attacker, strength, xRatio, zRatio);
            }
        }
        else {
            super.knockBack(attacker, strength, xRatio, zRatio);
        }
        this.popMotionStack();
    }

    @SuppressWarnings("deprecation")
    public void makeRotationAbsolute() {
        FieldState top = this.rotationFieldStateStack.peek();
        if (top == FieldState.RELATIVE) {
            this.makeRotationFieldsAbsolute();
        }
        this.rotationFieldStateStack.push(FieldState.ABSOLUTE);
    }

    /**
     * Should only be called by makeRotationAbsolute and popRotationStack
     */
    @Deprecated
    private void makeRotationFieldsAbsolute() {
        this.rotationYaw = this.rotationYawStore;
        this.rotationPitch = this.rotationPitchStore;
    }

    @SuppressWarnings("deprecation")
    public void makeRotationRelative() {
        FieldState top = this.rotationFieldStateStack.peek();
        if (top == FieldState.ABSOLUTE) {
            this.makeRotationFieldsRelative();
        }
        this.rotationFieldStateStack.push(FieldState.RELATIVE);
    }

    /**
     * Should only be called by makeRotationRelative and popRotationStack
     */
    @Deprecated
    private void makeRotationFieldsRelative() {
        final Vec3d fastAdjustedVectorForRotation =
                API.getGravityDirection(this)
                        .getInverseAdjustmentFromDOWNDirection()
                        .adjustLookVec(Vec3dHelper.getFastVectorForRotation(this.rotationPitch, this.rotationYaw));
        double[] fastPitchAndYawFromVector = Vec3dHelper.getFastPitchAndYawFromVector(fastAdjustedVectorForRotation);

        // Store the absolute yaw and pitch to instance fields so they can be restored easily
        this.rotationYawStore = this.rotationYaw;
        this.rotationPitchStore = this.rotationPitch;
        this.rotationYaw = (float)fastPitchAndYawFromVector[Vec3dHelper.YAW];
        this.rotationPitch = (float)fastPitchAndYawFromVector[Vec3dHelper.PITCH];
    }

    @SuppressWarnings("deprecation")
    public void popRotationStack() {
        FieldState removed = this.rotationFieldStateStack.pop();
        FieldState top = this.rotationFieldStateStack.peek();
        if (top != removed) {
            switch (top) {
                case ABSOLUTE:
                    this.makeRotationFieldsAbsolute();
                    break;
                case RELATIVE:
                    this.makeRotationFieldsRelative();
                    break;
            }
        }
    }

    //TODO: Move contents of switch into EnumGravityDirection
    @Override
    public void resetPositionToBB() {
        // Oh no you don't vanilla MC
        // I need my weird asymmetrical hitboxes so player.posY + player.getEyeHeight() still get's the player's eye position
        API.getGravityDirection(this).resetPositionToBB(this);
    }

    @Override
    public void turn(float yaw, float pitch) {
        final EnumGravityDirection direction = API.getGravityDirection(this);
        final EnumGravityDirection reverseDirection = direction.getInverseAdjustmentFromDOWNDirection();

        final double relativePitchChange = -pitch * 0.15d;
        final double relativeYawChange = yaw * 0.15d;

        final Vec3d normalLookVec = Vec3dHelper.getPreciseVectorForRotation(this.rotationPitch, this.rotationYaw);

        final Vec3d relativeLookVec = reverseDirection.adjustLookVec(normalLookVec);
        final double[] relativePY = Vec3dHelper.getPrecisePitchAndYawFromVector(relativeLookVec);
        final double relativePitch = relativePY[PITCH];
        final double relativeYaw = relativePY[YAW];

        final double changedRelativeYaw = relativeYaw + relativeYawChange;
        final double changedRelativePitch = relativePitch + relativePitchChange;
        final double clampedRelativePitch;

        // Any closer to -90 or 90 produce tiny values that the trig functions will effectively treat as zero
        // this causes an inability to rotate the camera when looking straight up or down
        // While, it's not a problem for UP and DOWN directions, it causes issues when going from UP/DOWN to a different
        // direction, so I've capped UP and DOWN directions as well
        final double maxRelativeYaw = /*direction == EnumGravityDirection.UP || direction == EnumGravityDirection.DOWN ? 90d :*/ 89.99d;
        final double minRelativeYaw = /*direction == EnumGravityDirection.UP || direction == EnumGravityDirection.DOWN ? -90d :*/ -89.99d;

        if (changedRelativePitch > maxRelativeYaw) {
            clampedRelativePitch = maxRelativeYaw;
        }
        else if (changedRelativePitch < minRelativeYaw) {
            clampedRelativePitch = minRelativeYaw;
        }
        else {
            clampedRelativePitch = changedRelativePitch;
        }

        // Directly set pitch and yaw
        final Vec3d relativeChangedLookVec = Vec3dHelper.getPreciseVectorForRotation(clampedRelativePitch, changedRelativeYaw);

        final Vec3d absoluteLookVec = direction.adjustLookVec(relativeChangedLookVec);
        final double[] absolutePY = Vec3dHelper.getPrecisePitchAndYawFromVector(absoluteLookVec);

        final double changedAbsolutePitch = absolutePY[PITCH];
        final double changedAbsoluteYaw = (absolutePY[YAW] % 360);

        // Yaw calculated through yaw change
        final double absoluteYawChange;
        final double effectiveStartingAbsoluteYaw = this.rotationYaw % 360;

        // Limit the change in yaw to 180 degrees each tick
        if (Math.abs(effectiveStartingAbsoluteYaw - changedAbsoluteYaw) > 180) {
            if (effectiveStartingAbsoluteYaw < changedAbsoluteYaw) {
                absoluteYawChange = changedAbsoluteYaw - (effectiveStartingAbsoluteYaw + 360);
            }
            else {
                absoluteYawChange = (changedAbsoluteYaw + 360) - effectiveStartingAbsoluteYaw;
            }
        }
        else {
            absoluteYawChange = changedAbsoluteYaw - effectiveStartingAbsoluteYaw;
        }

        float yawParam = (float)((absoluteYawChange) / 0.15);
        float pitchParam = (float)((this.rotationPitch - changedAbsolutePitch) / 0.15);

        super.turn(yawParam, pitchParam);
    }

    /**
     * Sets the x,y,z of the entity from the given parameters. Also seems to set up a bounding box.
     */
    @Override
    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
//        float f = this.width / 2.0F;
//        float f1 = this.height;

        // We need to make sure that the AttachCapabilitiesEvent has been fired before trying to set the boundingbox
        if (this.superConstructorsFinished) {
            this.setEntityBoundingBox(Hooks.getGravityAdjustedHitbox(this));
            this.setSize(this.width, this.height);
        }
    }

    public void storeMotionX() {
        this.motionXStore.push(this.motionX);
    }

    public void storeMotionY() {
        this.motionYStore.push(this.motionY);
    }

    public void storeMotionZ() {
        this.motionZStore.push(this.motionZ);
    }

    public float super_getEyeHeight() {
        return super.getEyeHeight();
    }

    public double undoMotionXChange() {
        double storedMotion = this.motionXStore.pop();
        double motionChange = this.motionX - storedMotion;
        this.motionX = storedMotion;
        return motionChange;
    }

    public double undoMotionYChange() {
        double storedMotion = this.motionYStore.pop();
        double motionChange = this.motionY - storedMotion;
        this.motionY = storedMotion;
        return motionChange;
    }

    public double undoMotionZChange() {
        double storedMotion = this.motionZStore.pop();
        double motionChange = this.motionZ - storedMotion;
        this.motionZ = storedMotion;
        return motionChange;
    }

    // TODO: ASM
    @Override
    protected void createRunningParticles() {
        AxisAlignedBB bb = this.getEntityBoundingBox();
        if (bb instanceof GravityAxisAlignedBB) {
            // Based on the super implementation
            GravityAxisAlignedBB gBB = (GravityAxisAlignedBB)bb;
            Vec3d belowBlock = gBB.expand(0, -0.20000000298023224D, 0).getOrigin();
            BlockPos blockpos = new BlockPos(belowBlock);
            IBlockState iblockstate = this.world.getBlockState(blockpos);

            if (iblockstate.getRenderType() != EnumBlockRenderType.INVISIBLE) {
                Vec3d particleSpawnPoint = gBB.expand(((double)this.rand.nextFloat() - 0.5D) * (double)this.width, 0.1D, ((double)this.rand.nextFloat() -
                        0.5D) * (double)this.width)
                        .getOrigin();
                double[] d = gBB.getDirection().adjustXYZValues(-this.motionX * 4.0D, 1.5D, -this.motionZ * 4.0D);
                this.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                        particleSpawnPoint.x,
                        particleSpawnPoint.y,
                        particleSpawnPoint.z,
                        d[0],
                        d[1],
                        d[2],
                        Block.getStateId(iblockstate));
            }
        }
        else {
            super.createRunningParticles();
        }
    }

    // This only works in a deobfuscated dev environment and is only used in the dev environment for debugging/coding purposes
    // Outside the dev environment, this method won't be overridden, as the name of the method in this class will not be
    // reobfuscated (probably)
    // Required so we can call this method from inside this package, this will end up calling EntityPlayerSP::updateAutoJump instead
    @SuppressWarnings("WeakerAccess")
    @SideOnly(Side.CLIENT)
    protected void updateAutoJump(float xChange, float zChange) {
        Transformer.logger.warn("Erroneously tried to call func_189810_i(auto-jump method) from " + this);
//        throw new RuntimeException("Unreachable code reached");
    }

    //TODO: Could ASM or call super.setPosition() and then my own code?

    //TODO: ASM the EntityPlayerMP class instead
    @Override
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {
        if (!this.world.isRemote) {

            BlockPos blockpos;

            AxisAlignedBB offsetAABB = this.getEntityBoundingBox().offset(0, -0.20000000298023224D, 0);
            if (offsetAABB instanceof GravityAxisAlignedBB) {
                GravityAxisAlignedBB offsetAABB_g = (GravityAxisAlignedBB)offsetAABB;
                Vec3d origin = offsetAABB_g.getOrigin();
                blockpos = new BlockPos(origin);
            }
            else {
                // Fallback
                int i, j, k;
                i = MathHelper.floor(this.posX);
                j = MathHelper.floor(this.posY - 0.20000000298023224D);
                k = MathHelper.floor(this.posZ);
                blockpos = new BlockPos(i, j, k);
                GravityMod.logWarning("Player bounding box is not gravity aware. In [UpAndDownAndAllAround]:EntityPlayerWithGravity::updateFallState");
            }

            IBlockState iblockstate = this.world.getBlockState(blockpos);

            if (iblockstate.getBlock().isAir(iblockstate, this.world, blockpos)) {
                BlockPos blockpos1 = API.getGravityDirection(this).makeRelativeBlockPos(blockpos).down();

                IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
                Block block = iblockstate1.getBlock();

                if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate) {
                    blockpos = blockpos1;
                    iblockstate = iblockstate1;
                }
            }

            super.updateFallState(y, onGroundIn, iblockstate, blockpos);
        }
        else {
            super.updateFallState(y, onGroundIn, state, pos);
        }
    }

    // TODO: ASM
    @Override
    protected void updateSize() {
        float f;
        float f1;

        if (this.isElytraFlying()) {
            f = 0.6F;
            f1 = 0.6F;
        }
        else if (this.isPlayerSleeping()) {
            f = 0.2F;
            f1 = 0.2F;
        }
        else if (this.isSneaking()) {
            f = 0.6F;
            f1 = 1.65F;
        }
        else {
            f = 0.6F;
            f1 = 1.8F;
        }

        if (f != this.width || f1 != this.height) {
            //TODO: ASM
            //begin
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
            //delete line
            //axisalignedbb = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double)f, axisalignedbb
            // .minY + (double)f1, axisalignedbb.minZ + (double)f);
            double heightExpansion = (f1 - this.height) / 2d;
            double widthExpansion = (f - this.width) / 2d;
            axisalignedbb = axisalignedbb.grow(widthExpansion, heightExpansion, widthExpansion).offset(0, heightExpansion, 0);
            //end ASM
//            axisalignedbb = Hooks.getGravityAdjustedHitbox(this, f, f1);

            if (!this.world.collidesWithAnyBlock(axisalignedbb)) {
                this.setSize(f, f1);
            }
        }
        net.minecraftforge.fml.common.FMLCommonHandler.instance().onPlayerPostTick(this);
    }

    // Overridden so that Hooks class can access this method
    @Override
    protected boolean pushOutOfBlocks(double x, double y, double z) {
        return super.pushOutOfBlocks(x, y, z);
    }

    void makePositionAbsolute() {
        if (this.positionVarsAreRelative) {
            EnumGravityDirection direction = API.getGravityDirection(this);

            double[] doubles = direction.adjustXYZValues(this.posX, this.posY, this.posZ);
            this.posX = doubles[0];
            this.posY = doubles[1];
            this.posZ = doubles[2];

            doubles = direction.adjustXYZValues(this.prevPosX, this.prevPosY, this.prevPosZ);
            this.prevPosX = doubles[0];
            this.prevPosY = doubles[1];
            this.prevPosZ = doubles[2];

            this.positionVarsAreRelative = false;
        }
    }

    void makePositionRelative() {
        if (!this.positionVarsAreRelative) {
            EnumGravityDirection direction = API.getGravityDirection(this).getInverseAdjustmentFromDOWNDirection();

            double[] doubles = direction.adjustXYZValues(this.posX, this.posY, this.posZ);
            this.posX = doubles[0];
            this.posY = doubles[1];
            this.posZ = doubles[2];

            doubles = direction.adjustXYZValues(this.prevPosX, this.prevPosY, this.prevPosZ);
            this.prevPosX = doubles[0];
            this.prevPosY = doubles[1];
            this.prevPosZ = doubles[2];

            this.positionVarsAreRelative = true;
        }
    }

    private enum FieldState {
        ABSOLUTE,
        RELATIVE
    }

}
