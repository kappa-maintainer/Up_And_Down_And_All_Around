package uk.co.mysterymayhem.gravitymod.common.items.materials;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import uk.co.mysterymayhem.gravitymod.common.config.ConfigHandler;
import uk.co.mysterymayhem.gravitymod.common.registries.IGravityModItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by Mysteryem on 2016-11-09.
 */
public class ItemGravityPearl extends Item implements IGravityModItem<ItemGravityPearl> {

    @Override
    public String getModObjectName() {
        return "gravitypearl";
    }

    // Non-block use
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
        playerIn.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(hand));
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (entityIn instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entityIn;
            ItemStack activeItemStack = player.getActiveItemStack();
            if (!activeItemStack.isEmpty()) {
                if (activeItemStack.getItem() == this) {
                    pushPullNearbyEntities(player);
                }
            }
        }
    }

    private static void pushPullNearbyEntities(EntityPlayer playerIn) {
        World world = playerIn.world;

        double range = ConfigHandler.gravitonPearlRange;
        float baseStrength = ConfigHandler.baseGravitonPearlStrength;

        double playerX = playerIn.posX;
        double playerY = playerIn.posY + playerIn.getEyeHeight();
        double playerZ = playerIn.posZ;

        Vec3d playerCentre = getCentreOfAABB(playerIn.getEntityBoundingBox());

        List<Entity> nearbyEntities = world.getEntitiesInAABBexcluding(
                playerIn,
                new AxisAlignedBB(playerX - range, playerY - range, playerZ - range, playerX + range, playerY + range, playerZ + range),
                entity -> (!(entity instanceof EntityPlayer)));

        if (playerIn.isSneaking()) {
            //pull
            for (Entity nearbyEntity : nearbyEntities) {
                float strength = baseStrength;
                if (nearbyEntity instanceof EntityItem) {
                    strength *= 5;
                }
                Vec3d entityCentre = getCentreOfAABB(nearbyEntity.getEntityBoundingBox());
                Vec3d movementVector = playerCentre.subtract(entityCentre);
                if (movementVector.lengthSquared() > 1) {
                    movementVector = movementVector.normalize();
                }

                double xMotionToAdd = movementVector.x * strength;
                double yMotionToAdd = movementVector.y * strength;
                double zMotionToAdd = movementVector.z * strength;

                double entMotionX = nearbyEntity.motionX;
                double entMotionY = nearbyEntity.motionY;
                double entMotionZ = nearbyEntity.motionZ;
//
                if ((xMotionToAdd > 0 && entMotionX < xMotionToAdd)
                        || (xMotionToAdd < 0 && entMotionX > xMotionToAdd)) {
                    nearbyEntity.motionX += xMotionToAdd;
                }
                if ((yMotionToAdd > 0 && entMotionY < yMotionToAdd)
                        || (yMotionToAdd < 0 && entMotionY > yMotionToAdd)) {
                    nearbyEntity.motionY += yMotionToAdd;
                }
                if ((zMotionToAdd > 0 && entMotionZ < zMotionToAdd)
                        || (zMotionToAdd < 0 && entMotionZ > zMotionToAdd)) {
                    nearbyEntity.motionZ += zMotionToAdd;
                }
            }
        }
        else {
            //push
            for (Entity nearbyEntity : nearbyEntities) {
                float strength = baseStrength;
                if (nearbyEntity instanceof IProjectile) {
                    strength *= 10;
                }
                Vec3d entityCentre = getCentreOfAABB(nearbyEntity.getEntityBoundingBox());
                Vec3d movementVector = playerCentre.subtract(entityCentre);
                if (movementVector.lengthSquared() > 1) {
                    movementVector = movementVector.normalize();
                }

                double xMotionToAdd = movementVector.x * strength;
                double yMotionToAdd = movementVector.y * strength;
                double zMotionToAdd = movementVector.z * strength;

                double entMotionX = nearbyEntity.motionX;
                double entMotionY = nearbyEntity.motionY;
                double entMotionZ = nearbyEntity.motionZ;

                if ((xMotionToAdd > 0 && entMotionX < xMotionToAdd)
                        || (xMotionToAdd < 0 && entMotionX > xMotionToAdd)) {
                    nearbyEntity.motionX -= xMotionToAdd;
                }
                if ((yMotionToAdd > 0 && entMotionY < yMotionToAdd)
                        || (yMotionToAdd < 0 && entMotionY > yMotionToAdd)) {
                    nearbyEntity.motionY -= yMotionToAdd;
                }
                if ((zMotionToAdd > 0 && entMotionZ < zMotionToAdd)
                        || (zMotionToAdd < 0 && entMotionZ > zMotionToAdd)) {
                    nearbyEntity.motionZ -= zMotionToAdd;
                }
            }
        }
    }

    // getCentre instance method of AxisAlignedBB is client only
    private static Vec3d getCentreOfAABB(AxisAlignedBB bb) {
        return new Vec3d(bb.minX + (bb.maxX - bb.minX) * 0.5D, bb.minY + (bb.maxY - bb.minY) * 0.5D, bb.minZ + (bb.maxZ - bb.minZ) * 0.5D);
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {
        super.onPlayerStoppedUsing(stack, worldIn, entityLiving, timeLeft);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("mouseovertext.gravitymod.gravitypearl"));
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity location, ItemStack itemstack) {
        if (location instanceof EntityItem) {
            EntityItem entityItem = (EntityItem)location;
            if (itemstack.getItem() == this) {
                itemstack.setItemDamage(0);
                entityItem.setNoGravity(true);
                // Item flies more in the direction the player is looking if this is done
                entityItem.motionY -= 0.1;
            }
        }

        return null;
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        int age = entityItem.age;
        if (entityItem.world.isRemote) {
            // For some reason this fixes some client/server desync. Is there perhaps a bigger vanilla issue at play here?
            age++;
        }
        if (age >= 10) {
            if (age < 20) {
                entityItem.motionX *= 0.5;
                entityItem.motionY *= 0.5;
                entityItem.motionZ *= 0.5;
            }
            else if (age < 30) {
                entityItem.motionX *= 0.1;
                entityItem.motionY *= 0.1;
                entityItem.motionZ *= 0.1;
            }
            else {
                entityItem.motionX = 0;
                entityItem.motionY = 0;
                entityItem.motionZ = 0;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerClient(IForgeRegistry<Item> registry) {
        ModelLoader.registerItemVariants(this,
                new ModelResourceLocation(this.getRegistryName() + "_push", "inventory"),
                new ModelResourceLocation(this.getRegistryName() + "_pull", "inventory"));
        IGravityModItem.super.registerClient(registry);
    }

    @Override
    public void register(IForgeRegistry<Item> registry) {
        this.addPropertyOverride(new ResourceLocation("use"), new IItemPropertyGetter() {
            @Override
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
                if (entityIn != null) {
                    ItemStack activeStack = entityIn.getActiveItemStack();
                    if (activeStack == stack && stack.getItem() == ItemGravityPearl.this) {
                        if (entityIn.isSneaking()) {
                            //pull
                            return 1f;
                        }
                        else {
                            //push
                            return 2f;
                        }
                    }
//                    }
                }
                return 0f;
            }
        });
        this.setMaxStackSize(16);

        IGravityModItem.super.register(registry);
    }
}
