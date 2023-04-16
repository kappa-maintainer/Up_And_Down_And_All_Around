package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = NetHandlerPlayServer.class, priority = 0)
public class MixinNetHandlerPlayerServer {
}
