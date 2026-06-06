package cn.ksmcbrigade.testsat.neoforge;

import cn.ksmcbrigade.testsat.Testsat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.Mod;

@Mod(Testsat.MOD_ID)
public final class TestsatNeoForge {
    public TestsatNeoForge() {
        // Run our common setup.
        Minecraft.getInstance().timer = new DeltaTracker.Timer(200F,0L,(x)->Minecraft.getInstance().getTickTargetMillis(x)/1.2F);
        System.out.println("nOW TIMER: "+Minecraft.getInstance().timer.msPerTick);
        Testsat.init();
    }
}
