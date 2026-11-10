package com.paranoidhighlighter.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ItemPickerScreen extends Screen {
	private final ModConfigScreen parent;
	private EditBox searchBox;
	private ItemList list;
	private List<ItemStack> allItems = List.of();

	public ItemPickerScreen(ModConfigScreen parent) {
		super(Component.translatable("text.paranoidhighlighter.picker.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.allItems = BuiltInRegistries.ITEM.stream()
				.map(ItemStack::new)
				.sorted(Comparator.comparing(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT)))
				.toList();

		this.searchBox = new EditBox(this.font, this.width / 2 - 160, 28, 320, 20,
				Component.translatable("text.paranoidhighlighter.picker.search"));
		this.searchBox.setHint(Component.translatable("text.paranoidhighlighter.picker.search_hint"));
		this.searchBox.setResponder(query -> this.list.refresh(query));
		addRenderableWidget(this.searchBox);
		setInitialFocus(this.searchBox);

		int listTop = 56;
		int listHeight = Math.max(40, this.height - listTop - 40);
		this.list = new ItemList(this.minecraft, this.width, listHeight, listTop, 24);
		this.list.refresh("");
		addRenderableWidget(this.list);

		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(this.width / 2 - 100, this.height - 28, 200, 20)
				.build());
	}

	private void pick(ItemStack stack) {
		this.parent.onItemPicked(stack.copyWithCount(1));
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private class ItemList extends ObjectSelectionList<ItemList.Entry> {
		private ItemList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}

		private void refresh(String query) {
			clearEntries();
			String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			List<ItemStack> matched = new ArrayList<>();
			for (ItemStack stack : ItemPickerScreen.this.allItems) {
				if (stack.isEmpty()) {
					continue;
				}
				Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
				String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
				String idStr = id.toString().toLowerCase(Locale.ROOT);
				if (q.isEmpty() || name.contains(q) || idStr.contains(q)) {
					matched.add(stack);
				}
			}
			for (ItemStack stack : matched) {
				addEntry(new Entry(stack));
			}
		}

		@Override
		public int getRowWidth() {
			return Math.min(400, this.width - 40);
		}

		private class Entry extends ObjectSelectionList.Entry<Entry> {
			private final ItemStack stack;

			private Entry(ItemStack stack) {
				this.stack = stack;
			}

			@Override
			public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				int x = getContentX();
				int y = getContentY();
				graphics.renderItem(this.stack, x + 2, y + 2);
				graphics.drawString(ItemPickerScreen.this.font, this.stack.getHoverName(), x + 24, y + 2, 0xFFFFFF);
				Identifier id = BuiltInRegistries.ITEM.getKey(this.stack.getItem());
				graphics.drawString(ItemPickerScreen.this.font, id.toString(), x + 24, y + 12, 0x808080);
			}

			@Override
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
				ItemPickerScreen.this.pick(this.stack);
				return true;
			}

			@Override
			public Component getNarration() {
				return this.stack.getHoverName();
			}
		}
	}
}
