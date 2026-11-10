package com.paranoidhighlighter.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class ModConfigScreen extends Screen {
	private final Screen parent;
	private TrackedItemsList list;
	private Component status = Component.empty();

	public ModConfigScreen(Screen parent) {
		super(Component.translatable("text.paranoidhighlighter.title"));
		this.parent = parent;
	}

	public static Screen create(Screen parent) {
		return new ModConfigScreen(parent);
	}

	@Override
	protected void init() {
		int topControlsY = 28;
		int buttonHeight = 20;
		int gap = 4;
		int left = 8;

		addRenderableWidget(CycleButton.onOffBuilder(ModConfig.enabled)
				.create(left, topControlsY, 110, buttonHeight,
						Component.translatable("text.paranoidhighlighter.option.enabled"),
						(btn, value) -> ModConfig.enabled = value));

		addRenderableWidget(CycleButton.onOffBuilder(ModConfig.glowDroppedItems)
				.create(left + 114, topControlsY, 120, buttonHeight,
						Component.translatable("text.paranoidhighlighter.option.glow_dropped_short"),
						(btn, value) -> ModConfig.glowDroppedItems = value));

		addRenderableWidget(CycleButton.onOffBuilder(ModConfig.glowItemFrames)
				.create(left + 238, topControlsY, 110, buttonHeight,
						Component.translatable("text.paranoidhighlighter.option.glow_frames_short"),
						(btn, value) -> ModConfig.glowItemFrames = value));

		addRenderableWidget(CycleButton.onOffBuilder(ModConfig.glowArmorStands)
				.create(left + 352, topControlsY, 120, buttonHeight,
						Component.translatable("text.paranoidhighlighter.option.glow_stands_short"),
						(btn, value) -> ModConfig.glowArmorStands = value));

		int sliderY = topControlsY + buttonHeight + gap;
		addRenderableWidget(new RangeSlider(left, sliderY, 220, buttonHeight));
		addRenderableWidget(new MaxEntitiesSlider(left + 228, sliderY, 220, buttonHeight));

		int extraY = sliderY + buttonHeight + gap;
		int extraWidth = 74;
		addRenderableWidget(onOff(left, extraY, extraWidth, "text.paranoidhighlighter.option.mobs",
				ModConfig.glowMobs, value -> ModConfig.glowMobs = value));
		addRenderableWidget(onOff(left + 78, extraY, extraWidth, "text.paranoidhighlighter.option.players",
				ModConfig.glowPlayers, value -> ModConfig.glowPlayers = value));
		addRenderableWidget(onOff(left + 156, extraY, extraWidth, "text.paranoidhighlighter.option.containers",
				ModConfig.glowContainers, value -> ModConfig.glowContainers = value));
		addRenderableWidget(onOff(left + 234, extraY, extraWidth, "text.paranoidhighlighter.option.vehicles",
				ModConfig.glowVehicles, value -> ModConfig.glowVehicles = value));
		addRenderableWidget(onOff(left + 312, extraY, extraWidth, "text.paranoidhighlighter.option.displays",
				ModConfig.glowItemDisplays, value -> ModConfig.glowItemDisplays = value));
		addRenderableWidget(onOff(left + 390, extraY, extraWidth, "text.paranoidhighlighter.option.alerts",
				ModConfig.alerts, value -> ModConfig.alerts = value));

		int listTop = extraY + buttonHeight + 8;
		int listBottomMargin = 56;
		int listHeight = Math.max(40, this.height - listTop - listBottomMargin);
		this.list = new TrackedItemsList(this.minecraft, this.width, listHeight, listTop, 28);
		this.list.refresh();
		addRenderableWidget(this.list);

		int bottomY = this.height - 28;
		int half = (this.width - 24) / 3;

		Button addHeld = Button.builder(Component.translatable("text.paranoidhighlighter.button.add_held"), b -> addHeldItem())
				.bounds(8, bottomY, half, buttonHeight)
				.tooltip(Tooltip.create(Component.translatable("text.paranoidhighlighter.button.add_held.tooltip")))
				.build();
		addRenderableWidget(addHeld);

		Button browse = Button.builder(Component.translatable("text.paranoidhighlighter.button.browse"), b ->
						this.minecraft.setScreen(new ItemPickerScreen(this)))
				.bounds(12 + half, bottomY, half, buttonHeight)
				.tooltip(Tooltip.create(Component.translatable("text.paranoidhighlighter.button.browse.tooltip")))
				.build();
		addRenderableWidget(browse);

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(16 + half * 2, bottomY, half, buttonHeight)
				.build());
	}

	private CycleButton<Boolean> onOff(int x, int y, int width, String key, boolean initial,
			java.util.function.Consumer<Boolean> apply) {
		return CycleButton.onOffBuilder(initial)
				.create(x, y, width, 20, Component.translatable(key), (btn, value) -> apply.accept(value));
	}

	private void addHeldItem() {
		Minecraft client = this.minecraft;
		if (client.player == null) {
			this.status = Component.translatable("text.paranoidhighlighter.status.need_world");
			return;
		}
		ItemStack held = client.player.getMainHandItem();
		if (held.isEmpty()) {
			this.status = Component.translatable("text.paranoidhighlighter.status.empty_hand");
			return;
		}
		if (ModConfig.addItem(held, GlowColor.AQUA)) {
			this.status = Component.translatable("text.paranoidhighlighter.status.added", held.getHoverName());
			this.list.refresh();
		} else {
			this.status = Component.translatable("text.paranoidhighlighter.status.already_added", held.getHoverName());
		}
	}

	void onItemPicked(ItemStack stack) {
		if (ModConfig.addItem(stack, GlowColor.AQUA)) {
			this.status = Component.translatable("text.paranoidhighlighter.status.added", stack.getHoverName());
		} else {
			this.status = Component.translatable("text.paranoidhighlighter.status.already_added", stack.getHoverName());
		}
		this.list.refresh();
	}

	void refreshList() {
		if (this.list != null) {
			this.list.refresh();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
		if (this.list != null) {
			graphics.drawString(this.font, Component.translatable("text.paranoidhighlighter.category.items"),
					12, this.list.getY() - 12, 0xA0A0A0);
		}
		if (!this.status.getString().isEmpty()) {
			graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height - 42, 0xFFFFA0);
		}
	}

	@Override
	public void onClose() {
		ModConfig.save();
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void removed() {
		ModConfig.save();
	}

	private static class RangeSlider extends AbstractSliderButton {
		private RangeSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), (ModConfig.glowRange - 1.0) / 255.0);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			int range = Mth.clamp((int) Math.round(1.0 + this.value * 255.0), 1, 256);
			setMessage(Component.translatable("text.paranoidhighlighter.option.glow_range_value", range));
		}

		@Override
		protected void applyValue() {
			ModConfig.glowRange = Mth.clamp(1.0 + this.value * 255.0, 1.0, 256.0);
		}
	}

	private static class MaxEntitiesSlider extends AbstractSliderButton {
		private MaxEntitiesSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), (ModConfig.maxGlowingEntities - 1.0) / 1023.0);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			int max = Mth.clamp((int) Math.round(1.0 + this.value * 1023.0), 1, 1024);
			setMessage(Component.translatable("text.paranoidhighlighter.option.max_entities_value", max));
		}

		@Override
		protected void applyValue() {
			ModConfig.maxGlowingEntities = Mth.clamp((int) Math.round(1.0 + this.value * 1023.0), 1, 1024);
		}
	}
}
