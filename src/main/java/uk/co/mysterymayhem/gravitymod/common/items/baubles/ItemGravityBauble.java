package uk.co.mysterymayhem.gravitymod.common.items.baubles;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import uk.co.mysterymayhem.gravitymod.api.IWeakGravityEnabler;
import uk.co.mysterymayhem.gravitymod.common.modsupport.ModSupport;
import uk.co.mysterymayhem.gravitymod.common.registries.IGravityModItem;
import uk.co.mysterymayhem.gravitymod.common.registries.ModItems;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Mysteryem on 2016-11-05.
 */
@Optional.Interface(iface = ModSupport.INTERFACE_IBAUBLE, modid = ModSupport.BAUBLES_MOD_ID)
public class ItemGravityBauble extends Item implements IBauble, IGravityModItem<ItemGravityBauble>, IWeakGravityEnabler {
    private static final ArrayList<String> DAMAGE_TO_NAME_MAP = new ArrayList<>();

    /*
    0: AMULET(0),
	1: RING(1,2),
	2: BELT(3),
	3: TRINKET(0,1,2,3,4,5,6),
	4: HEAD(4),
	5: BODY(5),
	6: CHARM(6);
     */

    @Override
    @Optional.Method(modid = ModSupport.BAUBLES_MOD_ID)
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.values()[itemStack.getItemDamage()];
    }

    @Override
    public String getModObjectName() {
        return "gravitybauble";
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        int i = stack.getItemDamage();
        if (i < 0 || i >= DAMAGE_TO_NAME_MAP.size()) {
            return super.getTranslationKey() + "." + "error";
        }
        else {
            return super.getTranslationKey() + "." + DAMAGE_TO_NAME_MAP.get(i);
        }
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab == ModItems.UP_AND_DOWN_CREATIVE_TAB) {
            for (int i = 0; i < DAMAGE_TO_NAME_MAP.size(); i++) {
                items.add(new ItemStack(this, 1, i));
            }
        }
        super.getSubItems(tab, items);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerClient(IForgeRegistry<Item> registry) {
        MeshDefinitions meshDefinitions = new MeshDefinitions();
        ModelResourceLocation[] modelResourceLocations = meshDefinitions.list.toArray(new ModelResourceLocation[meshDefinitions.list.size()]);
        ModelBakery.registerItemVariants(this, (ResourceLocation[])modelResourceLocations);
        ModelLoader.setCustomMeshDefinition(this, meshDefinitions);
    }

    @Override
    public void register(IForgeRegistry<Item> registry) {
        if (Loader.isModLoaded(ModSupport.BAUBLES_MOD_ID)) {
            // This code will not be run unless the baubles mod is loaded
            //
            // Thus, no attempt will be made to load the BaubleType class from the baubles mod
            // if the mod is not loaded, meaning that it won't crash when baubles isn't loaded
            for (BaubleType type : BaubleType.values()) {
                DAMAGE_TO_NAME_MAP.add(type.name().toLowerCase(Locale.ENGLISH));
            }
        }
        else {
            DAMAGE_TO_NAME_MAP.add("amulet");
            DAMAGE_TO_NAME_MAP.add("ring");
            DAMAGE_TO_NAME_MAP.add("belt");
            DAMAGE_TO_NAME_MAP.add("trinket");
            DAMAGE_TO_NAME_MAP.add("head");
            DAMAGE_TO_NAME_MAP.add("body");
            DAMAGE_TO_NAME_MAP.add("charm");
        }
        this.setHasSubtypes(true);
        this.setMaxStackSize(1);
        IGravityModItem.super.register(registry);
    }

    // Implements client only interface
    @SideOnly(Side.CLIENT)
    private class MeshDefinitions implements ItemMeshDefinition {

        final ArrayList<ModelResourceLocation> list;

        MeshDefinitions() {
            ItemGravityBauble item = ItemGravityBauble.this;
            this.list = new ArrayList<>();
            for (String suffix : DAMAGE_TO_NAME_MAP) {
                this.list.add(new ModelResourceLocation(item.getRegistryName() + "_" + suffix, "inventory"));
            }
        }

        @Override
        public ModelResourceLocation getModelLocation(ItemStack stack) {
            int metadata = stack.getMetadata();
            return this.list.get(metadata);
        }
    }
}
