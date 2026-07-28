package cn.ksmcbrigade.sat.transformers;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import cn.ksmcbrigade.sat.AccessAgent;
import org.objectweb.asm.*;

public class ReflectionTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (!"jdk/internal/reflect/Reflection".equals(className)) {
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

                if (name.equals("verifyModuleAccess") &&
                        descriptor.equals("(Ljava/lang/Module;Ljava/lang/Class;)Z")) {
                    return new ReturnConstantMethodVisitor(mv, true);
                }

                if (name.equals("isTrustedFinalField") &&
                        descriptor.equals("(Ljava/lang/reflect/Field;)Z")) {
                    return new ReturnConstantMethodVisitor(mv, false);
                }

                if (name.equals("filterFields") &&
                        descriptor.equals("(Ljava/lang/Class;[Ljava/lang/reflect/Field;)[Ljava/lang/reflect/Field;")) {
                    return new NoFilterMethodVisitor(mv);
                }

                if (name.equals("filterMethods") &&
                        descriptor.equals("(Ljava/lang/Class;[Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;")) {
                    return new NoFilterMethodVisitor(mv);
                }

                return mv;
            }
        };
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    private static class ReturnConstantMethodVisitor extends MethodVisitor {
        private final boolean value;

        ReturnConstantMethodVisitor(MethodVisitor mv, boolean value) {
            super(AccessAgent.getASMAPIVersion(), mv);
            this.value = value;
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            mv.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            mv.visitInsn(Opcodes.IRETURN);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {}

        @Override
        public void visitEnd() {
            mv.visitEnd();
        }
    }

    private static class NoFilterMethodVisitor extends MethodVisitor {
        NoFilterMethodVisitor(MethodVisitor mv) {
            super(AccessAgent.getASMAPIVersion(), mv);
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ARETURN);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {}

        @Override
        public void visitEnd() {
            mv.visitEnd();
        }
    }
}