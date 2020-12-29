package uk.co.mysterymayhem.gravitymod.common.blocks.gravitygenerator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityTier;
import uk.co.mysterymayhem.gravitymod.common.blocks.AbstractGravityModBlockWithItem;
import uk.co.mysterymayhem.gravitymod.common.blocks.GenericItemBlock;
import uk.co.mysterymayhem.gravitymod.common.registries.StaticGUIs;
import uk.co.mysterymayhem.mystlib.block.metaconverters.AbstractMetaMapper.MetaHelper;

import java.util.Collection;

/**
 * Created by Mysteryem on 2016-10-10.
 */
public class BlockGravityGenerator extends AbstractGravityModBlockWithItem<BlockGravityGenerator, GenericItemBlock<BlockGravityGenerator>> {

    // Used with get actual state when block is powered
    public static final PropertyBool ENABLED = PropertyBool.create("active");
    // Direction the block is faced
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    public static final PropertyEnum<EnumGravityTier> TIER = EnumGravityTier.BLOCKSTATE_PROPERTY;
    public static final PropertyBool REVERSED = PropertyBool.create("reversed");

    public BlockGravityGenerator() {
        super(Material.IRON, MapColor.IRON, new MetaHelper().addMeta(TIER, REVERSED).addNonMeta(FACING, ENABLED));
        this.setHardness(5F).setResistance(10f).setSoundType(SoundType.METAL);
    }

    @Override
    public GenericItemBlock<BlockGravityGenerator> createItem(BlockGravityGenerator block) {
        GenericItemBlock<BlockGravityGenerator> itemBlock = new GenericItemBlock<BlockGravityGenerator>(block) {
            @Override
            public EnumRarity getRarity(ItemStack stack) {
                if (stack.isItemEnchanted()) {
                    return EnumRarity.RARE;
                }
                int itemDamage = stack.getItemDamage();
                IBlockState stateFromMeta = BlockGravityGenerator.this.getStateFromMeta(itemDamage);
                switch (stateFromMeta.getValue(TIER)) {
                    case WEAK:
                        return GravityMod.RARITY_WEAK;
                    case NORMAL:
                        return GravityMod.RARITY_NORMAL;
                    default://case STRONG:
                        return GravityMod.RARITY_STRONG;
                }
            }

            @Override
            public String getTranslationKey(ItemStack stack) {
                int itemDamage = stack.getItemDamage();
                IBlockState stateFromMeta = BlockGravityGenerator.this.getStateFromMeta(itemDamage);
                String extra = TIER.getName(stateFromMeta.getValue(TIER));
                if (stateFromMeta.getValue(REVERSED)) {
                    extra += ".reversed";
                }
                return this.getBlock().getTranslationKey() + '.' + extra;
//                return super.getTranslationKey(stack);
            }
        };
        itemBlock.setHasSubtypes(true);
        return itemBlock;
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tileEntity;
        if (worldIn instanceof ChunkCache) {
            tileEntity = ((ChunkCache)worldIn).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        }
        else {
            tileEntity = worldIn.getTileEntity(pos);
        }
        EnumFacing facing = EnumFacing.UP;
        boolean blockPowered = false;
        if (tileEntity instanceof TileGravityGenerator) {
            TileGravityGenerator tileGravityGenerator = (TileGravityGenerator)tileEntity;
            World world = tileGravityGenerator.getWorld();
            if (world != null) {
                blockPowered = world.isBlockPowered(pos);
            }
            else {
                // fallback I guess
                blockPowered = tileGravityGenerator.isPowered();
            }
            facing = tileGravityGenerator.getFacing();

            // The tile entity might not match the blockstate in the case that the stored tile entity nbt gets messed up, so we'll allow the tile entity to
            // override the REVERSED property
            boolean reversed = tileGravityGenerator.isReversed();
            state = state.withProperty(REVERSED, reversed);
        }
        return state.withProperty(FACING, facing).withProperty(ENABLED, blockPowered);
    }

