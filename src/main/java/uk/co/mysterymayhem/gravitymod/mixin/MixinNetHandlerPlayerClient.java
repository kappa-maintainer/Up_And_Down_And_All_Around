package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = NetHandlerPlayClient.class, priority = 0)
public class MixinNetHandlerPlayerClient {
}
