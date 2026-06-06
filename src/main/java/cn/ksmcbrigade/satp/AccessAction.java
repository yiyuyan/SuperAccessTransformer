package cn.ksmcbrigade.satp;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public abstract class AccessAction implements TransformAction<AccessAction.Params> {

    public interface Params extends TransformParameters {
        @Input
        ListProperty<String> getTargetPackages();
    }

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File inputJar = getInputArtifact().get().getAsFile();
        List<String> packages = getParameters().getTargetPackages().get();

        boolean needsTransform = false;
        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".class") && isTargetPackage(name, packages)) {
                    needsTransform = true;
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan " + inputJar, e);
        }

        if (!needsTransform) {
            outputs.file(inputJar);
            return;
        }

        String outputName = inputJar.getName().replace(".jar", "-public.jar");
        File outputFile = outputs.file(outputName);

        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile))) {

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String name = entry.getName();
                byte[] data = jis.readAllBytes();

                if (name.endsWith(".class") && isTargetPackage(name, packages)) {
                    try {
                        data = transformClass(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                jos.putNextEntry(new JarEntry(name));
                jos.write(data);
                jos.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to transform " + inputJar, e);
        }
    }

    private boolean isTargetPackage(String className, List<String> packages) {
        for (String pkg : packages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private byte[] transformClass(byte[] original) {
        ClassReader cr = new ClassReader(original);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) return original;

        boolean changed = false;

        if ((cn.access & Opcodes.ACC_PRIVATE) != 0) {
            cn.access = (cn.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
            changed = true;
        } else if ((cn.access & Opcodes.ACC_PROTECTED) != 0) {
            cn.access = (cn.access & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
            changed = true;
        } else if ((cn.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            cn.access |= Opcodes.ACC_PUBLIC;
            changed = true;
        }
        if ((cn.access & Opcodes.ACC_FINAL) != 0) {
            cn.access &= ~Opcodes.ACC_FINAL;
            changed = true;
        }

        for (FieldNode fn : cn.fields) {
            if ("$VALUES".equals(fn.name)) continue;
            boolean fChanged = false;
            if ((fn.access & Opcodes.ACC_PRIVATE) != 0) {
                fn.access = (fn.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
                fChanged = true;
            } else if ((fn.access & Opcodes.ACC_PROTECTED) != 0) {
                fn.access = (fn.access & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                fChanged = true;
            } else if ((fn.access & (Opcodes.ACC_PUBLIC)) == 0) {
                fn.access |= Opcodes.ACC_PUBLIC;
                fChanged = true;
            }
            if ((fn.access & Opcodes.ACC_FINAL) != 0) {
                fn.access &= ~Opcodes.ACC_FINAL;
                fChanged = true;
            }
            if (fChanged) changed = true;
        }

        for (MethodNode mn : cn.methods) {
            if ("<clinit>".equals(mn.name) || mn.name.startsWith("handler$")) continue;
            boolean mChanged = false;
            if ((mn.access & Opcodes.ACC_PRIVATE) != 0) {
                mn.access = (mn.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
                mChanged = true;
            } else if ((mn.access & Opcodes.ACC_PROTECTED) != 0) {
                mn.access = (mn.access & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                mChanged = true;
            } else if ((mn.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
                mn.access |= Opcodes.ACC_PUBLIC;
                mChanged = true;
            }
            if ((mn.access & Opcodes.ACC_FINAL) != 0) {
                mn.access &= ~Opcodes.ACC_FINAL;
                mChanged = true;
            }
            if (mChanged) changed = true;
        }

        if (!changed) return original;

        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }
}