package io.github.pixelatedvolume.inexactcit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the mod.  Holds ID and logger and reports initialization.
 */
public class InexactCITMod implements ClientModInitializer {

    public static final String MOD_ID = "inexactcit";
    public static final Logger LOGGER =
        LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Inexact CIT.");
    }
}
