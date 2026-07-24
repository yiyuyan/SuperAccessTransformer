package cn.ksmcbrigade.sat;

import cn.ksmcbrigade.sat.transformers.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

public class AccessAgent {

    public static Logger LOGGER = LogUtils.getLogger();

    public static void attachSelf(boolean dev,boolean fabricDev){
        if(!dev){
            AccessUnsafeUtils.loadAgent(AccessUnsafeUtils.getJarPath(AccessAgent.class));
        }
        else{
            File file = new File(System.getProperty("user.dir")).getParentFile();
            if(fabricDev) file = file.getParentFile();
            AccessUnsafeUtils.loadAgent(file.toPath().resolve("build/libs/SuperAccessTransformer-1.1.1.jar").toFile().getAbsolutePath());
        }
    }

    public static void premain(String arg, Instrumentation instrumentation) throws ClassNotFoundException, UnmodifiableClassException {
        instrumentation.addTransformer(new AccessTransformer(),false);

        instrumentation.addTransformer(new ReflectionTransformer(),true);
        instrumentation.addTransformer(new AccessibleObjectTransformer(),true);
        instrumentation.addTransformer(new MethodHandleFieldAccessorImplTransformer(),true);

        instrumentation.retransformClasses(Class.forName("jdk.internal.reflect.Reflection"));
        instrumentation.retransformClasses(Class.forName("java.lang.reflect.AccessibleObject"));
        instrumentation.retransformClasses(Class.forName("jdk.internal.reflect.MethodHandleFieldAccessorImpl"));

        ModuleUtils.openAllModules();

        LOGGER.info("{} Loaded.",AccessAgent.class.getSimpleName());
    }

    public static void agentmain(String arg,Instrumentation instrumentation) throws ClassNotFoundException, UnmodifiableClassException {
        premain(arg,instrumentation);
    }
}
