package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import top.outlands.foundation.IExplicitTransformer;
import uk.co.mysterymayhem.gravitymod.core.FMLLoadingPlugin;
import uk.co.mysterymayhem.gravitymod.core.InsnPrinter;
import uk.co.mysterymayhem.gravitymod.core.ObfName;

import java.util.HashMap;
import java.util.Map;

public class EntityLivingBaseTransformer implements IExplicitTransformer, Opcodes {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";

    private static final Map<String, Pair<String, String>> onUpdateFieldMap = new HashMap<>(){
        {
            put(ObfName.get("posX", "field_70165_t"), Pair.of("getRelativePosX", "(Lnet/minecraft/entity/Entity;)D"));
            put(ObfName.get("prevPosX", "field_70169_q"), Pair.of("getRelativePrevPosX", "(Lnet/minecraft/entity/Entity;)D"));
            put(ObfName.get("posZ", "field_70161_v"), Pair.of("getRelativePosZ", "(Lnet/minecraft/entity/Entity;)D"));
            put(ObfName.get("prevPosZ", "field_70166_s"), Pair.of("getRelativePrevPosZ", "(Lnet/minecraft/entity/Entity;)D"));
            put(ObfName.get("rotationYaw", "field_70177_z"), Pair.of("getRelativeYaw", "(Lnet/minecraft/entity/Entity;)F"));
        }
    };
    @Override
    public byte[] transform(byte[] bytes) {

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);

        String moveRelative = ObfName.get("moveRelative", "func_191958_b");
        String updateDistance = ObfName.get("updateDistance", "func_110146_f");
        String jump = ObfName.get("jump", "func_70664_aZ");
        String onUpdate = ObfName.get("onUpdate", "func_70071_h_");
        String travel = ObfName.get("travel", "func_191986_a");

        for (var method : classNode.methods) {
            InsnList list = method.instructions;
            if (method.name.equals(moveRelative) || method.name.equals(updateDistance) || method.name.equals(jump)) {
                String rotationYaw = ObfName.get("rotationYaw", "field_70177_z");
                for (var node : list) {
                    if (node.getOpcode() == GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(rotationYaw)
                    ) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getRelativeYaw",
                                "(Lnet/minecraft/entity/Entity;)F",
                                false
                            )
                        );
                        list.remove(node);
                    }
                }
            } else if (method.name.equals(onUpdate)) {
                int count = 0;
                for (var node : list) {
                    if (node.getOpcode() == GETFIELD && node instanceof FieldInsnNode fin && onUpdateFieldMap.containsKey(fin.name)) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                onUpdateFieldMap.get(fin.name).getLeft(),
                                onUpdateFieldMap.get(fin.name).getRight(),
                                false
                            )
                        );
                        list.remove(node);
                        count++;
                        if (count == 5) {
                            break;
                        }
                    }
                }
            } else if (method.name.equals(travel)) {
                var node = list.getFirst();
                String getLookVec = ObfName.get("getLookVec", "func_70040_Z");
                while (node != null) {
                    if (node.getOpcode() == INVOKEVIRTUAL && node instanceof MethodInsnNode min && min.name.equals(getLookVec)) {
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getRelativeLookVec",
                                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                false
                            )
                        );
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                String rotationPitch = ObfName.get("rotationPitch", "field_70125_A");
                while (node != null) {
                    if (node.getOpcode() == GETFIELD && node instanceof FieldInsnNode fin && fin.name.equals(rotationPitch)) {
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getRelativePitch",
                                "(Lnet/minecraft/entity/Entity;)F",
                                false
                            )
                        );
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                String retain = ObfName.get("retain", "func_185345_c");
                while (node != null) {
                    if (node.getOpcode() == INVOKESTATIC && node instanceof MethodInsnNode min && min.name.equals(retain)) {
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        list.insertBefore(node, new InsnNode(POP2));
                        list.insertBefore(node, new InsnNode(POP2));
                        list.insertBefore(node, new InsnNode(POP2));
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getBlockPosBelowEntity",
                                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;",
                                false
                            )
                        );
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                String world = ObfName.get("world", "field_70170_p");
                String getBlockState = ObfName.get("getBlockState", "func_180495_p");
                int count = 0;
                while (node != null) {
                    if (node.getOpcode() == GETFIELD && node instanceof FieldInsnNode fin && fin.name.equals(world)) {
                        count++;
                        if (count == 3) {
                            node = node.getNext().getNext().getNext();
                            while (!(node.getOpcode() == INVOKEVIRTUAL && node instanceof MethodInsnNode min && min.name.equals(getBlockState))) {
                                if (node instanceof FrameNode || node instanceof LabelNode || node instanceof LineNumberNode) {
                                    node = node.getNext();
                                } else {
                                    node = node.getNext();
                                    list.remove(node.getPrevious());
                                }
                            }
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    INVOKESTATIC,
                                    HOOKS,
                                    "setPooledMutableBlockPosToBelowEntity",
                                    "(Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;",
                                    false
                                )
                            );
                            break;
                        } else {
                            node = node.getNext();
                        }
                    } else {
                        node = node.getNext();
                    }
                }
                String release = ObfName.get("release", "func_185344_t");
                while (node != null
                    && !(node.getOpcode() == INVOKEVIRTUAL && node instanceof MethodInsnNode min && min.name.equals(release))
                ) {
                    node = node.getNext();
                }
                String posY = ObfName.get("posY", "field_70163_u");
                count = 0;
                while (node != null) {
                    if (node.getOpcode() == GETFIELD && node instanceof FieldInsnNode fin && fin.name.equals(posY)) {
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getRelativePosY",
                                "(Lnet/minecraft/entity/Entity;)D",
                                false
                            )
                        );
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        count++;
                        if (count == 4) {
                            break;
                        }
                    } else {
                        node = node.getNext();
                    }
                }
                String limbSwingAmount = ObfName.get("limbSwingAmount", "field_70721_aZ");
                while (node != null) {
                    if (node.getOpcode() == GETFIELD && node instanceof FieldInsnNode fin && fin.name.equals(limbSwingAmount)) {
                        node = node.getPrevious().getPrevious();
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "makePositionRelative",
                                "(Lnet/minecraft/entity/EntityLivingBase;)V",
                                false
                            )
                        );
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                node = list.getLast();
                while (node.getOpcode() != PUTFIELD) {
                    node = node.getPrevious();
                }
                list.insert(
                    node,
                    new MethodInsnNode(
                        INVOKESTATIC,
                        HOOKS,
                        "makePositionAbsolute",
                        "(Lnet/minecraft/entity/EntityLivingBase;)V",
                        false
                    )
                );
                list.insert(node, new VarInsnNode(ALOAD, 0));
                
                /*
                var bv = new BasicVerifier();
                Analyzer<BasicValue> analyzer = new Analyzer<>(bv);
                try {
                    analyzer.analyze(classNode.name, method);
                } catch (AnalyzerException e) {
                    InsnPrinter.printMethod(method);
                    FMLLoadingPlugin.logger.info("=======================================");
                    FMLLoadingPlugin.logger.info(InsnPrinter.prettyPrint(e.node.getPrevious()));
                    FMLLoadingPlugin.logger.info(InsnPrinter.prettyPrint(e.node));
                    FMLLoadingPlugin.logger.info(InsnPrinter.prettyPrint(e.node.getNext()));
                    throw new RuntimeException(e);
                }*/
            }
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
