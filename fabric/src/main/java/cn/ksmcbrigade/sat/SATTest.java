package cn.ksmcbrigade.sat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public class SATTest implements ModInitializer {
    @Override
    public void onInitialize() {
        if(FabricLoader.getInstance().isDevelopmentEnvironment()){
            Minecraft.getInstance().timer = new DeltaTracker.Timer(200.0F, 0L, x -> Minecraft.getInstance().getTickTargetMillis(x));
            System.out.println("Now timer msPerTick: "+Minecraft.getInstance().timer.msPerTick);
        }
    }
}
