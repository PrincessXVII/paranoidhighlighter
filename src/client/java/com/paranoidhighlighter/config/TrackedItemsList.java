package com.paranoidhighlighter.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class TrackedItemsList extends ContainerObjectSelectionList<TrackedItemsList.Entry> {
	public TrackedItemsList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
		super(minecraft, width, height, y, itemHeight);
	}

	public void refresh() {
		clearEntries();
		for (TrackedItem item : ModConfig.trackedItems) {
			addEntry(new Entry(item));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(560, this.width - 20);
	}

	public class Entry extends ContainerObjectSelectionList.Entry<Entry> {
		private final TrackedItem tracked;
		private final CycleButton<Boolean> enabledButton;
		private final CycleButton<GlowColor> colorButton;
		private final CycleButton<Boolean> matchButton;
		private final Button removeButton;
		private final ItemStack icon;

		private Entry(TrackedItem tracked) {
			this.tracked = tracked;
			this.icon = tracked.toStack().orElse(ItemStack.EMPTY);

			this.enabledButton = CycleButton.onOffBuilder(tracked.enabled)
					.displayOnlyValue()
					.create(0, 0, 50, 20, Component.translatable("text.paranoidhighlighter.option.entry_enabled"),
							(btn, value) -> {
								tracked.enabled = value;
								ModConfig.save();
							});

			this.colorButton = CycleButton.builder(color -> Component.literal(color.name()), tracked.color)
					.withValues(GlowColor.values())
					.displayOnlyValue()
					.create(0, 0, 100, 20, Component.translatable("text.paranoidhighlighter.option.entry_color"),
							(btn, value) -> {
								tracked.color = value;
								ModConfig.save();
							});

			this.matchButton = CycleButton.booleanBuilder(
							Component.translatable("text.paranoidhighlighter.option.match_exact"),
							Component.translatable("text.paranoidhighlighter.option.match_type"),
							tracked.matchComponents)
					.displayOnlyValue()
					.create(0, 0, 64, 20, Component.empty(), (btn, value) -> {
						tracked.matchComponents = value;
						ModConfig.save();
					});

			this.removeButton = Button.builder(Component.translatable("text.paranoidhighlighter.button.remove"), b -> {
						ModConfig.trackedItems.remove(tracked);
						ModConfig.save();
						TrackedItemsList.this.refresh();
					})
					.size(70, 20)
					.build();
		}

		@Override
		public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int x = getContentX();
			int y = getContentY();

			if (!this.icon.isEmpty()) {
				graphics.renderItem(this.icon, x + 2, y + 2);
			}

			Component name = this.icon.isEmpty()
					? Component.literal(this.tracked.itemId)
					: this.icon.getHoverName();
			graphics.drawString(TrackedItemsList.this.minecraft.font, name, x + 24, y + 2, 0xFFFFFF);
			graphics.drawString(TrackedItemsList.this.minecraft.font, this.tracked.itemId, x + 24, y + 12, 0x808080);

			int right = getContentRight();
			this.removeButton.setX(right - 74);
			this.removeButton.setY(y);
			this.matchButton.setX(right - 142);
			this.matchButton.setY(y);
			this.colorButton.setX(right - 246);
			this.colorButton.setY(y);
			this.enabledButton.setX(right - 300);
			this.enabledButton.setY(y);

			this.enabledButton.render(graphics, mouseX, mouseY, partialTick);
			this.colorButton.render(graphics, mouseX, mouseY, partialTick);
			this.matchButton.render(graphics, mouseX, mouseY, partialTick);
			this.removeButton.render(graphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.enabledButton, this.colorButton, this.matchButton, this.removeButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.enabledButton, this.colorButton, this.matchButton, this.removeButton);
		}
	}
}
