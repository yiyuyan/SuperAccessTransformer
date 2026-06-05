package cn.ksmcbrigade.sat;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.*;

public class AccessAgent {

    public static void attachSelf(boolean dev,boolean fabricDev){
        if(!dev){
            AccessUnsafeUtils.loadAgent(AccessUnsafeUtils.getJarPath(AccessAgent.class));
        }
        else{
            File file = new File(System.getProperty("user.dir")).getParentFile();
            if(fabricDev) file = file.getParentFile();
            AccessUnsafeUtils.loadAgent(file.toPath().resolve("build/libs/SuperAccessTransformer-1.0.jar").toFile().getAbsolutePath());
        }
    }

    public static void premain(String arg, Instrumentation instrumentation) {
        instrumentation.addTransformer(new AccessTransformer(),true);
    }

    public static void agentmain(String arg,Instrumentation instrumentation){
        premain(arg,instrumentation);
    }
}
