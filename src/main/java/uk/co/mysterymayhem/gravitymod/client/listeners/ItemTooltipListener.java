package uk.co.mysterymayhem.gravitymod.client.listeners;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import uk.co.mysterymayhem.gravitymod.api.IWeakGravityEnabler;
import uk.co.mysterymayhem.gravitymod.common.config.ConfigHandler;
import uk.co.mysterymayhem.gravitymod.common.items.materials.ItemArmourPaste;
import uk.co.mysterymayhem.gravitymod.common.items.materials.ItemGravityDustInducer;
import uk.co.mysterymayhem.gravitymod.common.modsupport.ModSupport;
import uk.co.mysterymayhem.mystlib.util.KeyBindingUtil;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by Mysteryem on 2016-11-13.
 */
@SideOnly(Side.CLIENT)
public class ItemTooltipListener {

    @SubscribeEvent
    public static void onTooltipDisplay(ItemTooltipEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != null && !(player instanceof FakePlayer)) {
            ItemStack itemStack = event.getItemStack();
            if (!itemStack.isEmpty()) {
                List<String> toolTips = event.getToolTip();
                if (itemStack.getItem() instanceof IWeakGravityEnabler) {
                    addWeakGravityTooltip(toolTips, player);
                    addNormalGravityTooltip(toolTips, player);
                }
                else if (ItemArmourPaste.hasPasteTag(itemStack)) {
                    toolTips.add(I18n.format("mouseovertext.gravitymod.hasarmourpaste"));
//                    toolTips.add("Affected by normal strength and stronger gravity");
                    addNormalGravityTooltip(toolTips, player);
                }
                if (ItemGravityDustInducer.hasDistorterTag(itemStack)) {
                    addInducerTooltip(toolTips, player);
                }
            }
        }
    }

    public static void addWeakGravityTooltip(@Nonnull List<String> toolTips, @Nonnull EntityPlayer player) {
        int numWeakEnablersWorn = getNumWeakEnablersWorn(player);
        int numRequired = ConfigHandler.numWeakGravityEnablersRequiredForWeakGravity;
        boolean enoughEquipped = numWeakEnablersWorn >= numRequired;
        toolTips.add(I18n.format(
                "mouseovertext.gravitymod.weaktooltip",
                enoughEquipped ? "f" : "c",
                numWeakEnablersWorn,
                numRequired));
//        toolTips.add((enoughEquipped ? "§f" : "§c") + numWeakEnablersWorn + "§7/" + numRequired + " for §fweak§7 gravity");
    }

    public static void addNormalGravityTooltip(@Nonnull List<String> toolTips, @Nonnull EntityPlayer player) {
        KeyBinding keyBindSneak = Minecraft.getMinecraft().gameSettings.keyBindSneak;
        int numWeakEnablerCountsAs = ConfigHandler.numNormalEnablersWeakEnablersCountsAs;
        boolean weakEnablersDontCount = numWeakEnablerCountsAs == 0;
        if (!weakEnablersDontCount && KeyBindingUtil.isKeyPressed(keyBindSneak)) {
            toolTips.add(I18n.format(
                    "mouseovertext.gravitymod.normaltooltip.sneak",
                    getNumWeakEnablersWorn(player),
                    numWeakEnablerCountsAs,
                    getNumNormalEnablersWorn(player)));
//            toolTips.add("§f" + getNumWeakEnablersWorn(player) + " weak§7(x" + numWeakEnablerCountsAs + ") + §5" + getNumNormalEnablersWorn(player) + " normal§7");
        }
        else {
            int combinedNormalEnablersWorn = getCombinedNormalEnablersWorn(player);
            int numRequired = ConfigHandler.numNormalGravityEnablersRequiredForNormalGravity;
            boolean enoughEquipped = combinedNormalEnablersWorn >= numRequired;
            toolTips.add(I18n.format(
                    "mouseovertext.gravitymod.normaltooltip",
                    enoughEquipped ? "5" : "c",
                    combinedNormalEnablersWorn,
                    numRequired,
                    weakEnablersDontCount ? "" : "(" + keyBindSneak.getDisplayName() + ")"));
//            toolTips.add((enoughEquipped ? "§5" : "§c") + combinedNormalEnablersWorn + "§7/" + numRequired + " for §5normal§7 gravity (" + keyBindSneak.getDisplayName() + ")");
        }
    }

    public static void addInducerTooltip(@Nonnull List<String> toolTips, @Nonnull EntityPlayer player) {
        toolTips.add(I18n.format("mouseovertext.gravitymod.hasinducer"));
    }

    private static int getNumWeakEnablersWorn(@Nonnull EntityPlayer player) {
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        int numWeakGravityEnablers = 0;
        for (ItemStack stack : armorInventory) {
            if (stack != null && stack.getItem() instanceof IWeakGravityEnabler) {
                numWeakGravityEnablers++;
            }
        }
        if (ModSupport.isModLoaded(ModSupport.BAUBLES_MOD_ID)) {
            IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
            int slots = baublesHandler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack stack = baublesHandler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof IWeakGravityEnabler) {
                    numWeakGravityEnablers++;
                }
            }

        }
        return numWeakGravityEnablers;
    }

    // Does not count weak
    private static int getNumNormalEnablersWorn(@Nonnull EntityPlayer player) {
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        int numNormalGravityEnablers = 0;
        for (ItemStack stack : armorInventory) {
            if (stack != null && ItemArmourPaste.hasPasteTag(stack)) {
                numNormalGravityEnablers++;
            }
        }
        if (ModSupport.isModLoaded(ModSupport.BAUBLES_MOD_ID)) {
            IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
            int slots = baublesHandler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack stack = baublesHandler.getStackInSlot(i);
                if (!stack.isEmpty() && ItemArmourPaste.hasPasteTag(stack)) {
                    numNormalGravityEnablers++;
                }
            }

        }
        return numNormalGravityEnablers;
    }

    // Counts weak and gives them the value defined in config
    private static int getCombinedNormalEnablersWorn(@Nonnull EntityPlayer player) {
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        int numNormalGravityEnablersIncludingWeakEnablers = 0;
        for (ItemStack stack : armorInventory) {
            if (stack != null) {
                if (ItemArmourPaste.hasPasteTag(stack)) {
                    numNormalGravityEnablersIncludingWeakEnablers++;
                }
                else if (stack.getItem() instanceof IWeakGravityEnabler) {
                    numNormalGravityEnablersIncludingWeakEnablers += ConfigHandler.numNormalEnablersWeakEnablersCountsAs;
                }
            }
        }
        if (ModSupport.isModLoaded(ModSupport.BAUBLES_MOD_ID)) {
            IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
            int slots = baublesHandler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack stack = baublesHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (ItemArmourPaste.hasPasteTag(stack)) {
                        numNormalGravityEnablersIncludingWeakEnablers++;
                    }
                    else if (stack.getItem() instanceof IWeakGravityEnabler) {
                        numNormalGravityEnablersIncludingWeakEnablers += ConfigHandler.numNormalEnablersWeakEnablersCountsAs;
                    }
                }
            }

        }
        return numNormalGravityEnablersIncludingWeakEnablers;
    }
}
