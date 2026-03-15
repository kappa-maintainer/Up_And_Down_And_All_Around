package uk.co.mysterymayhem.gravitymod.core.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import top.outlands.foundation.IExplicitTransformer;

public class SuperClassReplacingTransformer implements IExplicitTransformer {
    private static final String classToReplace = "net/minecraft/entity/player/EntityPlayer";
    private static final String classReplacement = "uk/co/mysterymayhem/gravitymod/asm/EntityPlayerWithGravity";
    @Override
    public byte[] transform(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        CV cv = new CV(writer);
        reader.accept(cv, 0);
        return writer.toByteArray();
    }

    private static class CV extends ClassVisitor {
        public CV(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
            if (cv != null) {
                cv.visit(version, access, name, signature, classReplacement, interfaces);
            }
        }

        @Override
        public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
            MethodVisitor mv;
            mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
            if (mv != null) {
                mv = new MV(mv);
            }
            return mv;
        }
    }

    private static class MV extends MethodVisitor {
        public MV(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
            if (mv != null) {
                if (owner.equals(classToReplace)) {
                    mv.visitMethodInsn(opcode, classReplacement, name, descriptor, isInterface);
                } else {
                    mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            if (mv != null) {
                if (type.equals(classToReplace)) {
                    mv.visitTypeInsn(opcode, classReplacement);
                } else {
                    mv.visitTypeInsn(opcode, type);
                }
            }
        }

        @Override
        public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
            if (mv != null) {
                if (owner.equals(classToReplace)) {
                    mv.visitFieldInsn(opcode, classReplacement, name, descriptor);
                } else {
                    mv.visitFieldInsn(opcode, owner, name, descriptor);
                }
            }
        }
    }
}
