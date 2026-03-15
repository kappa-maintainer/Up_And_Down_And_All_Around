package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import top.outlands.foundation.IExplicitTransformer;

import java.util.HashMap;
import java.util.Map;

public class NetHandlerPlayClientTransformer implements IExplicitTransformer {
    private static final Map<String, String> fieldMap = new HashMap<>() {
        {
            put("minY", "posY");
            put("field_72338_b", "field_70163_u");
        }
    };
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        out:
        for (var method : classNode.methods) {
            if (method.name.equals("handlePlayerPosLook")
                || method.name.equals("func_184330_a"))
            {
                InsnList list = method.instructions;
                for (var node : list) {
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && (min.name.equals("getEntityBoundingBox") || min.name.equals("func_174813_aQ"))
                    ) {
                        if (min.getNext() instanceof FieldInsnNode fin && fieldMap.containsKey(fin.name)) {
                            fin.name = fieldMap.get(fin.name);
                            fin.owner = "net/minecraft/entity/player/EntityPlayer";
                            list.remove(node);
                            break out;
                        } else {
                            throw  new RuntimeException("Impossible insn node in NetHandlerPlayClient");
                        }
                    }
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
