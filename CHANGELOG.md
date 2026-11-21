# Changelog

## [Unreleased]

### Added
- **venicex3-cards library.** The card reader, prompt builder, content policy, picture requests
  and card-store links are now a separate pure-Kotlin module (`cards/`), published to GitHub
  Packages as `dev.sweitz:venicex3-cards` on each release. The app uses it unchanged.

### Fixed
- The Release workflow accepts `V`-prefixed and dotted tags (`V0.2.0`, `V.0.2.0`) as well as `v0.2.0`.

## [0.2.0] - 2026-11-21

### Added
- **Characters (SillyTavern-style).** Import SillyTavern / TavernAI character cards (`.png` with an
  embedded card, or `.json`, spec V1/V2/V3). Greeting, personality, scenario, example dialogue,
  post-history instructions and `{{char}}` / `{{user}}` macros work the way they do in SillyTavern.
- **Built-in companion** character, Mira (an adult), alongside the original assistant.
- **Card stores.** Import from a Chub.ai or Character Tavern card page link, or any direct `.png` /
  `.json` card link (CharaVault, AI Character Cards, GitHub), with `--es import_url`. A Card stores
  screen in the menu lists where to find cards.
- **New chat screen.** Rounded panel with a status pill, character name, listening waveform and
  hints for each step. Compact (avatar above) and portrait (figure beside) layouts. A character's
  image or muted looping video shows next to the chat.
- **Pictures.** "Show me…", "draw…" or "send me a selfie" generates an image with Venice.
- **Mature mode (18+), off by default.** Separate switches for text, images and video, locked
  until the owner confirms they're 18 or older over adb. An "18+" badge shows while it's on.
  Cards or picture requests involving minors always stay safe, and mature cards involving minors
  are refused on import.
- **Credits & licences** screen, [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and
  [COMMUNITY.md](COMMUNITY.md), crediting dependencies and other RayNeo X3 Pro projects.
- Menu entries for character, card stores, layout, mature switches and credits. New adb options:
  `character`, `user_name`, `layout`, `image_model`, `import_card`, `import_url`,
  `list_characters`, `export_template`, `confirm_adult`, `mature`, `mature_text`,
  `mature_images`, `mature_video`.
- README screenshots (layout previews), share images and post text in `docs/`.
- Release workflow: pushing a `v*` tag publishes a GitHub Release with the APKs.

### Changed
- Double tap now also closes an answer before exiting.
- Venice's own system prompt is only used with the plain assistant, not with characters.

### Known issues
- The RayNeo Mercury SDK `.aar` in `app/libs/` has no stated licence (see
  THIRD_PARTY_NOTICES.md).
- The Chub.ai and Character Tavern link formats couldn't be tested before release.

## [0.1.0]

- First version: voice chat with Venice AI on RayNeo X3 Pro glasses, streaming answers,
  temple gestures, read aloud, web search and the model menu.
