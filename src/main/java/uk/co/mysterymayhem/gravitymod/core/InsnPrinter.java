package uk.co.mysterymayhem.gravitymod.core;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.PrintWriter;
import java.io.StringWriter;

public class InsnPrinter {
    private static final Printer printer = new Textifier();
    private static final TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);

    public static String prettyPrint(AbstractInsnNode insnNode) {
        if (insnNode == null) return "null";
        insnNode.accept(methodPrinter);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString().trim();
    }
    
    public static void printMethod(MethodNode methodNode) {
        methodNode.instructions.forEach(n -> FMLLoadingPlugin.logger.info(prettyPrint(n)));
    }
}
