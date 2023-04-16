package uk.co.mysterymayhem.gravitymod.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.Bytecode;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String s) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return Arrays.asList(
                "MixinEntityLivingBase",
                "MixinEntity",
                "MixinWorld",
                "MixinEntityPlayerSP",
                "MixinRenderLivingBase",
                "MixinEntityRenderer",
                "MixinAbstractClientPlayer",
                "MixinEntityOtherPlayerMP",
                "MixinEntityPlayerMP",
                "MixinItemStack",
                "MixinNetHandlerPlayerClient",
                "MixinNetHandlerPlayerServer",
                "MixinSoundManager"
        );
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

        Transformer.logger.info("Running post mixin Patcher on" + s);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(cw);
        ClassNode result = new ClassNode();
        ClassReader cr = new ClassReader(new Transformer().transform(s, s, cw.toByteArray()));
        cr.accept(result, 0);

        Bytecode.replace(result, classNode);
    }
}
