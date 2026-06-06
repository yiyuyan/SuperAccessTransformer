package cn.ksmcbrigade.satp;

import java.util.ArrayList;
import java.util.List;

public class AccessExtension {
    private List<String> packages = new ArrayList<>();

    public AccessExtension() {
        packages.add("net/minecraft/");
        packages.add("com/mojang/");
    }

    public List<String> getPackages() {
        return packages;
    }

    public void packageName(String pkg) {
        if (!pkg.endsWith("/")) pkg += "/";
        packages.add(pkg);
    }
}
