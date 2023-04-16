package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ItemStack.class, priority = 0)
public class MixinItemStack {
}
