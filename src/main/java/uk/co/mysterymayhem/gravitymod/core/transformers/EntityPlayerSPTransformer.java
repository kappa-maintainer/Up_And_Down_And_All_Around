package uk.co.mysterymayhem.gravitymod.core.transformers;

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
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import top.outlands.foundation.IExplicitTransformer;
import uk.co.mysterymayhem.gravitymod.core.FMLLoadingPlugin;
import uk.co.mysterymayhem.gravitymod.core.InsnPrinter;
import uk.co.mysterymayhem.gravitymod.core.ObfName;

public class EntityPlayerSPTransformer implements IExplicitTransformer, Opcodes {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";

    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        final String onUpdateWalkingPlayer = ObfName.get("onUpdateWalkingPlayer", "func_175161_p");
        final String pushOutOfBlocks = ObfName.get("pushOutOfBlocks", "func_145771_j");
        final String onLivingUpdate = ObfName.get("onLivingUpdate", "func_70636_d");
        final String updateAutoJump = ObfName.get("updateAutoJump", "func_189810_i");

        for (var method : classNode.methods) {
            InsnList list = method.instructions;
            if (method.name.equals(onUpdateWalkingPlayer)) {
                // Replace all axisalignedbb.minY to this.posY
                String minY = ObfName.get("minY", "field_72338_b");
                String posY = ObfName.get("posY", "field_70163_u");
                for (var node : list) {
                    if (node.getOpcode() == GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(minY)
                    ) {
                        if (node.getPrevious() instanceof VarInsnNode vin
                            && vin.getOpcode() == ALOAD
                        ) {
                            vin.var = 0;
                            fin.name = posY;
                            fin.desc = "D";
                            fin.owner = "net/minecraft/client/entity/EntityPlayerSP";
                        }
                    }
                }
            } else if (method.name.equals(pushOutOfBlocks)) {
                /*
                        } else {
                          -BlockPos blockpos = new BlockPos(x, y, z);
                          +BlockPos blockpos = Hooks.makeRelativeBlockPos(new BlockPos(x, y, z), this);
                           double d0 = x - blockpos.getX();
                 */
                for (var node : list) {
                    if (node.getOpcode() == INVOKESPECIAL
                        && node instanceof MethodInsnNode min
                        && min.owner.equals("net/minecraft/util/math/BlockPos")
                    ) {
                        list.insert(
                            min,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "makeRelativeBlockPos",
                                "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                false
                            )
                        );
                        list.insert(min, new VarInsnNode(ALOAD, 0));
                        break;
                    }
                }
            } else if (method.name.equals(onLivingUpdate)) {
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
                VarInsnNode store = null;
                LabelNode postEventBlock = null;
                LabelNode getFoodStatsBlock = null;
                LabelNode temp = null;
                String getFoodStats = ObfName.get("getFoodStats", "func_71024_bL");
                for (var node : list) {
                    if (node instanceof LabelNode labelNode) {
                        temp = labelNode;
                    }
                    if (store == null && node.getOpcode() == ASTORE && node instanceof VarInsnNode vin) {
                        store = vin;
                    }
                    if (node.getOpcode() == NEW
                        && node instanceof TypeInsnNode tin
                        && tin.desc.equals("net/minecraftforge/client/event/PlayerSPPushOutOfBlocksEvent")
                    ) {
                        postEventBlock = temp;
                    }
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(getFoodStats)
                    ) {
                        getFoodStatsBlock = temp;
                        break;
                    }
                }
                if (store == null || postEventBlock == null || getFoodStatsBlock == null) {
                    throw new RuntimeException("Transform failed in " + this.getClass().getName() + "'s onLivingUpdate");
                }
                InsnList tempList = new InsnList();
                tempList.add(new VarInsnNode(ALOAD, store.var));
                tempList.add(
                    new TypeInsnNode(
                        INSTANCEOF,
                        "uk/co/mysterymayhem/gravitymod/common/util/boundingboxes/GravityAxisAlignedBB"
                    )
                );
                tempList.add(new JumpInsnNode(IFEQ, postEventBlock));
                tempList.add(new VarInsnNode(ALOAD, 0));
                tempList.add(new VarInsnNode(ALOAD, store.var));
                tempList.add(
                    new MethodInsnNode(
                        INVOKESTATIC,
                        HOOKS,
                        "pushEntityPlayerSPOutOfBlocks",
                        "(Luk/co/mysterymayhem/gravitymod/asm/EntityPlayerWithGravity;Lnet/minecraft/util/math/AxisAlignedBB;)V",
                        false
                    )
                );
                tempList.add(new JumpInsnNode(GOTO, getFoodStatsBlock));

