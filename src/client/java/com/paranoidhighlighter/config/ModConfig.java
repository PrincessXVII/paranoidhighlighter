package com.paranoidhighlighter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.paranoidhighlighter.client.ParanoidHighlighterClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("paranoidhighlighter.json");

	public static boolean enabled = true;
	public static double glowRange = 64.0;
	public static int maxGlowingEntities = 128;
	public static boolean glowDroppedItems = true;
	public static boolean glowItemFrames = true;
	public static boolean glowArmorStands = true;
	public static boolean glowItemDisplays = true;
	public static boolean glowMobs = true;
	public static boolean glowPlayers = true;
	public static boolean glowVehicles = true;
	public static boolean glowContainers = true;
	public static boolean alerts = true;
	public static final List<TrackedItem> trackedItems = new ArrayList<>();

	private ModConfig() {
	}

	/** @return false if an equivalent stack is already tracked */
	public static boolean addItem(ItemStack stack, GlowColor color) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		TrackedItem candidate = new TrackedItem(stack.copyWithCount(1), color);
		ItemStack candStack = candidate.toStack().orElse(ItemStack.EMPTY);
		for (TrackedItem existing : trackedItems) {
			ItemStack other = existing.toStack().orElse(ItemStack.EMPTY);
			if (!other.isEmpty() && !candStack.isEmpty()
					&& ItemStack.isSameItemSameComponents(candStack, other)) {
				return false;
			}
		}
		trackedItems.add(candidate);
		save();
		return true;
	}

	public static void load() {
		if (!Files.exists(PATH)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			enabled = getBool(root, "enabled", true);
			glowRange = getDouble(root, "glowRange", 64.0);
			maxGlowingEntities = getInt(root, "maxGlowingEntities", 128);
			glowDroppedItems = getBool(root, "glowDroppedItems", true);
			glowItemFrames = getBool(root, "glowItemFrames", true);
			glowArmorStands = getBool(root, "glowArmorStands", true);
			glowItemDisplays = getBool(root, "glowItemDisplays", true);
			glowMobs = getBool(root, "glowMobs", true);
			glowPlayers = getBool(root, "glowPlayers", true);
			glowVehicles = getBool(root, "glowVehicles", true);
			glowContainers = getBool(root, "glowContainers", true);
			alerts = getBool(root, "alerts", true);

			trackedItems.clear();
			if (root.has("trackedItems") && root.get("trackedItems").isJsonArray()) {
				JsonArray array = root.getAsJsonArray("trackedItems");
				array.forEach(el -> {
					if (el.isJsonObject()) {
						trackedItems.add(TrackedItem.fromJson(el.getAsJsonObject()));
					}
				});
			}
		} catch (Exception e) {
			ParanoidHighlighterClient.LOGGER.error("Failed to load config, using defaults", e);
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		root.addProperty("enabled", enabled);
		root.addProperty("glowRange", glowRange);
		root.addProperty("maxGlowingEntities", maxGlowingEntities);
		root.addProperty("glowDroppedItems", glowDroppedItems);
		root.addProperty("glowItemFrames", glowItemFrames);
		root.addProperty("glowArmorStands", glowArmorStands);
		root.addProperty("glowItemDisplays", glowItemDisplays);
		root.addProperty("glowMobs", glowMobs);
		root.addProperty("glowPlayers", glowPlayers);
		root.addProperty("glowVehicles", glowVehicles);
		root.addProperty("glowContainers", glowContainers);
		root.addProperty("alerts", alerts);

		JsonArray array = new JsonArray();
		for (TrackedItem item : trackedItems) {
			array.add(item.toJson());
		}
		root.add("trackedItems", array);

		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			ParanoidHighlighterClient.LOGGER.error("Failed to save config", e);
		}
	}

	private static boolean getBool(JsonObject obj, String key, boolean def) {
		return obj.has(key) ? obj.get(key).getAsBoolean() : def;
	}

	private static double getDouble(JsonObject obj, String key, double def) {
		return obj.has(key) ? obj.get(key).getAsDouble() : def;
	}

	private static int getInt(JsonObject obj, String key, int def) {
		return obj.has(key) ? obj.get(key).getAsInt() : def;
	}
}
