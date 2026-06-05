package cn.ksmcbrigade.sat;


import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;

public class SuperAccessTransformerService implements IModFileCandidateLocator {

    static {
        try {
            AccessAgent.attachSelf(!FMLLoader.isProduction(),false);
        } catch (Throwable e) {
            AccessAgent.attachSelf(false,false);
        }
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {}

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
