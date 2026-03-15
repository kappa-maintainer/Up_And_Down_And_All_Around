package uk.co.mysterymayhem.gravitymod.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.outlands.foundation.TransformerDelegate;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.asm.transformers.EntityPlayerSPTransformer;
import uk.co.mysterymayhem.gravitymod.asm.transformers.SoundManagerTransformer;
import uk.co.mysterymayhem.gravitymod.asm.transformers.SuperClassReplacingTransformer;

import java.util.Map;

/**
 * Created by Mysteryem on 2016-08-16.
 */
@IFMLLoadingPlugin.TransformerExclusions({"uk.co.mysterymayhem.gravitymod.asm.FMLLoadingPlugin"})
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
