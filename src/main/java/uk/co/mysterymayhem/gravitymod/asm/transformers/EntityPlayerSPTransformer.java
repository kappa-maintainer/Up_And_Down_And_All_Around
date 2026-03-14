package uk.co.mysterymayhem.gravitymod.asm.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;

import java.util.HashMap;
import java.util.Map;

public class EntityPlayerSPTransformer implements IExplicitTransformer {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";
    private static final Map<String, String> ouwpFieldMap = new HashMap<>() {
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
            InsnList list = method.instructions;
            switch (method.name) {
                case "onUpdateWalkingPlayer":
                case "func_175161_p": {
                    // Replace all axisalignedbb.minY to this.posY
                    for (var node : list) {
                        if (node.getOpcode() == Opcodes.GETFIELD
                            && node instanceof FieldInsnNode fin
                            && ouwpFieldMap.containsKey(fin.name)
                        ) {
                            if (node.getPrevious() instanceof VarInsnNode vin
                                && vin.getOpcode() == Opcodes.ALOAD
                            ) {
                                vin.var = 0;
                                fin.name = ouwpFieldMap.get(fin.name);
                                fin.desc = "D";
                                fin.owner = "net/minecraft/client/entity/EntityPlayerSP";
                            }
                        }
                    }
                    break;
                }
                case "pushOutOfBlocks":
                case "func_145771_j": {
                    /*
                            } else {
                              -BlockPos blockpos = new BlockPos(x, y, z);
                              +BlockPos blockpos = Hooks.makeRelativeBlockPos(new BlockPos(x, y, z), this);
                               double d0 = x - blockpos.getX();
                     */
                    for (var node : list) {
                        if (node.getOpcode() == Opcodes.INVOKESPECIAL
                            && node instanceof MethodInsnNode min
                            && min.owner.equals("net/minecraft/util/math/BlockPos")
                        ) {
                            list.insert(
                                min,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "makeRelativeBlockPos",
                                    "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                    false
                                )
                            );
                            list.insert(min, new VarInsnNode(Opcodes.ALOAD, 0));
                            break;
                        }
                    }
                    break;
                }
                /*
                       +if (axisalignedbb instanceof GravityAxisAlignedBB) {
                       +   Hooks.pushEntityPlayerSPOutOfBlocks(this, axisalignedbb);
                       +} else {
                           PlayerSPPushOutOfBlocksEvent event = new PlayerSPPushOutOfBlocksEvent(this, axisalignedbb);
                           if (!MinecraftForge.EVENT_BUS.post(event)) {
                           ...
                           }
                        }
                 */
                    
                case "onLivingUpdate":
                case "func_70636_d": {
                    VarInsnNode store = null;
                    LabelNode postEventBlock = null;
                    LabelNode getFoodStatsBlock = null;
                    LabelNode temp = null;
                    for (var node : list) {
                        if (node instanceof LabelNode labelNode) {
                            temp = labelNode;
                        }
                        if (store == null && node.getOpcode() == Opcodes.ASTORE && node instanceof VarInsnNode vin) {
                            store = vin;
                        }
                        if (node.getOpcode() == Opcodes.NEW
                            && node instanceof TypeInsnNode tin
                            && tin.desc.equals("net/minecraftforge/client/event/PlayerSPPushOutOfBlocksEvent")
                        ) {
                            postEventBlock = temp;
                        }
                        if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && node instanceof MethodInsnNode min
                            && (min.name.equals("getFoodStats") || min.name.equals("func_71024_bL"))
                        ) {
                            getFoodStatsBlock = temp;
                            break;
                        }
                    }
                    if (store == null || postEventBlock == null || getFoodStatsBlock == null) {
                        throw new RuntimeException("Transform failed in " + this.getClass().getName() + "'s onLivingUpdate");
                    }
                    InsnList tempList = new InsnList();
                    tempList.add(new VarInsnNode(Opcodes.ALOAD, store.var));
                    tempList.add(
                        new TypeInsnNode(
                            Opcodes.INSTANCEOF,
                            "uk/co/mysterymayhem/gravitymod/common/util/boundingboxes/GravityAxisAlignedBB"
                        )
                    );
                    tempList.add(new JumpInsnNode(Opcodes.IFEQ, postEventBlock));
                    tempList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    tempList.add(new VarInsnNode(Opcodes.ALOAD, store.var));
                    tempList.add(
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HOOKS,
                            "pushEntityPlayerSPOutOfBlocks",
                            "(Luk/co/mysterymayhem/gravitymod/asm/EntityPlayerWithGravity;Lnet/minecraft/util/math/AxisAlignedBB;)V",
                            false
                        )
                    );
                    tempList.add(new JumpInsnNode(Opcodes.GOTO, getFoodStatsBlock));

