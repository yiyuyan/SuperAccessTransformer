package cn.ksmcbrigade.testsat.neoforge;

import cn.ksmcbrigade.testsat.Testsat;
import net.neoforged.neoforge.common.util.Lazy;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@Mod(Testsat.MOD_ID)
public final class TestsatNeoForge {
    public TestsatNeoForge() {
        // Run our common setup.
        Minecraft.getInstance().timer = new DeltaTracker.Timer(200F,0L,(x)->Minecraft.getInstance().getTickTargetMillis(x)/1.2F);
        System.out.println("nOW TIMER: "+Minecraft.getInstance().timer.msPerTick);
        Lazy<String> l = new Lazy<>(() -> "hi");

        System.out.println("lazy: "+l.get());
        System.out.println("network: "+ NetworkRegistry.ATTRIBUTE_FLOW.name());
        Testsat.init();
    }
}
