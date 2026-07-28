package cn.ksmcbrigade.sat.transformers;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import cn.ksmcbrigade.sat.AccessAgent;
import org.objectweb.asm.*;

public class MethodHandleFieldAccessorImplTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (!"jdk/internal/reflect/MethodHandleFieldAccessorImpl".equals(className)) {
            return null;
        }

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassVisitor(AccessAgent.getASMAPIVersion(), cw) {
            @Override
            public MethodVisitor visitMethod(int access,
                                             String name,
                                             String descriptor,
                                             String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor,
                        signature, exceptions);
                if (name.equals("isReadOnly") && descriptor.equals("()Z")) {
                    return new MethodVisitor(AccessAgent.getASMAPIVersion(), mv) {
                        @Override
                        public void visitCode() {
                            mv.visitCode();
                            mv.visitInsn(Opcodes.ICONST_0);
                            mv.visitInsn(Opcodes.IRETURN);
                        }

                        @Override
                        public void visitMaxs(int maxStack, int maxLocals) {}

                        @Override
                        public void visitEnd() {
                            mv.visitEnd();
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
}