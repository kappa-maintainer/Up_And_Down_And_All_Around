package uk.co.mysterymayhem.gravitymod.common.items.materials;

import baubles.api.IBauble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.api.IWeakGravityEnabler;
import uk.co.mysterymayhem.gravitymod.common.modsupport.ModSupport;
import uk.co.mysterymayhem.gravitymod.common.registries.IGravityModItem;
import uk.co.mysterymayhem.gravitymod.common.registries.StaticItems;
import uk.co.mysterymayhem.mystlib.util.KeyBindingUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Craft with armour to enable [no prefix] gravity field interaction
 * Created by Mysteryem on 2016-11-11.
 */
public class ItemArmourPaste extends Item implements IGravityModItem<ItemArmourPaste> {
    private static final String NBT_KEY = "mystgravity_paste";

    private static final EnumSet<EntityEquipmentSlot> armourSlots = EnumSet.of(
            EntityEquipmentSlot.CHEST, EntityEquipmentSlot.FEET, EntityEquipmentSlot.HEAD, EntityEquipmentSlot.LEGS);

    public static boolean hasPasteTag(@Nonnull ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        return tagCompound != null && tagCompound.hasKey(NBT_KEY);
    }

    private static boolean isItemArmour(@Nonnull ItemStack stack, @Nonnull Item item) {
        if (item instanceof ItemArmor) {
            return true;
        }

        for (EntityEquipmentSlot slot : armourSlots) {
            if (item.isValidArmor(stack, slot, null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isItemBauble(@Nullable Item item) {
        return ModSupport.isModLoaded(ModSupport.BAUBLES_MOD_ID) && item instanceof IBauble;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag advanced) {
        KeyBinding keyBindSneak = Minecraft.getMinecraft().gameSettings.keyBindSneak;
        if (KeyBindingUtil.isKeyPressed(keyBindSneak)) {
            if (ModSupport.isModLoaded(ModSupport.BAUBLES_MOD_ID)) {
                tooltip.add(I18n.format("mouseovertext.mysttmtgravitymod.armourpaste.infobaubles"));
                tooltip.add(I18n.format("mouseovertext.mysttmtgravitymod.armourpaste.inforemovalbaubles"));
            }
            else {
                tooltip.add(I18n.format("mouseovertext.mysttmtgravitymod.armourpaste.info"));
                tooltip.add(I18n.format("mouseovertext.mysttmtgravitymod.armourpaste.inforemoval"));
            }
        }
        else {
            tooltip.add(keyBindSneak.getDisplayName() + I18n.format("mouseovertext.mysttmtgravitymod.presskeyfordetails"));
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return stack.isItemEnchanted() ? EnumRarity.RARE : GravityMod.RARITY_NORMAL;
    }

    @Override
    public String getModObjectName() {
        return "armourpaste";
    }

    @Override
    public void postInit() {
        ForgeRegistries.RECIPES.register(new ArmourPasteRecipe(new ResourceLocation(GravityMod.MOD_ID, "armour_paste")).setRegistryName(new ResourceLocation(GravityMod.MOD_ID, "armour_paste_recipe")));
        ForgeRegistries.RECIPES.register(new ArmourPasteRemoval(new ResourceLocation(GravityMod.MOD_ID, "armour_paste")).setRegistryName(new ResourceLocation(GravityMod.MOD_ID, "armour_paste_removal")));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerClient(IForgeRegistry<Item> registry) {
        String ANY_ARMOUR_TEXT = I18n.format("crafting.mysttmtgravitymod.armorpasteinfo.nopaste");
        String ANY_ARMOUR_WITH_PASTE_TEXT = I18n.format("crafting.mysttmtgravitymod.armorpasteinfo.paste");
        ArmourPasteRemoval.DUMMY_RECIPE_INPUT.setStackDisplayName(ANY_ARMOUR_WITH_PASTE_TEXT);
        ArmourPasteRemoval.DUMMY_RECIPE_OUTPUT.setStackDisplayName(ANY_ARMOUR_TEXT);
        ArmourPasteRecipe.DUMMY_RECIPE_INPUT.setStackDisplayName(ANY_ARMOUR_TEXT);
        ArmourPasteRecipe.DUMMY_RECIPE_OUTPUT.setStackDisplayName(ANY_ARMOUR_WITH_PASTE_TEXT);
        IGravityModItem.super.registerClient(registry);
    }

    private static class ArmourPasteRecipe extends ShapelessRecipes {

        static final ItemStack DUMMY_RECIPE_INPUT = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
        static final ItemStack DUMMY_RECIPE_OUTPUT = new ItemStack(Items.CHAINMAIL_CHESTPLATE);

        static {
            NBTTagCompound tagCompound = DUMMY_RECIPE_OUTPUT.getTagCompound();
            if (tagCompound == null) {
                tagCompound = new NBTTagCompound();
                DUMMY_RECIPE_OUTPUT.setTagCompound(tagCompound);
            }
            tagCompound.setBoolean(NBT_KEY, true);
        }


        public ArmourPasteRecipe(ResourceLocation group) {
            super(
                    group.toString(),
                    DUMMY_RECIPE_OUTPUT,
                    NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(DUMMY_RECIPE_INPUT), Ingredient.fromItem(StaticItems.ARMOUR_PASTE)));
        }

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            int nonPasteItemsFound = 0;
            int pasteItemsFound = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    if (item == StaticItems.ARMOUR_PASTE) {
                        if (++pasteItemsFound > 1) {
                            return false;
                        }
                    }
                    else if (isItemArmour(stack, item) || isItemBauble(item)) {
                        if (stack.getCount() != 1 || hasPasteTag(stack) || item instanceof IWeakGravityEnabler) {
                            return false;
                        }
                        else if (++nonPasteItemsFound > 1) {
                            return false;
                        }
                    }
                    else {
                        return false;
                    }
                }
            }
            return nonPasteItemsFound == 1 && pasteItemsFound == 1;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack armourStack = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                armourStack = inv.getStackInSlot(i);
                if (!armourStack.isEmpty() && armourStack.getItem() != StaticItems.ARMOUR_PASTE) {
                    break;
                }
            }

            ItemStack copy = armourStack.copy();
            NBTTagCompound tagCompound = copy.getTagCompound();
            if (tagCompound == null) {
                tagCompound = new NBTTagCompound();
                copy.setTagCompound(tagCompound);
            }
            tagCompound.setBoolean(NBT_KEY, true);
            //
//            copy.stackSize = 1;
            return copy;

        }

        @Override
        public ItemStack getRecipeOutput() {
            return super.getRecipeOutput();
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            return ForgeHooks.defaultRecipeGetRemainingItems(inv);
        }
    }

    private static class ArmourPasteRemoval extends ShapelessRecipes {

        static final ItemStack DUMMY_RECIPE_INPUT = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
        static final ItemStack DUMMY_RECIPE_OUTPUT = new ItemStack(Items.CHAINMAIL_CHESTPLATE);

        static {
            NBTTagCompound tagCompound = DUMMY_RECIPE_INPUT.getTagCompound();
            if (tagCompound == null) {
                tagCompound = new NBTTagCompound();
                DUMMY_RECIPE_INPUT.setTagCompound(tagCompound);
            }
            tagCompound.setBoolean(NBT_KEY, true);
        }

        public ArmourPasteRemoval(ResourceLocation group) {
            super(
                    group.toString(),
                    DUMMY_RECIPE_OUTPUT,
                    NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(DUMMY_RECIPE_INPUT), Ingredient.fromItem(Items.WATER_BUCKET)));
        }

        @Override
        public ItemStack getRecipeOutput() {
            return super.getRecipeOutput();
        }

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            int pasteItemsFound = 0;
            int waterBucketItemsFound = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    if (item == Items.WATER_BUCKET) {
                        if (++waterBucketItemsFound > 1) {
                            return false;
                        }
                    }
                    else if (stack.getCount() == 1 && (isItemArmour(stack, item) || isItemBauble(item)) && hasPasteTag(stack)) {
                        if (++pasteItemsFound > 1) {
                            // Found too many paste items
                            return false;
                        }
                    }
                    else {
                        // Found an item that isn't a water bucket or an armour piece/bauble with a paste tag
                        return false;
                    }
                }
            }
            // Necessary to check as either could be zero
            return pasteItemsFound == 1 && waterBucketItemsFound == 1;
        }

        @Nullable
        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack armourStack = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                armourStack = inv.getStackInSlot(i);
                if (!armourStack.isEmpty() && armourStack.getItem() != Items.WATER_BUCKET) {
                    break;
                }
            }

            ItemStack copy = armourStack.copy();
            NBTTagCompound tagCompound = copy.getTagCompound();
            if (tagCompound != null) {
                tagCompound.removeTag(NBT_KEY);
                if (tagCompound.isEmpty()) {
                    // Prevent leaving behind empty tags
                    copy.setTagCompound(null);
                }
            }
//            copy.stackSize = 1;
            return copy;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            return ForgeHooks.defaultRecipeGetRemainingItems(inv);
        }
    }
}
