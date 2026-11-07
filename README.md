# secondlang

A client-side Minecraft **26.3** (Fabric) mod that gives every language a configurable
**fallback chain**, so a player whose main language is Japanese and whose backup is Chinese
keeps Chinese text instead of dropping straight to English.

## Why this exists

Minecraft only ever loads two languages: `en_us` and the one selected in **Options →
Language**. Every key missing from the selected language therefore falls back to English —
even when a perfectly good translation exists in a third language, or in the base language a
custom variant like `ja_jp_rubi` is derived from. secondlang lets you say "for this
language, try these others first" and merges them below the selected language.

## Installation

* Minecraft 26.3 with Fabric Loader 0.20.1 or newer
* Fabric API
* **Mod Menu is optional** — with it the mod gets a configuration screen, without it you edit
  the JSON file by hand

Drop `secondlang-<version>.jar` into your `mods` folder. Client-side only; nothing is needed
on the server and nothing is sent to it.

## Configuration

The file lives at `<game directory>/config/secondlang.json`:

```json
{
  "chains": {
    "ja_jp_rubi": ["ja_jp"]
  }
}
```

* The key is the language **code** you select in Options → Language (for example
  `ja_jp_rubi`), not its display name.
* The value lists fallback languages, **highest priority first**.
* Do not list the selected language itself and do not list `en_us`: English is always the
  base layer and is added automatically.
* On first launch the file is created with the example above. Deleting it, or leaving
  `chains` empty, restores vanilla behaviour.
* The file is plain JSON and can be edited by hand; restart the game or press **F3+T** to
  reload resource packs afterwards.

## The in-game screen

With Mod Menu installed: **Mod Menu → secondlang → Configure**.

1. The language list shows every language the game knows with its current chain (`—`
   when none). Select a row and choose **Edit chain…**.
2. The chain editor shows the chain, highest priority on top, with **↑ / ↓ / ✕** on every
   row. **Add language…** opens a picker of the remaining languages.
3. Pick a language to append it to the chain; **Back** returns without saving anything else.

Changes take effect immediately: saving a chain reloads resource packs.

## Relationship with ruppi

[ruppi](https://github.com/Shiro-Design-16/ruppi) is a Japanese ruby-text mod. When the selected
language ends in `_rubi`, ruppi inserts the base language (for example `ja_jp`) directly
above English so that resource packs shipping only the base language still apply.

The two mods coexist, and secondlang never removes what another mod put into the stack:

* ruppi's base language always sits **above English**.
* secondlang's chain sits **above ruppi's base language**, so an explicitly configured
  fallback still wins.
* When ruppi is installed and you edit a `_rubi` language, the chain editor shows a
  read-only reminder of the language ruppi adds. It is informational only — ruppi's own
  settings are never read or changed.

## Behaviour details

* A language that no resource pack provides (`lang/<code>.json` is missing everywhere) is
  skipped instead of crashing the game — a typo in the config is harmless.
* `en_us` is always present and always has the lowest priority.
* The selected language is always last, i.e. the highest priority.
* Duplicates are removed while preserving order, so the final stack is stable.
* In 26.3 the language list comes from the `language` section of each pack's `pack.mcmeta`,
  not from scanning `lang/*.json`; a custom code such as `ja_jp_rubi` only appears when some
  installed pack declares it.

## Licence and credits

GPL-3.0; see `LICENSE`. The project skeleton came from ruppi's Fabric template, and the
wrap-operation approach to `ClientLanguage.loadFrom` follows ruppi's implementation.
