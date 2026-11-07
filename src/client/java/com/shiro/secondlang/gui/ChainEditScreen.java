package com.shiro.secondlang.gui;

import com.shiro.secondlang.SecondLangConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** Ordered fallback chain for one language; the top row has the highest priority. */
public class ChainEditScreen extends Screen {
	private static final Component ADD_LANGUAGE = Component.translatable("gui.secondlang.add_language");
	private static final Component MOVE_UP = Component.translatable("gui.secondlang.move_up");
	private static final Component MOVE_DOWN = Component.translatable("gui.secondlang.move_down");
	private static final Component REMOVE = Component.translatable("gui.secondlang.remove");
	private static final int HEADER_HEIGHT = 46;
	private static final int FOOTER_HEIGHT = 40;
	private static final int ROW_HEIGHT = 24;
	private static final int BUTTON_SIZE = 20;
	private static final String RUBI_SUFFIX = "_rubi";

	private final Screen parent;
	private final String code;
	private int scroll;

	public ChainEditScreen(final Screen parent, final String code) {
		super(Component.translatable("gui.secondlang.chain_title", Component.literal(code)));
		this.parent = parent;
		this.code = code;
	}

	private List<String> chain() {
		return SecondLangConfig.chainFor(this.code);
	}

	/**
	 * The language ruppi derives from a {@code xxx_rubi} selection, or {@code null} when ruppi is
	 * absent or the edited language is not a ruppi variant. Read-only: ruppi's own config is
	 * never inspected, the rule is the one ruppi documents.
	 */
	private @Nullable String ruppiBase() {
		if (this.code.length() <= RUBI_SUFFIX.length() || !this.code.endsWith(RUBI_SUFFIX)) {
			return null;
		}
		if (!FabricLoader.getInstance().isModLoaded("ruppi")) {
			return null;
		}
		return this.code.substring(0, this.code.length() - RUBI_SUFFIX.length());
	}

	/** Visual row of the read-only ruppi note; meaningful only while {@link #ruppiBase()} is set. */
	private int noteRow() {
		List<String> chain = this.chain();
		return chain.isEmpty() ? 1 : chain.size();
	}

	private int totalRows() {
		int rows = this.chain().isEmpty() ? 1 : this.chain().size();
		return this.ruppiBase() == null ? rows : rows + 1;
	}

	private int rowsVisible() {
		return Math.max(1, (this.height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
	}

	private int rowY(final int visualRow) {
		return HEADER_HEIGHT + visualRow * ROW_HEIGHT;
	}

	@Override
	protected void init() {
		List<String> chain = this.chain();
		this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, this.totalRows() - this.rowsVisible()));
		int buttonX = this.width / 2 + 60;
		for (int row = 0; row < this.rowsVisible(); row++) {
			int index = this.scroll + row;
			if (index >= chain.size()) {
				break;
			}
			int y = this.rowY(row);
			int current = index;
			this.addRenderableWidget(Button.builder(MOVE_UP, button -> this.move(current, -1)).size(BUTTON_SIZE, BUTTON_SIZE).pos(buttonX, y).build());
			this.addRenderableWidget(Button.builder(MOVE_DOWN, button -> this.move(current, 1)).size(BUTTON_SIZE, BUTTON_SIZE).pos(buttonX + BUTTON_SIZE + 4, y).build());
			this.addRenderableWidget(Button.builder(REMOVE, button -> this.remove(current)).size(BUTTON_SIZE, BUTTON_SIZE).pos(buttonX + 2 * (BUTTON_SIZE + 4), y).build());
		}

		int footerY = this.height - 32;
		this.addRenderableWidget(Button.builder(ADD_LANGUAGE, button -> this.minecraft.gui.setScreen(new LanguagePickerScreen(this, this.code))).width(150).pos(this.width / 2 - 154, footerY).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.secondlang.back"), button -> this.onClose()).width(150).pos(this.width / 2 + 4, footerY).build());
	}

	private void move(final int index, final int delta) {
		List<String> chain = new ArrayList<>(this.chain());
		int target = index + delta;
		if (index < 0 || index >= chain.size() || target < 0 || target >= chain.size()) {
			return;
		}
		Collections.swap(chain, index, target);
		SecondLangConfig.setChain(this.code, chain);
		this.rebuildWidgets();
	}

	private void remove(final int index) {
		List<String> chain = new ArrayList<>(this.chain());
		if (index < 0 || index >= chain.size()) {
			return;
		}
		chain.remove(index);
		SecondLangConfig.setChain(this.code, chain);
		this.rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
		int maxScroll = Math.max(0, this.totalRows() - this.rowsVisible());
		int next = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, maxScroll);
		if (next != this.scroll) {
			this.scroll = next;
			this.rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		Font font = this.font;
		graphics.centeredText(font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("gui.secondlang.chain_hint"), this.width / 2, 26, 0xFF9E9E9E);

		List<String> chain = this.chain();
		String ruppiBase = this.ruppiBase();
		if (chain.isEmpty()) {
			graphics.centeredText(font, Component.translatable("gui.secondlang.empty_chain"), this.width / 2, this.rowY(0) + 2, 0xFF9E9E9E);
		}
		int textX = this.width / 2 - 160;
		for (int row = 0; row < this.rowsVisible(); row++) {
			int index = this.scroll + row;
			if (index >= totalRows()) {
				break;
			}
			if (index < chain.size()) {
				String entryCode = chain.get(index);
				LanguageInfo info = this.minecraft.getLanguageManager().getLanguage(entryCode);
				Component label = info == null ? Component.literal(entryCode) : Component.literal(entryCode + "   ").append(info.toComponent());
				graphics.text(font, label, textX, this.rowY(row) + 6, 0xFFFFFFFF);
			} else if (ruppiBase != null && index == this.noteRow()) {
				graphics.text(font, Component.translatable("gui.secondlang.ruppi_note", ruppiBase), textX, this.rowY(row) + 6, 0xFF9E9E9E);
			}
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
