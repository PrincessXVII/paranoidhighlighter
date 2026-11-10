package com.paranoidhighlighter.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class TrackedItem {
	public boolean enabled = true;
	public GlowColor color = GlowColor.AQUA;
	/** When false, any stack of this item matches, ignoring name, enchantments and other components. */
	public boolean matchComponents = true;
	public String itemId = "minecraft:diamond";
	/** Serialized {@link DataComponentPatch}, may be null/empty for default components. */
	public JsonElement components;
	private transient ItemStack cached;
	private transient boolean cacheReady;

	public TrackedItem() {
	}

	public TrackedItem(ItemStack stack, GlowColor color) {
		this.enabled = true;
		this.color = color;
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		this.itemId = id.toString();
		DataComponentPatch patch = stack.getComponentsPatch();
		if (!patch.isEmpty()) {
			this.components = DataComponentPatch.CODEC
					.encodeStart(JsonOps.INSTANCE, patch)
					.result()
					.orElse(null);
		}
	}

	public Optional<GlowColor> match(ItemStack stack) {
		if (!enabled || stack == null || stack.isEmpty()) {
			return Optional.empty();
		}
		if (!cacheReady) {
			cached = toStack().orElse(ItemStack.EMPTY);
			cacheReady = true;
		}
		if (cached.isEmpty()) {
			return Optional.empty();
		}
		boolean same = matchComponents
				? ItemStack.isSameItemSameComponents(stack, cached)
				: ItemStack.isSameItem(stack, cached);
		return same ? Optional.of(color) : Optional.empty();
	}

	public Optional<ItemStack> toStack() {
		Identifier id = Identifier.tryParse(itemId);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			return Optional.empty();
		}
		Item item = BuiltInRegistries.ITEM.getValue(id);
		ItemStack stack = new ItemStack(item);
		if (components != null && !components.isJsonNull()) {
			DataComponentPatch.CODEC.parse(JsonOps.INSTANCE, components).result().ifPresent(stack::applyComponents);
		}
		return Optional.of(stack);
	}

	public String displayKey() {
		return itemId;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("enabled", enabled);
		obj.addProperty("color", color.name());
		obj.addProperty("matchComponents", matchComponents);
		obj.addProperty("itemId", itemId);
		if (components != null && !components.isJsonNull()) {
			obj.add("components", components);
		}
		return obj;
	}

	public static TrackedItem fromJson(JsonObject obj) {
		TrackedItem item = new TrackedItem();
		if (obj.has("enabled")) {
			item.enabled = obj.get("enabled").getAsBoolean();
		}
		if (obj.has("color")) {
			try {
				item.color = GlowColor.valueOf(obj.get("color").getAsString());
			} catch (IllegalArgumentException ignored) {
				item.color = GlowColor.AQUA;
			}
		}
		if (obj.has("matchComponents")) {
			item.matchComponents = obj.get("matchComponents").getAsBoolean();
		}
		if (obj.has("itemId")) {
			item.itemId = obj.get("itemId").getAsString();
		}
		if (obj.has("components")) {
			item.components = obj.get("components");
		}
		return item;
	}

	public static TrackedItem fromJsonElement(JsonElement element) {
		if (element.isJsonObject()) {
			return fromJson(element.getAsJsonObject());
		}
		return fromJson(JsonParser.parseString(element.getAsString()).getAsJsonObject());
	}
}
