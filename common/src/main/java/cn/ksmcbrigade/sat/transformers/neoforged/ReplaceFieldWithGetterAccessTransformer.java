package cn.ksmcbrigade.sat.transformers.neoforged;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ReplaceFieldWithGetterAccessTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (!className.equals("net/neoforged/neoforge/coremods/ReplaceFieldWithGetterAccess")) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            for (MethodNode method : classNode.methods) {
                if (method.name.equals("transform") &&
                        method.desc.equals("(Lorg/objectweb/asm/tree/ClassNode;Lnet/neoforged/neoforgespi/transformation/SimpleTransformationContext;)V")) {

                    int patchedCalls = 0;

                    AbstractInsnNode current = method.instructions.getFirst();
                    while (current != null) {
                        AbstractInsnNode next = current.getNext();
                        if (current.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode minsn = (MethodInsnNode) current;
                            if (minsn.owner.equals("net/neoforged/neoforge/coremods/ReplaceFieldWithGetterAccess") &&
                                    minsn.name.equals("redirectFieldToMethod")) {

                                LabelNode tryStart = new LabelNode();
                                LabelNode tryEnd = new LabelNode();
                                LabelNode catchStart = new LabelNode();
                                LabelNode afterCatch = new LabelNode();

                                InsnList wrapper = new InsnList();
                                wrapper.add(tryStart);
                                wrapper.add(current.clone(null));
                                wrapper.add(tryEnd);
                                wrapper.add(new JumpInsnNode(Opcodes.GOTO, afterCatch));

                                wrapper.add(catchStart);
                                wrapper.add(new InsnNode(Opcodes.POP));
                                wrapper.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                                wrapper.add(afterCatch);

                                method.instructions.insert(current, wrapper);
                                method.instructions.remove(current);
                                method.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchStart, "java/lang/RuntimeException"));

                                patchedCalls++;
                                current = next;
                                continue;
                            }
                        }
                        current = next;
                    }

                    break;
                }
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw);

            return cw.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}