                    list.insert(store, tempList);
                    break;
                }
                case "isHeadspaceFree": {
                    /*
                         private boolean isHeadspaceFree(BlockPos pos, int height) {
                           +pos = Hooks.makeRelativeBlockPos(pos, this);
                            for (int y = 0; y < height; y++) {
                     */
                    for (var node : list) {
                        if (node.getOpcode() == Opcodes.ICONST_0) {
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 1));
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "makeRelativeBlockPos",
                                    "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                    false
                                )
                            );
                            list.insertBefore(node, new VarInsnNode(Opcodes.ASTORE, 1));
                            break;
                        }
                    }
                    break;
                }
                case "updateAutoJump":
                case "func_189810_i": {
                    var node = list.getFirst();
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.NEW
                            && node instanceof TypeInsnNode tin
                            && tin.desc.equals("net/minecraft/util/math/Vec3d")
                        ) {
                            for (int j = 10; j > 0; j--) {
                                node = node.getNext();
                                list.remove(node.getPrevious());
                            }
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getBottomOfEntity",
                                    "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                    false
                                )
                            );
                            break;
                        } else {
                            node = node.getNext();
                        }
                    }
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.GETFIELD
                            && node instanceof FieldInsnNode fin
                            && (fin.name.equals("posX") || fin.name.equals("field_70165_t"))
                        ) {
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getOriginRelativePosX",
                                    "(Lnet/minecraft/entity/Entity;)D",
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && node instanceof MethodInsnNode min
                            && (min.name.equals("getEntityBoundingBox") || min.name.equals("func_174813_aQ"))
                        ) {
                            list.remove(node.getNext());
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getOriginRelativePosY",
                                    "(Lnet/minecraft/entity/Entity;)D",
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.GETFIELD
                            && node instanceof FieldInsnNode fin
                            && (fin.name.equals("posZ") || fin.name.equals("field_70161_v"))
                        ) {
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getOriginRelativePosZ",
                                    "(Lnet/minecraft/entity/Entity;)D",
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.INVOKESPECIAL) {
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "adjustVec",
                                    "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                    false
                                )
                            );
                            list.insert(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            break;
                        } else {
                            node = node.getNext();
                        }
                    }
                    int count = 0;
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.GETFIELD
                            && node instanceof FieldInsnNode fin
                            && (fin.name.equals("rotationYaw") || fin.name.equals("field_70177_z"))
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
                            node = node.getNext();
                            list.remove(node.getPrevious());
                            if (++count >= 2) {
                                break;
                            }
                        } else {
                            node = node.getNext();
                        }
                    }
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && node instanceof MethodInsnNode min
                            && (min.name.equals("getForward") || min.name.equals("func_189651_aD"))
                        ) {
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.ASTORE && node instanceof VarInsnNode vin && vin.var == 13) {
                            while (node.getPrevious().getOpcode() != Opcodes.NEW) {
                                list.remove(node.getPrevious());
                            }
                            list.remove(node.getPrevious());
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getBlockPosAtTopOfPlayer",
                                    "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                    false
                                )
                            );
                            list.insert(node, new VarInsnNode(Opcodes.ASTORE, 10));
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "adjustVec",
                                    "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                    false
                                )
                            );
                            list.insert(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insert(node, new VarInsnNode(Opcodes.ALOAD, 10));
                            break;
                        } else {
                            node = node.getNext();
                        }
                    }
                    node = commonGetRelativeUpPatch(node, list);
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.NEW
                            && node instanceof TypeInsnNode tin
                            && tin.desc.equals("net/minecraft/util/math/AxisAlignedBB")
                        ) {
                            node = node.getNext().getNext();
                            list.remove(node.getPrevious());
                            list.remove(node.getPrevious());
                            while (node.getNext().getOpcode() != Opcodes.INVOKEVIRTUAL) {
                                node = node.getNext();
                            }
                            list.remove(node.getNext());
                            list.remove(node.getNext());
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "constructNewGAABBFrom2Vec3d",
                                    "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/AxisAlignedBB;",
                                    false
                                )
                            );
                            list.insert(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "addAdjustedVector",
                                    "(Lnet/minecraft/util/math/Vec3d;DDDLnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                    false
                                )
                            );
                            list.insert(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            break;
                        } else {
                            node = node.getNext();
                        }
                    }
                    count = 0;
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && node instanceof MethodInsnNode min
                            && (min.name.equals("add") || min.name.equals("func_178787_e"))
                        ) {
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "addAdjustedVector",
                                    "(Lnet/minecraft/util/math/Vec3d;DDDLnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                    false
                                )
                            );
                            node = node.getNext();
                            list.remove(node.getPrevious());
                            if (++count >= 2) {
                                break;
                            }
                        } else {
                            node = node.getNext();
                        }
                    }
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.DCONST_1 && node instanceof InsnNode) {
                            node = node.getNext();
                            list.remove(node.getPrevious());
                            list.insertBefore(node, new InsnNode(Opcodes.DCONST_0));
                            while (node.getOpcode() != Opcodes.INVOKESPECIAL) {
                                node = node.getNext();
                            }
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "addAdjustedVector",
                                    "(Lnet/minecraft/util/math/Vec3d;DDDLnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                    false
                                )
                            );
                            list.insert(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insert(node, new InsnNode(Opcodes.DCONST_0));
                            list.insert(node, new InsnNode(Opcodes.DCONST_1));
                            list.insert(node, new InsnNode(Opcodes.DCONST_0));
                            break;
                        } else {
                            node = node.getNext();
                        }
                    }
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.GETFIELD
                            && node instanceof FieldInsnNode fin
                            && (fin.name.equals("maxY") || fin.name.equals("field_72337_e"))
                        ) {
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getRelativeTopOfBB",
                                    "(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/entity/Entity;)D",
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && node instanceof MethodInsnNode min
                            && (min.name.equals("up") || min.name.equals("func_177984_a"))
                        ) {
                            list.insert(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getRelativeUpBlockPos",
                                    "(Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.GETFIELD
                            && node instanceof FieldInsnNode fin
                            && (fin.name.equals("maxY") || fin.name.equals("field_72337_e"))
                        ) {
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getRelativeTopOfBB",
                                    "(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/entity/Entity;)D",
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
                    while (node != null) {
                        if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && node instanceof MethodInsnNode min
                            && (min.name.equals("getY") || min.name.equals("func_177956_o"))
                        ) {
                            list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                            list.insertBefore(
                                node,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    HOOKS,
                                    "getRelativeYOfBlockPos",
                                    "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)I",
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
                    node = commonGetRelativeBottomOfBBPatch(node, list);
                    node = commonGetRelativeUpPatch(node, list);
                    commonGetRelativeBottomOfBBPatch(node, list);
                    break out;
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
    
    private AbstractInsnNode commonGetRelativeUpPatch(AbstractInsnNode node, InsnList list) {
        while (node != null) {
            if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                && node instanceof MethodInsnNode min
                && (min.name.equals("up") || min.name.equals("func_177984_a"))
            ) {
                list.insert(
                    node,
                    new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "getRelativeUpBlockPos",
                        "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
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
        return node;
    }
    
    private AbstractInsnNode commonGetRelativeBottomOfBBPatch(AbstractInsnNode node, InsnList list) {
        while (node != null) {
            if (node.getOpcode() == Opcodes.GETFIELD
                && node instanceof FieldInsnNode fin
                && (fin.name.equals("minY") || fin.name.equals("field_72338_b"))
            ) {
                list.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                list.insertBefore(
                    node,
                    new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "getRelativeBottomOfBB",
                        "(Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/entity/Entity;)D",
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
        return node;
    }
}
