package com.paranoidhighlighter.client;

import com.paranoidhighlighter.config.ModConfig;
import com.paranoidhighlighter.glow.ContainerOutlineRenderer;
import com.paranoidhighlighter.glow.GlowTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParanoidHighlighterClient implements ClientModInitializer {
	public static final String MOD_ID = "paranoidhighlighter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ModKeybinds.register();
		ClientTickEvents.END_CLIENT_TICK.register(GlowTracker::tick);
		WorldRenderEvents.AFTER_ENTITIES.register(ContainerOutlineRenderer::render);
		LOGGER.info("Paranoid Highlighter loaded");
	}
}
