package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;
import uk.co.mysterymayhem.gravitymod.core.ObfName;

import java.util.HashMap;
import java.util.Map;

public class SoundManagerTransformer implements IExplicitTransformer {
    
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";
    private static final Map<String, String> fieldMap = new HashMap<>(){
        {
            put(ObfName.get("prevRotationPitch", "field_70127_C"), "getRelativePrevPitch");
            put(ObfName.get("prevRotationYaw", "field_70126_B"), "getRelativePrevYaw");
            put(ObfName.get("rotationPitch", "field_70125_A"), "getRelativePitch");
            put(ObfName.get("rotationYaw", "field_70177_z"), "getRelativeYaw");
        }
    };
    
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        int counter = 6;
        out:
        for (var method : classNode.methods) {
            if (method.name.equals("setListener")
                && method.desc.equals("(Lnet/minecraft/entity/Entity;F)V"))
            {
                InsnList list = method.instructions;
                for (var node : list) {
                    if (counter > 0
                        && node.getOpcode() == Opcodes.GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fieldMap.containsKey(fin.name)
                    ) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                fieldMap.get(fin.name),
                                "(Lnet/minecraft/entity/Entity;)F",
                                false
                            )
                        );
                        list.remove(node);
                        counter--;
                    }
                    if (counter == 0
                        && node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.desc.equals("(FFFFFF)V")
                    ) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                "setListenerOrientationHook",
                                "(Lpaulscode/sound/SoundSystem;FFFFFFLnet/minecraft/entity/Entity;)V",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(Opcodes.ALOAD, 1));
                        list.remove(node);
                        break out;
                    }
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