                list.insert(store, tempList);
            } else if (method.name.equals("isHeadspaceFree")) {
                /*
                     private boolean isHeadspaceFree(BlockPos pos, int height) {
                       +pos = Hooks.makeRelativeBlockPos(pos, this);
                        for (int y = 0; y < height; y++) {
                 */
                for (var node : list) {
                    if (node.getOpcode() == ICONST_0) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 1));
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "makeRelativeBlockPos",
                                "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                false
                            )
                        );
                        list.insertBefore(node, new VarInsnNode(ASTORE, 1));
                        break;
                    }
                }
            } else if (method.name.equals(updateAutoJump)) {
                var node = list.getFirst();
                while (node != null) {
                    if (node.getOpcode() == NEW
                        && node instanceof TypeInsnNode tin
                        && tin.desc.equals("net/minecraft/util/math/Vec3d")
                    ) {
                        for (int j = 10; j > 0; j--) {
                            node = node.getNext();
                            list.remove(node.getPrevious());
                        }
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                String posX = ObfName.get("posX", "field_70165_t");
                while (node != null) {
                    if (node.getOpcode() == GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(posX)
                    ) {
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                String getEntityBoundingBox = ObfName.get("getEntityBoundingBox", "func_174813_aQ");
                while (node != null) {
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(getEntityBoundingBox)
                    ) {
                        list.remove(node.getNext());
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                String posZ = ObfName.get("posZ", "field_70161_v");
                while (node != null) {
                    if (node.getOpcode() == GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(posZ)
                    ) {
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                    if (node.getOpcode() == INVOKESPECIAL) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "adjustVec",
                                "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                int count = 0;
                String rotationYaw = ObfName.get("rotationYaw", "field_70177_z");
                while (node != null) {
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
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        if (++count >= 2) {
                            break;
                        }
                    } else {
                        node = node.getNext();
                    }
                }
                String getForward = ObfName.get("getForward", "func_189651_aD");
                while (node != null) {
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(getForward)
                    ) {
                        list.insert(
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
                while (node != null) {
                    if (node.getOpcode() == ASTORE && node instanceof VarInsnNode vin && vin.var == 13) {
                        while (node.getPrevious().getOpcode() != NEW) {
                            list.remove(node.getPrevious());
                        }
                        list.remove(node.getPrevious());
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getBlockPosAtTopOfPlayer",
                                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ASTORE, 10));
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "adjustVec",
                                "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        list.insert(node, new VarInsnNode(ALOAD, 10));
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                node = commonGetRelativeUpPatch(node, list);
                while (node != null) {
                    if (node.getOpcode() == NEW
                        && node instanceof TypeInsnNode tin
                        && tin.desc.equals("net/minecraft/util/math/AxisAlignedBB")
                    ) {
                        node = node.getNext().getNext();
                        list.remove(node.getPrevious());
                        list.remove(node.getPrevious());
                        while (node.getNext().getOpcode() != INVOKEVIRTUAL) {
                            node = node.getNext();
                        }
                        list.remove(node.getNext());
                        list.remove(node.getNext());
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "constructNewGAABBFrom2Vec3d",
                                "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/AxisAlignedBB;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "addAdjustedVector",
                                "(Lnet/minecraft/util/math/Vec3d;DDDLnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                count = 0;
                String add = ObfName.get("add", "func_72441_c");
                while (node != null) {
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(add)
                    ) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "addAdjustedVector",
                                "(Lnet/minecraft/util/math/Vec3d;DDDLnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                false
                            )
                        );
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        if (++count == 2) {
                            break;
                        }
                    } else {
                        node = node.getNext();
                    }
                }
                while (node != null) {
                    if (node.getOpcode() == DCONST_1 && node instanceof InsnNode) {
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        list.insertBefore(node, new InsnNode(DCONST_0));
                        while (node.getOpcode() != INVOKESPECIAL) {
                            node = node.getNext();
                        }
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "addAdjustedVector",
                                "(Lnet/minecraft/util/math/Vec3d;DDDLnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/Vec3d;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        list.insert(node, new InsnNode(DCONST_0));
                        list.insert(node, new InsnNode(DCONST_1));
                        list.insert(node, new InsnNode(DCONST_0));
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                String maxY = ObfName.get("maxY", "field_72337_e");
                while (node != null) {
                    if (node.getOpcode() == GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(maxY)
                    ) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                String up = ObfName.get("up", "func_177981_b");
                while (node != null) {
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(up)
                    ) {
                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "getRelativeUpBlockPos",
                                "(Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        node = node.getNext();
                        list.remove(node.getPrevious());
                        break;
                    } else {
                        node = node.getNext();
                    }
                }
                while (node != null) {
                    if (node.getOpcode() == GETFIELD
                        && node instanceof FieldInsnNode fin
                        && fin.name.equals(maxY)
                    ) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                String getY = ObfName.get("getY", "func_177956_o");
                while (node != null) {
                    if (node.getOpcode() == INVOKEVIRTUAL
                        && node instanceof MethodInsnNode min
                        && min.name.equals(getY)
                    ) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
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
                /*
                var sv = new BasicVerifier();
                Analyzer<BasicValue> analyzer = new Analyzer<>(sv);
                try {
                    analyzer.analyze(classNode.name, method);
                } catch (AnalyzerException e) {
                    InsnPrinter.printMethod(method);
                    FMLLoadingPlugin.logger.info("=========================================================");
                    FMLLoadingPlugin.logger.info(InsnPrinter.prettyPrint(e.node.getPrevious()));
                    FMLLoadingPlugin.logger.info(InsnPrinter.prettyPrint(e.node));
                    FMLLoadingPlugin.logger.info(InsnPrinter.prettyPrint(e.node.getNext()));
                    throw new RuntimeException(e);
                }*/
                break;
            }
        }
        
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
    
    private AbstractInsnNode commonGetRelativeUpPatch(AbstractInsnNode node, InsnList list) {
        String up = ObfName.get("up", "func_177984_a");
        while (node != null) {
            if (node.getOpcode() == INVOKEVIRTUAL
                && node instanceof MethodInsnNode min
                && min.name.equals(up)
            ) {
                list.insert(
                    node,
                    new MethodInsnNode(
                        INVOKESTATIC,
                        HOOKS,
                        "getRelativeUpBlockPos",
                        "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;",
                        false
                    )
                );
                list.insert(node, new VarInsnNode(ALOAD, 0));
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
        String minY = ObfName.get("minY", "field_72338_b");
        while (node != null) {
            if (node.getOpcode() == GETFIELD
                && node instanceof FieldInsnNode fin
                && fin.name.equals(minY)
            ) {
                list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                list.insertBefore(
                    node,
                    new MethodInsnNode(
                        INVOKESTATIC,
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
