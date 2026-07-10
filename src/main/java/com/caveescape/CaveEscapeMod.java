package com.caveescape;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaveEscapeMod implements ModInitializer {
    public static final String MOD_ID = "caveescape";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Cave Escape Challenge initializing...");

        // Handle first spawn and respawn: build the sealed cave and give gear.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            CaveManager.setupPlayer(newPlayer);
        });

        // Detect the initial join via the tick handler tracking, and check escape.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CaveManager.tick(server);
        });

        LOGGER.info("Cave Escape Challenge ready. Good luck escaping!");
    }
}