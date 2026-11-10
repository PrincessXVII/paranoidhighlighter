package com.paranoidhighlighter.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.paranoidhighlighter.config.GlowColor;
import com.paranoidhighlighter.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class ModKeybinds {
	public static final KeyMapping TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.paranoidhighlighter.toggle",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_Y,
			KeyMapping.Category.MISC));
	public static final KeyMapping ADD_HELD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.paranoidhighlighter.add_held",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			KeyMapping.Category.MISC));

	private ModKeybinds() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (TOGGLE.consumeClick()) {
				ModConfig.enabled = !ModConfig.enabled;
				ModConfig.save();
				if (client.player != null) {
					client.player.displayClientMessage(Component.translatable(ModConfig.enabled
							? "text.paranoidhighlighter.status.toggled_on"
							: "text.paranoidhighlighter.status.toggled_off"), true);
				}
			}
			while (ADD_HELD.consumeClick()) {
				if (client.player == null || client.screen != null) {
					continue;
				}
				ItemStack held = client.player.getMainHandItem();
				if (held.isEmpty()) {
					client.player.displayClientMessage(
							Component.translatable("text.paranoidhighlighter.status.empty_hand"), true);
					continue;
				}
				boolean added = ModConfig.addItem(held, GlowColor.AQUA);
				client.player.displayClientMessage(Component.translatable(added
						? "text.paranoidhighlighter.status.added"
						: "text.paranoidhighlighter.status.already_added", held.getHoverName()), true);
			}
		});
	}
}
