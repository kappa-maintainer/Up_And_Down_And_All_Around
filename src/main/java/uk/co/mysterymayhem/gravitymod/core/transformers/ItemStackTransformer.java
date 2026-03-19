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

public class ItemStackTransformer implements IExplicitTransformer, Opcodes {
    private static final String HOOKS = "uk/co/mysterymayhem/gravitymod/asm/Hooks";
    @Override
    public byte[] transform(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        String onItemUse = ObfName.get("onItemUse", "func_179546_a");
        String useItemRightClick = ObfName.get("useItemRightClick", "func_77957_a");
        String onPlayerStoppedUsing = ObfName.get("onPlayerStoppedUsing", "func_77974_b");
        
        for (var method : classNode.methods) {
            InsnList list = method.instructions;
            if (method.name.equals(onItemUse)) {
                String Item_onItemUse = ObfName.get("onItemUse", "func_180614_a");
                for (var node : list) {
                    if (node.getOpcode() == INVOKEVIRTUAL && node instanceof MethodInsnNode min && min.name.equals(Item_onItemUse)) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(node, new VarInsnNode(ALOAD, 1));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "onItemUsePre",
                                "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Luk/co/mysterymayhem/gravitymod/asm/util/ItemStackAndBoolean;"
                            )
                        );
                        list.insertBefore(node, new VarInsnNode(ASTORE, 10));

                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "onItemUsePost",
                                "(Luk/co/mysterymayhem/gravitymod/asm/util/ItemStackAndBoolean;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 1));
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        list.insert(node, new VarInsnNode(ALOAD, 10));
                        break;
                    }
                }
            } else if (method.name.equals(useItemRightClick)) {
                String onItemRightClick = ObfName.get("onItemRightClick", "func_77659_a");
                for (var node : list) {
                    if (node.getOpcode() == INVOKEVIRTUAL && node instanceof MethodInsnNode min && min.name.equals(onItemRightClick)) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(node, new VarInsnNode(ALOAD, 2));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "onItemRightClickPre",
                                "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Luk/co/mysterymayhem/gravitymod/asm/util/ItemStackAndBoolean;",
                                false
                            )
                        );
                        list.insertBefore(node, new VarInsnNode(ASTORE, 4));

                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "onItemRightClickPost",
                                "(Luk/co/mysterymayhem/gravitymod/asm/util/ItemStackAndBoolean;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 2));
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        list.insert(node, new VarInsnNode(ALOAD, 4));
                        break;
                    }
                }
            } else if (method.name.equals(onPlayerStoppedUsing)) {
                String Item_onPlayerStoppedUsing = ObfName.get("onPlayerStoppedUsing", "func_77615_a");
                for (var node : list) {
                    if (node.getOpcode() == INVOKEVIRTUAL && node instanceof MethodInsnNode min && min.name.equals(Item_onPlayerStoppedUsing)) {
                        list.insertBefore(node, new VarInsnNode(ALOAD, 0));
                        list.insertBefore(node, new VarInsnNode(ALOAD, 2));
                        list.insertBefore(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "onPlayerStoppedUsingPre",
                                "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Luk/co/mysterymayhem/gravitymod/asm/util/ItemStackAndBoolean;",
                                false
                            )
                        );
                        list.insertBefore(node, new VarInsnNode(ASTORE, 4));

                        list.insert(
                            node,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                HOOKS,
                                "onPlayerStoppedUsingPost",
                                "(Luk/co/mysterymayhem/gravitymod/asm/util/ItemStackAndBoolean;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V",
                                false
                            )
                        );
                        list.insert(node, new VarInsnNode(ALOAD, 2));
                        list.insert(node, new VarInsnNode(ALOAD, 0));
                        list.insert(node, new VarInsnNode(ALOAD, 4));
                        break;
                    }
                    
                }
            }
        }
        
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
