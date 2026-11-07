package com.shiro.secondlang.gui;

import com.shiro.secondlang.SecondLangConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Lists every known language together with its configured fallback chain. */
public class FallbackListScreen extends Screen {
	private static final Component EDIT_CHAIN = Component.translatable("gui.secondlang.edit_chain");
	private static final int HEADER_HEIGHT = 40;
	private static final int FOOTER_HEIGHT = 40;
	private static final int ROW_HEIGHT = 20;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);
	private @Nullable LanguageList list;
	private @Nullable Button editButton;

	public FallbackListScreen(final Screen parent) {
		super(Component.translatable("gui.secondlang.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.title, this.font));

		this.list = this.layout.addToContents(new LanguageList(this.minecraft));

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.defaultCellSetting().alignHorizontallyCenter();
		this.editButton = footer.addChild(Button.builder(EDIT_CHAIN, button -> this.editSelected()).width(150).build());
		footer.addChild(Button.builder(Component.translatable("gui.secondlang.done"), button -> this.onClose()).width(150).build());
		this.editButton.active = this.list.getSelected() != null;

		this.layout.visitWidgets(widget -> this.addRenderableWidget(widget));
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.list != null) {
			this.list.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void onClose() {
		if (SecondLangConfig.consumeReloadRequest()) {
			this.minecraft.reloadResourcePacks();
		}
		this.minecraft.gui.setScreen(this.parent);
	}

	private void editSelected() {
		if (this.list == null) {
			return;
		}
		LanguageList.Entry selected = this.list.getSelected();
		if (selected == null) {
			return;
		}
		this.minecraft.gui.setScreen(new ChainEditScreen(this, selected.code));
	}

	private class LanguageList extends ObjectSelectionList<LanguageList.Entry> {
		LanguageList(final Minecraft minecraft) {
			super(minecraft, FallbackListScreen.this.width, FallbackListScreen.this.height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ROW_HEIGHT);
			FallbackListScreen.this.minecraft.getLanguageManager().getLanguages().forEach((code, info) -> this.addEntry(new Entry(code, info)));
		}

		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 110;
		}

		private class Entry extends ObjectSelectionList.Entry<Entry> {
			private final String code;
			private final Component name;

			Entry(final String code, final LanguageInfo info) {
				this.code = code;
				this.name = info.toComponent();
			}

			@Override
			public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
				Font font = FallbackListScreen.this.font;
				int x = this.getContentX();
				int y = this.getContentYMiddle() - font.lineHeight / 2;
				graphics.text(font, this.code, x + 4, y, 0xFFFFFFFF);
				graphics.text(font, this.name, x + 108, y, 0xFF9E9E9E);

				List<String> chain = SecondLangConfig.chainFor(this.code);
				String summary = chain.isEmpty() ? "\u2014" : String.join(" \u2192 ", chain);
				graphics.text(font, summary, this.getContentRight() - 4 - font.width(summary), y, 0xFFE0C060);
			}

			@Override
			public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
				LanguageList.this.setSelected(this);
				if (FallbackListScreen.this.editButton != null) {
					FallbackListScreen.this.editButton.active = true;
				}
				if (doubleClick) {
					FallbackListScreen.this.editSelected();
				}
				return super.mouseClicked(event, doubleClick);
			}

			@Override
			public Component getNarration() {
				return Component.literal(this.code);
			}
		}
	}
}
