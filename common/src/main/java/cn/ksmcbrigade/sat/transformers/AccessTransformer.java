package cn.ksmcbrigade.sat.transformers;

import cn.ksmcbrigade.sat.AccessAgent;
import com.google.gson.*;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.util.List;

import static java.lang.reflect.Modifier.*;

public class AccessTransformer implements ClassFileTransformer {
    private static boolean loaded;

    public static List<String> EXCLUDED_PACKAGES;

    public AccessTransformer() {
        AccessAgent.LOGGER.info("Constructing SuperAccessTransformer.");

        File file = new File("config/sat-blacklist.json");
        if(!file.exists()){
            try {
                JsonArray array = new JsonArray();
                array.add("com/sun/jna/");
                FileUtils.writeStringToFile(file,new GsonBuilder().setPrettyPrinting().create().toJson(array), Charset.defaultCharset());
            } catch (IOException e) {
                AccessAgent.LOGGER.warn("Failed to create configs: {}",e.getMessage());
            }
        }
        try {
            JsonArray array = JsonParser.parseString(FileUtils.readFileToString(file,Charset.defaultCharset())).getAsJsonArray();
            EXCLUDED_PACKAGES = array.asList().stream().map(JsonElement::getAsString).toList();
        } catch (IOException e) {
            AccessAgent.LOGGER.error("Failed to parse configs.",e);
            AccessAgent.LOGGER.info("Using default config...");
            EXCLUDED_PACKAGES = List.of("com/sun/jna/");
        }
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass,
                            ProtectionDomain protectionDomain, byte[] bytes) {
        if (!loaded) {
            loaded = true;
            AccessAgent.LOGGER.info("SuperAccessTransformer is running.");
        }

        if (shouldExclude(s)) return bytes;

        try {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            if (isInterface(cn.access)) {
                return bytes;
            }
            for (FieldNode fn : cn.fields) {
                if (fn.name.equals("$VALUES")) continue;

                if (isPrivate(fn.access)) {
                    fn.access &= ~Opcodes.ACC_PRIVATE;
                    fn.access |= Opcodes.ACC_PUBLIC;
                }
                if (isProtected(fn.access)) {
                    fn.access &= ~Opcodes.ACC_PROTECTED;
                    fn.access |= Opcodes.ACC_PUBLIC;
                }
                if (isFinal(fn.access)) {
                    fn.access &= ~Opcodes.ACC_FINAL;
                }
            }

            for (MethodNode mn : cn.methods) {
                if (mn.name.equals("<clinit>")) continue;

                if (isPrivate(mn.access)) {
                    mn.access &= ~Opcodes.ACC_PRIVATE;
                    mn.access |= Opcodes.ACC_PUBLIC;
                }
                if (isProtected(mn.access)) {
                    mn.access &= ~Opcodes.ACC_PROTECTED;
                    mn.access |= Opcodes.ACC_PUBLIC;
                }
                if (isFinal(mn.access)) {
                    mn.access &= ~Opcodes.ACC_FINAL;
                }
            }

            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            return cw.toByteArray();

        } catch (Throwable t) {
            return bytes;
        }
    }

    private boolean shouldExclude(String className) {
        if (className == null) return false;
        for (String prefix : EXCLUDED_PACKAGES) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }
}