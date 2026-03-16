package uk.co.mysterymayhem.gravitymod.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.outlands.foundation.TransformerDelegate;
import uk.co.mysterymayhem.gravitymod.asm.Transformer;
import uk.co.mysterymayhem.gravitymod.core.transformers.EntityPlayerSPTransformer;
import uk.co.mysterymayhem.gravitymod.core.transformers.EntityRendererTransformer;
import uk.co.mysterymayhem.gravitymod.core.transformers.NetHandlerPlayClientTransformer;
import uk.co.mysterymayhem.gravitymod.core.transformers.SoundManagerTransformer;
import uk.co.mysterymayhem.gravitymod.core.transformers.SuperClassReplacingTransformer;

import java.util.Map;

/**
 * Created by Mysteryem on 2016-08-16.
 */
@IFMLLoadingPlugin.TransformerExclusions({"uk.co.mysterymayhem.gravitymod.core.FMLLoadingPlugin"})
@IFMLLoadingPlugin.Name("gravitymod")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class FMLLoadingPlugin implements IFMLLoadingPlugin {
    public static final Logger logger = LogManager.getLogger("UpAndDown-Core");

    @Override
    public String[] getASMTransformerClass() {
        TransformerDelegate.registerExplicitTransformer(
            new SuperClassReplacingTransformer(),
            "net.minecraft.client.entity.AbstractClientPlayer",
            "net.minecraft.entity.player.EntityPlayerMP"
        );
        TransformerDelegate.registerExplicitTransformer(
            new SoundManagerTransformer(),
            "net.minecraft.client.audio.SoundManager"
        );
        TransformerDelegate.registerExplicitTransformer(
            new EntityPlayerSPTransformer(),
            "net.minecraft.client.entity.EntityPlayerSP"
        );
        TransformerDelegate.registerExplicitTransformer(
            new NetHandlerPlayClientTransformer(),
            "net.minecraft.client.network.NetHandlerPlayClient"
        );
        TransformerDelegate.registerExplicitTransformer(
            new EntityRendererTransformer(),
            "net.minecraft.client.renderer.EntityRenderer"
        );
        return new String[]{Transformer.class.getName()};
        //return null;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
//        return GravityMod.class.getName();
    }

    @Override
    public String getSetupClass() {
        return null;//GravityMod.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // nothing to do here
    }

}
