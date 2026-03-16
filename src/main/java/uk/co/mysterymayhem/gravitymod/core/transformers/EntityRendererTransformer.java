package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;

public class EntityRendererTransformer implements IExplicitTransformer {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        out:
        for (var method : classNode.methods) {
            if (method.name.equals("getMouseOver") || method.name.equals("func_78473_a")) {
                InsnList list = method.instructions;
                for (var node : list) {
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && (min.name.equals("getEntityBoundingBox") || min.name.equals("func_174813_aQ"))
                    ) {
                        min.setOpcode(Opcodes.INVOKESTATIC);
                        min.name = "getVanillaEntityBoundingBox";
                        min.owner = HOOKS;
                        min.desc = "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/AxisAlignedBB;";
                        min.itf = false;
                        break;
                    }
                }
            }
            if (method.name.equals("drawNameplate") || method.name.equals("func_189692_a")) {
                for (var node : method.instructions) {
                    if (node.getOpcode() == Opcodes.INVOKESTATIC
                        && node instanceof MethodInsnNode min
                        && (min.name.equals("disableLighting") || min.name.equals("func_179140_f"))
                    ) {
                        method.instructions.insertBefore(node, new VarInsnNode(Opcodes.ILOAD, 8));
                        method.instructions.insertBefore(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
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
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
