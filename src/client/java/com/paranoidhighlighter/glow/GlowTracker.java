package com.paranoidhighlighter.glow;

import com.paranoidhighlighter.config.GlowColor;
import com.paranoidhighlighter.config.ModConfig;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GlowTracker {
	private static final Int2IntOpenHashMap GLOWING = new Int2IntOpenHashMap();
	private static final IntOpenHashSet SEEN_ENTITIES = new IntOpenHashSet();
	private static final Set<BlockPos> SEEN_BLOCKS = new HashSet<>();
	private static List<BlockHighlight> BLOCKS = List.of();
	private static boolean primed;

	private GlowTracker() {
	}

	public static void tick(Minecraft client) {
		GLOWING.clear();
		BLOCKS = List.of();
		if (!ModConfig.enabled || client.level == null || client.player == null || ModConfig.trackedItems.isEmpty()) {
			SEEN_ENTITIES.clear();
			SEEN_BLOCKS.clear();
			primed = false;
			return;
		}

		LocalPlayer player = client.player;
		double range = Math.max(1.0, ModConfig.glowRange);
		AABB box = player.getBoundingBox().inflate(range);
		List<Candidate> candidates = new ArrayList<>();
		Component alertName = null;
		int fresh = 0;

		for (Entity entity : client.level.getEntities(player, box, GlowMatcher::isTrackable)) {
			double distSq = player.distanceToSqr(entity);
			if (distSq > range * range) {
				continue;
			}
			var color = GlowMatcher.match(entity);
			if (color.isEmpty()) {
				continue;
			}
			candidates.add(new Candidate(entity.getId(), distSq, color.get().rgb()));
			if (primed && !SEEN_ENTITIES.contains(entity.getId())) {
				fresh++;
				if (alertName == null) {
					alertName = nameOf(entity);
				}
			}
		}

		candidates.sort(Comparator.comparingDouble(Candidate::distSq));
		int limit = Math.max(1, ModConfig.maxGlowingEntities);
		int count = Math.min(limit, candidates.size());
		IntOpenHashSet nowEntities = new IntOpenHashSet(count);
		for (int i = 0; i < count; i++) {
			Candidate candidate = candidates.get(i);
			GLOWING.put(candidate.entityId, candidate.color);
			nowEntities.add(candidate.entityId);
		}

		List<BlockHighlight> blocks = ModConfig.glowContainers
				? findContainers(client, player, range, limit)
				: List.of();
		Set<BlockPos> nowBlocks = new HashSet<>();
		for (BlockHighlight highlight : blocks) {
			nowBlocks.add(highlight.pos());
			if (primed && !SEEN_BLOCKS.contains(highlight.pos())) {
				fresh++;
				if (alertName == null) {
					alertName = highlight.name();
				}
			}
		}

		if (primed && fresh > 0 && ModConfig.alerts) {
			Component message = fresh == 1 && alertName != null
					? Component.translatable("text.paranoidhighlighter.alert.one", alertName)
					: Component.translatable("text.paranoidhighlighter.alert.many", fresh);
			player.displayClientMessage(message, true);
			player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.45F, 1.6F);
		}

		SEEN_ENTITIES.clear();
		SEEN_ENTITIES.addAll(nowEntities);
		SEEN_BLOCKS.clear();
		SEEN_BLOCKS.addAll(nowBlocks);
		BLOCKS = blocks;
		primed = true;
	}

	public static boolean shouldGlow(Entity entity) {
		return GLOWING.containsKey(entity.getId());
	}

	public static int getColor(Entity entity) {
		int id = entity.getId();
		if (!GLOWING.containsKey(id)) {
			return GlowColor.WHITE.rgb();
		}
		return GLOWING.get(id);
	}

	public static List<BlockHighlight> blockHighlights() {
		return BLOCKS;
	}

	private static List<BlockHighlight> findContainers(Minecraft client, LocalPlayer player, double range, int limit) {
		int radius = Math.max(1, (int) Math.ceil(range / 16.0));
		int centerX = player.blockPosition().getX() >> 4;
		int centerZ = player.blockPosition().getZ() >> 4;
		List<BlockHighlight> found = new ArrayList<>();
		double rangeSq = range * range;
		for (int chunkX = centerX - radius; chunkX <= centerX + radius; chunkX++) {
			for (int chunkZ = centerZ - radius; chunkZ <= centerZ + radius; chunkZ++) {
				LevelChunk chunk = client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (!(blockEntity instanceof Container container)) {
						continue;
					}
					BlockPos pos = blockEntity.getBlockPos();
					if (player.distanceToSqr(Vec3.atCenterOf(pos)) > rangeSq) {
						continue;
					}
					GlowMatcher.matchContainer(container).ifPresent(color -> {
						ItemStack sample = firstMatch(container);
						Component name = sample.isEmpty()
								? Component.translatable("text.paranoidhighlighter.alert.container")
								: sample.getHoverName();
						found.add(new BlockHighlight(pos.immutable(), color.rgb(), name));
					});
				}
			}
		}
		found.sort(Comparator.comparingDouble(highlight -> player.distanceToSqr(Vec3.atCenterOf(highlight.pos()))));
		if (found.size() > limit) {
			return List.copyOf(found.subList(0, limit));
		}
		return List.copyOf(found);
	}

	private static ItemStack firstMatch(Container container) {
		int size = container.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = container.getItem(slot);
			if (GlowMatcher.matchStack(stack).isPresent()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static Component nameOf(Entity entity) {
		if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
			return itemEntity.getItem().getHoverName();
		}
		if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame frame && !frame.getItem().isEmpty()) {
			return frame.getItem().getHoverName();
		}
		if (entity instanceof net.minecraft.world.entity.Display.ItemDisplay display && !display.getItemStack().isEmpty()) {
			return display.getItemStack().getHoverName();
		}
		return entity.getName();
	}

	public record BlockHighlight(BlockPos pos, int color, Component name) {
	}

	private record Candidate(int entityId, double distSq, int color) {
	}
}
