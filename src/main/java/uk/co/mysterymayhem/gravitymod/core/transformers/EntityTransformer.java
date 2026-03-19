package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;
import uk.co.mysterymayhem.gravitymod.core.ObfName;

import java.util.HashMap;
import java.util.Map;

public class EntityTransformer implements IExplicitTransformer {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";
    private static final Map<String, String> offsetMap = new HashMap<>(){
        {
            put(ObfName.get("calculateYOffset", "func_72323_b"), "reverseYOffset");
            put(ObfName.get("calculateXOffset", "func_72316_a"), "reverseXOffset");
            put(ObfName.get("calculateZOffset", "func_72322_c"), "reverseZOffset");
        }
    };
    
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        String moveRelative = ObfName.get("moveRelative", "func_191958_b");
        String move = ObfName.get("move", "func_70091_d");
        
        out:
        for (var method: classNode.methods) {
            InsnList list = method.instructions;
            String rotationYaw = ObfName.get("rotationYaw", "field_70177_z");
            if (method.name.equals(moveRelative)) {
                for (var node : list) {
                    if (node.getOpcode() == Opcodes.GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(rotationYaw)
                    ) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                "getRelativeYaw",
                                "(Lnet/minecraft/entity/Entity;)F",
                                false
                            )
                        );
                        list.remove(node);
                    }
                }
            } else if (method.name.equals(move)) {
                var node = list.getFirst();
                int count = 0;
                while (node != null) {
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && offsetMap.containsKey(min.name)
                    ) {
                        min.setOpcode(Opcodes.INVOKESTATIC);
                        min.owner = HOOKS;
                        min.name = offsetMap.get(min.name);
                        min.desc = "(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/util/math/AxisAlignedBB;D)D";
                        count++;
                        if (count == 10) {
                            break;
                        }
                    } else {
                        node = node.getNext();
                    }
                }
                count = 0;
                while (node != null) {
                    if (node.getOpcode() == Opcodes.NEW) {
                        
                        while (count < 5) {
                            list.remove(node.getNext());
                            count++;
                        }
                        
                        list.insert(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                "getImmutableBlockPosBelowEntity",
                                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(Opcodes.ALOAD, 0));
                        
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                String down = ObfName.get("down", "func_177977_b");
                while (node != null) {
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(down)
                    ) {
                        list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                "getRelativeDownBlockPos",
                                "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;")
                        );
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                String isRiding = ObfName.get("isRiding", "func_184218_aH");
                while (node != null) {
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(isRiding)
                    ) {
                        int[] vars = new int[3];
                        count = 0;
                        while (count < 3) {
                            node = node.getNext();
                            if (node.getOpcode() == Opcodes.DSTORE && node instanceof VarInsnNode vin) {
                                vars[count] = vin.var;
                                count++;
                            }
                        }
                        while (node.getNext().getOpcode() != Opcodes.GETSTATIC) {
                            node = node.getNext();
                        }
                        list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                        list.insertBefore(node, new VarInsnNode(Opcodes.DLOAD, vars[0]));
                        list.insertBefore(node, new VarInsnNode(Opcodes.DLOAD, vars[1]));
                        list.insertBefore(node, new VarInsnNode(Opcodes.DLOAD, vars[2]));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HOOKS,
                                "inverseAdjustXYZ",
                                "(Lnet/minecraft/entity/Entity;DDD)[D")
                        );
                        list.insertBefore(node, new InsnNode(Opcodes.DUP));
                        list.insertBefore(node, new InsnNode(Opcodes.DUP));
                        list.insertBefore(node, new InsnNode(Opcodes.ICONST_0));
                        list.insertBefore(node, new InsnNode(Opcodes.DALOAD));
                        list.insertBefore(node, new VarInsnNode(Opcodes.DSTORE, vars[0]));
                        list.insertBefore(node, new InsnNode(Opcodes.ICONST_1));
                        list.insertBefore(node, new InsnNode(Opcodes.DALOAD));
                        list.insertBefore(node, new VarInsnNode(Opcodes.DSTORE, vars[1]));
                        list.insertBefore(node, new InsnNode(Opcodes.ICONST_2));
                        list.insertBefore(node, new InsnNode(Opcodes.DALOAD));
                        list.insertBefore(node, new VarInsnNode(Opcodes.DSTORE, vars[2]));
                        
                        break out;
                    } else {
                        node = node.getNext();
                    }
                }
            }
        }
        
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
