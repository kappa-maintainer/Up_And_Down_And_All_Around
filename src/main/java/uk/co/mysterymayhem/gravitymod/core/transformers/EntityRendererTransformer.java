package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;
import uk.co.mysterymayhem.gravitymod.core.ObfName;

public class EntityRendererTransformer implements IExplicitTransformer, Opcodes {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        String getMouseOver = ObfName.get("getMouseOver", "func_78473_a");
        String drawNameplate = ObfName.get("drawNameplate", "func_189692_a");
        out:
        for (var method : classNode.methods) {
            if (method.name.equals(getMouseOver)) {
                InsnList list = method.instructions;
                String getEntityBoundingBox = ObfName.get("getEntityBoundingBox", "func_174813_aQ");
                for (var node : list) {
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(getEntityBoundingBox)
                    ) {
                        min.setOpcode(INVOKESTATIC);
                        min.name = "getVanillaEntityBoundingBox";
                        min.owner = HOOKS;
                        min.desc = "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/AxisAlignedBB;";
                        min.itf = false;
                        break;
                    }
                }
            } else if (method.name.equals(drawNameplate)) {
                InsnList list = method.instructions;
                String disableLighting = ObfName.get("disableLighting", "func_179140_f");
                for (var node : list) {
                    if (node.getOpcode() == INVOKESTATIC
                        && node instanceof MethodInsnNode min
                        && min.name.equals(disableLighting)
                    ) {
                        list.insertBefore(node, new VarInsnNode(ILOAD, 8));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "runNameplateCorrection",
                                "(Z)V",
                                false
                            )
                        );
                        break out;
                    }
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
