package com.shiro.secondlang;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Builds the language stack handed to {@code ClientLanguage.loadFrom}. */
public final class LanguageStacks {
	private LanguageStacks() {
	}

	/**
	 * {@code ["en_us"] + reversedFallbacks + entries the caller already had + [selected]}.
	 *
	 * <p>Entries of {@code original} that are not {@code en_us}, not the selected language and
	 * not part of the configured chain are kept in their original order, so a language another
	 * mod inserted into the stack (for example ruppi's {@code xxx_rubi} base language) survives
	 * no matter which of the two wrappers runs first. Everything is de-duplicated in order and
	 * every language no resource pack provides is dropped, except {@code en_us} and the selected
	 * language.
	 */
	public static List<String> build(ResourceManager resourceManager, List<String> original, String selected) {
		String selectedCode = selected == null || selected.isEmpty() ? SecondLangConfig.DEFAULT_LANGUAGE : selected;
		List<String> fallbacks = SecondLangConfig.chainFor(selectedCode);

		List<String> requested = new ArrayList<>();
		requested.add(SecondLangConfig.DEFAULT_LANGUAGE);
		for (int i = fallbacks.size() - 1; i >= 0; i--) {
			requested.add(fallbacks.get(i));
		}
		for (String code : original) {
			if (code == null || code.isEmpty()) {
				continue;
			}
			if (SecondLangConfig.DEFAULT_LANGUAGE.equals(code) || selectedCode.equals(code) || fallbacks.contains(code)) {
				continue;
			}
			requested.add(code);
		}
		requested.add(selectedCode);

		Set<String> stack = new LinkedHashSet<>();
		for (String code : requested) {
			boolean isDefault = SecondLangConfig.DEFAULT_LANGUAGE.equals(code);
			boolean isSelected = selectedCode.equals(code);
			if (!isDefault && !isSelected && !hasTranslations(resourceManager, code)) {
				SecondLangConfig.LOGGER.warn("[secondlang] skipping '{}': no resource pack provides its translations", code);
				continue;
			}
			stack.add(code);
		}
		if (stack.isEmpty()) {
			stack.add(SecondLangConfig.DEFAULT_LANGUAGE);
		}
		return new ArrayList<>(stack);
	}

	/** Whether any namespace of {@code resourceManager} ships {@code lang/<code>.json}. */
	public static boolean hasTranslations(ResourceManager resourceManager, String code) {
		String path = String.format(Locale.ROOT, "lang/%s.json", code);
		try {
			for (String namespace : resourceManager.getNamespaces()) {
				Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
				if (!resourceManager.getResourceStack(location).isEmpty()) {
					return true;
				}
			}
		} catch (RuntimeException e) {
			SecondLangConfig.LOGGER.warn("[secondlang] could not check translations for '{}'", code, e);
		}
		return false;
	}
}
