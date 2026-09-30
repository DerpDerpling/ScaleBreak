package derp.scalingbreak.client;

import net.fabricmc.api.ClientModInitializer;

public class ScalingbreakClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScaleBreakConfig.load();
        ScaleBreakCommands.register();
    }
}
