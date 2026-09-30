package derp.scalingbreak;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scalingbreak implements ModInitializer {
	public static final String MOD_ID = "scalebreak";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("ScaleBreak Loaded");
	}
}
