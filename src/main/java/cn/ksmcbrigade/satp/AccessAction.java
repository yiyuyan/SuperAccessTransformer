package cn.ksmcbrigade.satp;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public abstract class AccessAction implements TransformAction<AccessAction.Params> {

    public interface Params extends TransformParameters {
        @Input
        ListProperty<String> getTargetPackages();
        @Input
        ListProperty<String> getSkipMethods();
    }

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(@NotNull TransformOutputs outputs) {
        File inputJar = getInputArtifact().get().getAsFile();
        List<String> packages = getParameters().getTargetPackages().get();

        boolean needsTransform = false;
        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class") && isTargetPackage(entry.getName(), packages)) {
                    needsTransform = true;
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan input jar: " + inputJar, e);
        }

        if (!needsTransform) {
            outputs.file(inputJar);
            return;
        }

        File outputFile = outputs.file(inputJar.getName().replace(".jar", "-public.jar"));
        File tempFile = new File(outputFile.getParentFile(), outputFile.getName() + ".tmp");

        try {
            Manifest manifest;
            try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar))) {
                manifest = jis.getManifest();
            }

            try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
                 JarOutputStream jos = manifest != null
                         ? new JarOutputStream(new FileOutputStream(tempFile), manifest)
                         : new JarOutputStream(new FileOutputStream(tempFile))) {

                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith("META-INF/") &&
                            (name.endsWith(".SF") || name.endsWith(".RSA") ||
                                    name.endsWith(".DSA") || name.endsWith(".EC"))) {
                        continue;
                    }

                    byte[] data = jis.readAllBytes();
                    if (name.endsWith(".class") && isTargetPackage(name, packages)) {
                        try {
                            data = transformClass(data, getParameters().getSkipMethods().get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    JarEntry newEntry = new JarEntry(name);
                    newEntry.setTime(entry.getTime());
                    jos.putNextEntry(newEntry);
                    jos.write(data);
                    jos.closeEntry();
                }
                jos.finish();
            }

            Files.move(tempFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile.toPath());
                Files.deleteIfExists(outputFile.toPath());
            } catch (IOException ignored) {}
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

    private byte[] transformClass(byte[] original, List<String> skipRules) {
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
            } else if ((fn.access & Opcodes.ACC_PUBLIC) == 0) {
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
            if ("rotlerp".equals(mn.name) && (cn.name.endsWith("Mob") || cn.name.endsWith("WitherBoss"))) continue;
            if (shouldSkip(skipRules, cn.name, mn.name)) continue;

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

    private boolean shouldSkip(List<String> rules, String simpleClassName, String methodName) {
        for (String rule : rules) {
            if (rule.trim().isEmpty()) continue;
            String[] parts = rule.trim().split(";");
            if (parts.length == 2) {
                if (simpleClassName.endsWith(parts[0]) && parts[1].equals(methodName)) {
                    return true;
                }
            }
        }
        return false;
    }
}