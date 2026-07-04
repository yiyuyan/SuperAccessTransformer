package cn.ksmcbrigade.sat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public class ModuleUtils {

    private static final MethodHandles.Lookup IMPL_LOOKUP =
            AccessUnsafeUtils.getFieldValue(MethodHandles.Lookup.class, "IMPL_LOOKUP", MethodHandles.Lookup.class);

    private static MethodHandle implAddOpens;
    private static MethodHandle implAddReads;
    private static MethodHandle implAddExports;

    static {
        try {
            implAddOpens = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpens",
                    MethodType.methodType(void.class, String.class, Module.class));
            implAddReads = IMPL_LOOKUP.findVirtual(Module.class, "implAddReads",
                    MethodType.methodType(void.class, Module.class));
            implAddExports = IMPL_LOOKUP.findVirtual(Module.class, "implAddExports",
                    MethodType.methodType(void.class, String.class, Module.class));
        } catch (Throwable e) {
            System.err.println("[ModuleUtils] Failed to initialize method handles: " + e.getMessage());
        }
    }

    public static void openAllModulesToModule(Module targetModule) {
        ModuleLayer.boot().modules().forEach(module -> {
            if (module != targetModule) {
                addReads(module, targetModule);

                module.getDescriptor().packages().forEach(pkg -> {
                    addOpens(module, pkg, targetModule);
                    addExports(module, pkg, targetModule);
                });
            }
        });
    }

    public static void openModuleToAllModules(Module sourceModule) {
        ModuleLayer.boot().modules().forEach(module -> {
            if (module != sourceModule) {
                addReads(sourceModule, module);

                module.getDescriptor().packages().forEach(pkg -> {
                    addOpens(sourceModule, pkg, module);
                    addExports(sourceModule, pkg, module);
                });
            }
        });
    }

    public static void openAllModules() {
        Set<Module> allModules = ModuleLayer.boot().modules();

        for (Module sourceModule : allModules) {
            for (Module targetModule : allModules) {
                if (sourceModule != targetModule) {
                    addReads(sourceModule, targetModule);

                    sourceModule.getDescriptor().packages().forEach(pkg -> {
                        addOpens(sourceModule, pkg, targetModule);
                        addExports(sourceModule, pkg, targetModule);
                    });
                }
            }
        }
    }

    public static void openPackage(String sourceModuleName, String packageName, String targetModuleName) {
        ModuleLayer.boot().findModule(sourceModuleName).ifPresent(sourceModule -> {
            ModuleLayer.boot().findModule(targetModuleName).ifPresent(targetModule -> {
                addOpens(sourceModule, packageName, targetModule);
                addExports(sourceModule, packageName, targetModule);
            });
        });
    }

    public static void addReads(Module sourceModule, Module targetModule) {
        try {
            if (implAddReads != null) {
                implAddReads.invoke(sourceModule, targetModule);
            } else {
                addReadsFallback(sourceModule, targetModule);
            }
        } catch (Throwable e) {
            try {
                addReadsFallback(sourceModule, targetModule);
            } catch (Throwable ignored) {

            }
        }
    }

    public static void addOpens(Module sourceModule, String packageName, Module targetModule) {
        try {
            if (implAddOpens != null) {
                implAddOpens.invoke(sourceModule, packageName, targetModule);
            } else {
                addOpensFallback(sourceModule, packageName, targetModule);
            }
        } catch (Throwable e) {
            try {
                addOpensFallback(sourceModule, packageName, targetModule);
            } catch (Throwable ignored) {

            }
        }
    }

    public static void addExports(Module sourceModule, String packageName, Module targetModule) {
        try {
            if (implAddExports != null) {
                implAddExports.invoke(sourceModule, packageName, targetModule);
            } else {
                addExportsFallback(sourceModule, packageName, targetModule);
            }
        } catch (Throwable e) {
            try {
                addExportsFallback(sourceModule, packageName, targetModule);
            } catch (Throwable ignored) {

            }
        }
    }

    private static void addReadsFallback(Module sourceModule, Module targetModule) {
        try {
            Map<String, Set<Module>> extraReads = AccessUnsafeUtils.getFieldValue(
                    sourceModule, "extraReads", Map.class);
            if (extraReads == null) {
                extraReads = new HashMap<>();
                AccessUnsafeUtils.setFieldValue(sourceModule, "extraReads", extraReads);
            }
            extraReads.computeIfAbsent(targetModule.getName(), k -> new HashSet<>()).add(targetModule);
        } catch (Exception ignored) {

        }
    }


    private static void addOpensFallback(Module sourceModule, String packageName, Module targetModule) {
        try {
            Map<String, Set<Module>> openPackages = AccessUnsafeUtils.getFieldValue(
                    sourceModule, "openPackages", Map.class);
            if (openPackages == null) {
                openPackages = new HashMap<>();
                AccessUnsafeUtils.setFieldValue(sourceModule, "openPackages", openPackages);
            }
            openPackages.computeIfAbsent(packageName, k -> new HashSet<>()).add(targetModule);
        } catch (Exception ignored) {

        }
    }

    private static void addExportsFallback(Module sourceModule, String packageName, Module targetModule) {
        try {
            Map<String, Set<Module>> extraExports = AccessUnsafeUtils.getFieldValue(
                    sourceModule, "extraExports", Map.class);
            if (extraExports == null) {
                extraExports = new HashMap<>();
                AccessUnsafeUtils.setFieldValue(sourceModule, "extraExports", extraExports);
            }
            extraExports.computeIfAbsent(packageName, k -> new HashSet<>()).add(targetModule);
        } catch (Exception ignored) {}
    }
}