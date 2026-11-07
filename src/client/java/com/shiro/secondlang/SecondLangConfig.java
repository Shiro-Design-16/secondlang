package com.shiro.secondlang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores {@code config/secondlang.json}: {@code chains[selectedLanguageCode] = fallback codes},
 * ordered from highest priority to lowest.
 */
public final class SecondLangConfig {
	public static final String MOD_ID = "secondlang";
	public static final String DEFAULT_LANGUAGE = "en_us";
	public static final Logger LOGGER = LoggerFactory.getLogger("secondlang");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String EXAMPLE_CODE = "ja_jp_rubi";
	private static final String EXAMPLE_FALLBACK = "ja_jp";

	private static Map<String, List<String>> chains = new LinkedHashMap<>();
	private static boolean loaded;
	private static boolean reloadRequested;

	private SecondLangConfig() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("secondlang.json");
	}

	/** Loads the configuration once; safe to call from any client thread. */
	public static synchronized void load() {
		loaded = true;
		chains = new LinkedHashMap<>();
		Path path = path();
		if (!Files.isRegularFile(path)) {
			Map<String, List<String>> example = new LinkedHashMap<>();
			example.put(EXAMPLE_CODE, new ArrayList<>(List.of(EXAMPLE_FALLBACK)));
			chains = example;
			save();
			LOGGER.info("[secondlang] created example configuration at {}", path);
			return;
		}
		try {
			String json = Files.readString(path, StandardCharsets.UTF_8);
			JsonElement root = JsonParser.parseString(json);
			if (!root.isJsonObject()) {
				throw new JsonSyntaxException("root element is not an object");
			}
			JsonElement chainsElement = root.getAsJsonObject().get("chains");
			if (chainsElement != null && chainsElement.isJsonObject()) {
				for (Map.Entry<String, JsonElement> entry : chainsElement.getAsJsonObject().entrySet()) {
					if (!entry.getValue().isJsonArray()) {
						LOGGER.warn("[secondlang] ignoring chain for '{}': not a list", entry.getKey());
						continue;
					}
					List<String> codes = new ArrayList<>();
					for (JsonElement element : entry.getValue().getAsJsonArray()) {
						if (!element.isJsonPrimitive()) {
							continue;
						}
						String code = element.getAsString();
						if (!code.isEmpty() && !codes.contains(code)) {
							codes.add(code);
						}
					}
					normalize(entry.getKey(), codes);
					if (!codes.isEmpty()) {
						chains.put(entry.getKey(), codes);
					}
				}
			}
			LOGGER.info("[secondlang] loaded {} fallback chain(s) from {}", chains.size(), path);
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("[secondlang] could not read {} - using an empty configuration", path, e);
			chains = new LinkedHashMap<>();
		}
	}

	private static synchronized void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	/** Fallback codes for {@code code}, highest priority first, never containing {@code code} itself. */
	public static synchronized List<String> chainFor(String code) {
		ensureLoaded();
		List<String> chain = chains.get(code);
		return chain == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(chain));
	}

	public static synchronized Map<String, List<String>> chains() {
		ensureLoaded();
		Map<String, List<String>> copy = new LinkedHashMap<>();
		chains.forEach((code, chain) -> copy.put(code, List.copyOf(chain)));
		return copy;
	}

	/** Replaces the chain for {@code code}; an empty chain removes the entry. */
	public static synchronized void setChain(String code, List<String> chain) {
		ensureLoaded();
		List<String> codes = new ArrayList<>(chain);
		normalize(code, codes);
		if (codes.isEmpty()) {
			chains.remove(code);
		} else {
			chains.put(code, codes);
		}
		reloadRequested = true;
		save();
	}

	/** Drops entries that can never contribute: the language itself and its own default. */
	private static void normalize(String code, List<String> codes) {
		codes.removeIf(candidate -> candidate.equals(code) || candidate.equals(DEFAULT_LANGUAGE));
	}

	public static synchronized boolean consumeReloadRequest() {
		boolean requested = reloadRequested;
		reloadRequested = false;
		return requested;
	}

	private static void save() {
		JsonObject root = new JsonObject();
		JsonObject chainObject = new JsonObject();
		chains.forEach((code, chain) -> {
			JsonArray array = new JsonArray();
			chain.forEach(array::add);
			chainObject.add(code, array);
		});
		root.add("chains", chainObject);
		Path path = path();
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(path, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("[secondlang] could not write {}", path, e);
		}
	}
}
