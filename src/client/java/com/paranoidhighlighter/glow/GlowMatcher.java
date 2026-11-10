package com.paranoidhighlighter.glow;

import com.paranoidhighlighter.config.GlowColor;
import com.paranoidhighlighter.config.ModConfig;
import com.paranoidhighlighter.config.TrackedItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.IntFunction;

public final class GlowMatcher {
	private GlowMatcher() {
	}

	public static Optional<GlowColor> match(Entity entity) {
		if (!ModConfig.enabled || ModConfig.trackedItems.isEmpty()) {
			return Optional.empty();
		}

		if (entity instanceof ItemEntity itemEntity) {
			if (!ModConfig.glowDroppedItems) {
				return Optional.empty();
			}
			return matchStack(itemEntity.getItem());
		}

		if (entity instanceof ItemFrame itemFrame) {
			if (!ModConfig.glowItemFrames) {
				return Optional.empty();
			}
			return matchStack(itemFrame.getItem());
		}

		if (entity instanceof Display.ItemDisplay display) {
			if (!ModConfig.glowItemDisplays) {
				return Optional.empty();
			}
			return matchStack(display.getItemStack());
		}

		if (entity instanceof ArmorStand armorStand) {
			if (!ModConfig.glowArmorStands) {
				return Optional.empty();
			}
			return matchEquipment(armorStand);
		}

		if (entity instanceof AbstractChestBoat || entity instanceof AbstractMinecartContainer) {
			if (!ModConfig.glowVehicles) {
				return Optional.empty();
			}
			return matchEntityInventory(entity);
		}

		if (entity instanceof Player) {
			if (!ModConfig.glowPlayers) {
				return Optional.empty();
			}
			return matchEquipment((LivingEntity) entity);
		}

		if (entity instanceof LivingEntity living) {
			if (!ModConfig.glowMobs) {
				return Optional.empty();
			}
			return matchEquipment(living);
		}

		return Optional.empty();
	}

	public static Optional<GlowColor> matchContainer(Container container) {
		int size = container.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			Optional<GlowColor> color = matchStack(container.getItem(slot));
			if (color.isPresent()) {
				return color;
			}
		}
		return Optional.empty();
	}

	public static boolean isTrackable(Entity entity) {
		return entity instanceof ItemEntity
				|| entity instanceof ItemFrame
				|| entity instanceof Display.ItemDisplay
				|| entity instanceof ArmorStand
				|| entity instanceof AbstractChestBoat
				|| entity instanceof AbstractMinecartContainer
				|| entity instanceof LivingEntity;
	}

	public static Optional<GlowColor> matchStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return Optional.empty();
		}
		for (TrackedItem tracked : ModConfig.trackedItems) {
			Optional<GlowColor> color = tracked.match(stack);
			if (color.isPresent()) {
				return color;
			}
		}
		return Optional.empty();
	}

	private static Optional<GlowColor> matchEquipment(LivingEntity living) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			Optional<GlowColor> color = matchStack(living.getItemBySlot(slot));
			if (color.isPresent()) {
				return color;
			}
		}
		return Optional.empty();
	}

	private static Optional<GlowColor> matchEntityInventory(Entity entity) {
		if (entity instanceof Container container) {
			return matchContainer(container);
		}
		if (entity instanceof ContainerEntity container) {
			return matchSlots(container.getContainerSize(), container::getItem);
		}
		return Optional.empty();
	}

	private static Optional<GlowColor> matchSlots(int size, IntFunction<ItemStack> getter) {
		for (int slot = 0; slot < size; slot++) {
			Optional<GlowColor> color = matchStack(getter.apply(slot));
			if (color.isPresent()) {
				return color;
			}
		}
		return Optional.empty();
	}
}
