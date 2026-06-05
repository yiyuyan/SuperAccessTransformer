package cn.ksmcbrigade.sat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class SATMod implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        AccessAgent.attachSelf(FabricLoader.getInstance().isDevelopmentEnvironment(),true);
    }
}
