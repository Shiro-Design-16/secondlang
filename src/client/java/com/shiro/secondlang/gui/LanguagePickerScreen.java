package com.shiro.secondlang.gui;

import com.shiro.secondlang.LanguageStacks;
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
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Offers the languages that are not part of the chain yet and really ship translations. */
public class LanguagePickerScreen extends Screen {
	private static final int HEADER_HEIGHT = 40;
	private static final int FOOTER_HEIGHT = 40;
	private static final int ROW_HEIGHT = 20;

	private final ChainEditScreen parent;
	private final String code;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);
	private @Nullable PickerList list;

	public LanguagePickerScreen(final ChainEditScreen parent, final String code) {
		super(Component.translatable("gui.secondlang.pick_title", Component.literal(code)));
		this.parent = parent;
		this.code = code;
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.title, this.font));

		this.list = this.layout.addToContents(new PickerList(this.minecraft));
		this.layout.addToFooter(Button.builder(Component.translatable("gui.secondlang.back"), button -> this.onClose()).width(200).build());

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
		this.minecraft.gui.setScreen(this.parent);
	}

	private void select(final String languageCode) {
		List<String> chain = new java.util.ArrayList<>(SecondLangConfig.chainFor(this.code));
		chain.add(languageCode);
		SecondLangConfig.setChain(this.code, chain);
		this.onClose();
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		if (this.list != null && this.list.children().isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("gui.secondlang.picker_empty"), this.width / 2, this.height / 2, 0xFF9E9E9E);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private class PickerList extends ObjectSelectionList<PickerList.Entry> {
		PickerList(final Minecraft minecraft) {
			super(minecraft, LanguagePickerScreen.this.width, LanguagePickerScreen.this.height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ROW_HEIGHT);
			List<String> used = SecondLangConfig.chainFor(LanguagePickerScreen.this.code);
			minecraft.getLanguageManager().getLanguages().forEach((languageCode, info) -> {
				if (languageCode.equals(LanguagePickerScreen.this.code) || languageCode.equals(SecondLangConfig.DEFAULT_LANGUAGE) || used.contains(languageCode)) {
					return;
				}
				if (!LanguageStacks.hasTranslations(minecraft.getResourceManager(), languageCode)) {
					return;
				}
				this.addEntry(new Entry(languageCode, info));
			});
		}

		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 60;
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
				Font font = LanguagePickerScreen.this.font;
				int y = this.getContentYMiddle() - font.lineHeight / 2;
				graphics.text(font, this.code, this.getContentX() + 4, y, 0xFFFFFFFF);
				graphics.text(font, this.name, this.getContentX() + 108, y, 0xFF9E9E9E);
			}

			@Override
			public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
				LanguagePickerScreen.this.select(this.code);
				return true;
			}

			@Override
			public Component getNarration() {
				return Component.literal(this.code);
			}
		}
	}
}
