package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import top.outlands.foundation.IExplicitTransformer;
import uk.co.mysterymayhem.gravitymod.core.ObfableName;

import java.util.HashMap;
import java.util.Map;

public class RenderLivingBaseTransformer implements IExplicitTransformer {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";

    private static final Map<String, Pair<String, String>> fieldMap = new HashMap<>(){
        {
            put(ObfableName.get("prevRotationYawHead", "field_70758_at"), Pair.of("getPrevRelativeYawHead", "(Lnet/minecraft/entity/EntityLivingBase;)F"));
            put(ObfableName.get("rotationYawHead", "field_70759_as"), Pair.of("getRelativeYawHead", "(Lnet/minecraft/entity/EntityLivingBase;)F"));
            put(ObfableName.get("prevRotationPitch", "field_70127_C"), Pair.of("getRelativePrevPitch", "(Lnet/minecraft/entity/Entity;)F"));
            put(ObfableName.get("rotationPitch", "field_70125_A"), Pair.of("getRelativePitch", "(Lnet/minecraft/entity/Entity;)F"));
        }
    };
    
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        String doRender = ObfableName.get("doRender", "func_76986_a");
        for (var method : classNode.methods) {
            if (method.name.equals(doRender)) {
                InsnList list = method.instructions;
                for (var node : list) {
                    if (node.getOpcode() == Opcodes.GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fieldMap.containsKey(fin.name)
                    ) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                fieldMap.get(fin.name).getLeft(),
                                fieldMap.get(fin.name).getRight()
                            )
                        );
                        list.remove(node);
                    }
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