    // Gravity generators don't have inventories at the moment
//    @Override
//    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
//        TileEntity tileEntity = worldIn.getTileEntity(pos);
//        if (tileEntity instanceof TileGravityGenerator && tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
//            IItemHandler itemHandler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
//            int slots = itemHandler.getSlots();
//            for (int i = 0; i < slots; i++) {
//                // Extracting items may not be necessary
//                ItemStack stackInSlot = itemHandler.extractItem(i, Integer.MAX_VALUE, false);
//                if (stackInSlot != null) {
//                    Block.spawnAsEntity(worldIn, pos, stackInSlot);
//                }
//            }
//        }
//
//        super.breakBlock(worldIn, pos, state);
//    }

    @Override
    public int damageDropped(IBlockState state) {
        return this.getMetaFromState(state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);

//            if (tileEntity instanceof TileGravityGenerator && playerIn instanceof EntityPlayerMP) {
//                EntityPlayerMP playerMP = (EntityPlayerMP)playerIn;
//                playerMP.connection.sendPacket(new SPacketOpenWindow(playerMP.currentWindowId, ));
//            }
            if (tileEntity instanceof TileGravityGenerator) {
                StaticGUIs.GUI_GRAVITY_GENERATOR.openGUI(playerIn, worldIn, pos);
            }
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileGravityGenerator) {
            TileGravityGenerator generator = (TileGravityGenerator)tileEntity;
            generator.setFacing(EnumFacing.getDirectionFromEntityLiving(pos, placer));
        }
        else {
            GravityMod.logWarning("BlockGravityGenerator places, but found a %s of class %s at %s",
                    tileEntity,
                    tileEntity == null ? null : tileEntity.getClass(),
                    pos);
        }
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (Boolean isReversed : REVERSED.getAllowedValues()) {
            for (EnumGravityTier tier : EnumGravityTier.values()) {
//                list.add(new ItemStack(this, 1, tier.ordinal()/*this.getMetaFromState(defaultState.withProperty(TIER, tier))*/));
                items.add(new ItemStack(this, 1, this.getMetaFromState(this.getDefaultState().withProperty(TIER, tier).withProperty(REVERSED, isReversed))));
            }
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        Collection<IProperty<?>> propertyNames = state.getPropertyKeys();
        if (propertyNames.contains(FACING)) {
            return new TileGravityGenerator(state.getValue(TIER), state.getValue(FACING), state.getValue(REVERSED));
        }
        else {
            GravityMod.logWarning("Failed to create tile entity in %s due to invalid blockstate %s", world, state);
            return null;
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase
            placer, EnumHand hand) {
        if (placer != null) {
            facing = EnumFacing.getDirectionFromEntityLiving(pos, placer);
        }
        int metadata = placer.getHeldItem(hand).getMetadata();
        return this.getStateFromMeta(metadata).withProperty(FACING, facing);
    }

    @Override
    public GenericItemBlock<BlockGravityGenerator> getItem() {
        return this.item;
    }

    @Override
    public String getModObjectName() {
        return "gravitygenerator";
    }


    @SideOnly(Side.CLIENT)
    @Override
    public void registerItemClient(IForgeRegistry<Item> registry) {
        for (Boolean isReversed : REVERSED.getAllowedValues()) {
            for (EnumGravityTier tier : EnumGravityTier.values()) {
                int meta = this.getMetaFromState(this.getDefaultState().withProperty(TIER, tier).withProperty(REVERSED, isReversed));
                ModelResourceLocation modelResourceLocation = new ModelResourceLocation(this.getRegistryName(),
                        ENABLED.getName() + "=" + ENABLED.getName(false) + ","
                                + FACING.getName() + "=" + FACING.getName(EnumFacing.NORTH) + ","
                                + REVERSED.getName() + "=" + REVERSED.getName(isReversed) + ","
                                + TIER.getName() + "=" + TIER.getName(tier));
                ModelLoader.registerItemVariants(this.item, modelResourceLocation);
                ModelLoader.setCustomModelResourceLocation(this.item, meta, modelResourceLocation);
            }
        }
    }

    @Override
    public void register(IForgeRegistry<Block> registry) {
        this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.UP).withProperty(REVERSED, Boolean.FALSE));

        super.register(registry);
    }
}
