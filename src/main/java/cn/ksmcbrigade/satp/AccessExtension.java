package cn.ksmcbrigade.satp;

import org.gradle.api.provider.ListProperty;

public abstract class AccessExtension {

    public abstract ListProperty<String> getPackages();

    public abstract ListProperty<String> getSkipMethods();

    public AccessExtension() {
        getPackages().add("net/minecraft/");
        getPackages().add("com/mojang/");

        //for fabric
        getSkipMethods().add("WitherBoss;rotlerp");
        getSkipMethods().add("Mob;rotlerp");
    }

    public void packageName(String pkg) {
        if (!pkg.endsWith("/")) pkg += "/";
        getPackages().add(pkg.replace(".","/"));
    }

    public void skip(String rule) {
        getSkipMethods().add(rule);
    }
}