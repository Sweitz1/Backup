# VeniceX3: Venice AI on RayNeo X3 Pro glasses

An unofficial Android client that brings [Venice AI](https://venice.ai) chat to RayNeo X3 Pro AR
glasses. Tap the temple, ask out loud, and read the answer (or hear it). Not affiliated with
Venice or RayNeo.

- **Voice in:** tap to record, tap again to send. Speech is transcribed by Venice
  (`/audio/transcriptions`, default `openai/whisper-large-v3`).
- **Streaming answers** from Venice chat (`/chat/completions`), default model
  `venice-uncensored-1-2`, with a glasses-friendly system prompt (short, plain text, no markdown).
- **Both eyes:** built on the RayNeo Mercury SDK (`BaseMirrorActivity`), so every screen renders
  identically in the left and right displays on a pure-black (see-through) background.
- **Temple gestures** for everything; no phone or keyboard needed after setup.
- **Read aloud** (optional) with the glasses' on-device text-to-speech.
- **Private by default:** chat history is kept in memory only and audio clips are deleted after
  transcription. A 24-hour request log stores metadata only (see below).

- **Characters (SillyTavern-style):** chat with a character instead of the plain assistant. A
  built-in companion (Mira) is included. You can import any SillyTavern / TavernAI character card
  (`.png` with the card embedded, or `.json`, spec V1/V2/V3). The card's greeting, personality,
  scenario, example dialogue and post-history instructions are used the way SillyTavern uses them,
  with `{{char}}` / `{{user}}` filled in.
- **Avatar or video:** a card's image, or a muted looping `.mp4`/`.webm`, is shown next to the chat,
  either as a small avatar above the panel (`compact`) or a tall figure beside it (`portrait`).
- **Pictures:** say "show me a picture of…", "draw…" or "send me a selfie" and Venice image
  generation (`/image/generate`) draws it on the display. Selfies use the character's description.
- **Mature mode (18+, off by default):** separate switches for mature **text**, **images** and
  **video**. Nothing changes until you confirm you're 18 or older over adb (see below).

## Gestures

| Temple gesture | Chat screen | Menu |
| --- | --- | --- |
| Tap | Start speaking / stop and send (ask again) | Select |
| Double tap | Cancel listening or answering; close the answer; when idle, exit | Back |
| Long press | New chat (the character greets you again) | – |
| Triple tap | Open menu | – |
| Swipe forward / back | Scroll the answer | Move up / down |

The menu has: **Character** (opens the character list), **Layout** (compact / portrait),
**Mature mode** and its text / images / video switches, **Read answers aloud** (on/off),
**Venice web search** (off / auto / on) and the list of Venice text models available to your key.

## Characters

Copy cards into the app's characters folder. adb can write there, and the character list rescans
it every time it opens:

```bash
DIR=/sdcard/Android/data/dev.sweitz.veniceglasses/files/characters
adb shell mkdir -p $DIR
adb push "Rin.png" $DIR/              # SillyTavern card PNG (also used as the avatar)
adb push "Kai.json" "Kai.webp" $DIR/  # JSON card plus an avatar with the same name
adb push "Kai.mp4" $DIR/              # optional looping video in place of the avatar
adb push "my-art.png" $DIR/companion.png   # avatar (or companion.mp4) for the built-in Mira
```

Then triple tap → **Character**, or set it over adb:

```bash
PKG=dev.sweitz.veniceglasses
adb shell am start -n $PKG/.ui.ConfigActivity --es character "Rin" --es user_name "Sam" --es layout portrait
adb shell am start -n $PKG/.ui.ConfigActivity --ez list_characters true && adb logcat -d -s VeniceX3
# Write the built-in companion out as a V2 card to use as a template
adb shell am start -n $PKG/.ui.ConfigActivity --ez export_template true
```

Cards tagged `NSFW` (or with `extensions.nsfw: true`) are only listed while mature text is on.
Chat history stays in memory only, as before.

## Mature mode (18+)

Off by default. The switches in the menu stay locked until the owner confirms over adb, which
other apps on the glasses can't do:

```bash
# You must be 18 or older. confirm_adult false turns everything back off.
adb shell am start -n $PKG/.ui.ConfigActivity --ez confirm_adult true --ez mature true \
  --ez mature_text true --ez mature_images false --ez mature_video false
```

| Switch | Off | On |
| --- | --- | --- |
| Mature text | Characters are told to keep things safe for work; NSFW cards are hidden | Adult themes between adult characters; NSFW cards are listed |
| Mature images | Venice `safe_mode: true` (adult images are blurred); NSFW card avatars hidden | `safe_mode: false`; NSFW card avatars shown |
| Mature video | NSFW card videos aren't played | NSFW card videos play |

An "18+" badge shows in the chat header while any mature switch is on. Some protections stay on
whatever the switches say. A character whose card mentions minors or an age under 18 always
stays safe for work, and so does any picture request that does. Every mature prompt says all
characters are adults, and image requests always carry a negative prompt against childlike
subjects. The image model is set with `--es image_model <id>` (default `venice-sd35`).

## Build and install

Requirements: Android Studio (Ladybug or newer) or the Android SDK with JDK 17, and the glasses
in developer mode with adb access.

```bash
# Optional: bake your key into debug builds (local.properties is git-ignored)
echo "VENICE_API_KEY=your_venice_inference_key" >> local.properties

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant the microphone up front (permission dialogs are awkward on glasses)
adb shell pm grant dev.sweitz.veniceglasses android.permission.RECORD_AUDIO
```

The RayNeo SDK is included as `app/libs/MercuryAndroidSDK-v0.2.6.aar`.

## Settings over adb

There's no keyboard on the glasses, so settings are set with adb. The settings screen is
protected by a permission only adb's shell has, so other apps on the glasses can't change your key.

```bash
PKG=dev.sweitz.veniceglasses

# Set or change the API key and model
adb shell am start -n $PKG/.ui.ConfigActivity --es api_key "YOUR_KEY" --es model "venice-uncensored-1-2"

# Other options
adb shell am start -n $PKG/.ui.ConfigActivity \
  --es stt_model "openai/whisper-large-v3" \
  --es web_search auto \
  --ez read_aloud true \
  --ez venice_system_prompt false

# Ask a question without speaking (useful for testing)
adb shell am start -n $PKG/.ui.ChatActivity --es ask "What's the capital of Portugal?"

# Check what's set (the key itself is never printed)
adb shell am start -n $PKG/.ui.ConfigActivity && adb logcat -d -s VeniceX3
```

Use a Venice **inference** API key (create one in your Venice account under API). An admin
key isn't needed.

## Request log

Each API call adds a row to a rolling 24-hour CSV with the columns
`date,model,type,request_id,status,latency_ms,cost_usd`. Only metadata is logged, never
prompts, transcripts or answers. `request_id` is Venice's `CF-RAY` header, which Venice support
asks for. `cost_usd` stays empty because the API doesn't return a per-request price.

```bash
# Print it to logcat
adb shell am start -n dev.sweitz.veniceglasses/.ui.ConfigActivity --ez print_log true
adb logcat -d -s VeniceX3

# Or copy the file from a debug build
adb exec-out run-as dev.sweitz.veniceglasses cat files/requests-24h.csv > requests-24h.csv
```

## Project layout

```
app/libs/                         RayNeo Mercury SDK (.aar)
app/src/main/java/dev/sweitz/veniceglasses/
  VeniceGlassesApp.kt             Initializes the RayNeo SDK, settings, API client
  data/VeniceClient.kt            Venice API: streaming chat, speech-to-text, model list
  data/SseParser.kt               Server-sent events parser for streamed replies
  data/ThinkFilter.kt             Strips <think>…</think> reasoning from streamed text
  data/Conversation.kt            In-memory history and the glasses system prompt
  data/RequestLog.kt              Rolling 24 h CSV of request metadata
  data/Settings.kt                Private preferences (key, model, toggles, mature switches)
  data/Character.kt               Character model + built-in assistant and companion
  data/CharacterCard.kt           SillyTavern card reader (JSON V1/V2/V3, PNG tEXt/zTXt/iTXt)
  data/CharacterStore.kt          Loads cards and their avatar/video from the characters folder
  data/PromptBuilder.kt           SillyTavern-style prompt assembly and {{char}}/{{user}} macros
  data/ContentPolicy.kt           Mature-mode gating and the always-on minor safeguards
  data/ImageIntent.kt             Detects "show me / draw / selfie" requests
  audio/VoiceRecorder.kt          Records questions as AAC .m4a
  audio/Speaker.kt                On-device text-to-speech
  ui/ChatActivity.kt              Main mirrored screen and temple gestures
  ui/ChatViewModel.kt             Listening → transcribing → answering state machine
  ui/ModelPickerActivity.kt       Menu: character, layout, mature switches, read aloud, web search, models
  ui/CharacterPickerActivity.kt   Character list
  ui/WaveformView.kt              Microphone level bars while listening
  ui/CreditsActivity.kt           Credits & licences screen (data/Credits.kt)
  ui/ConfigActivity.kt            adb-only settings
app/src/test/                     Unit tests for the parser, filter, history, log, cards, prompts and policy
```

## Credits and licences

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for what VeniceX3 is built with. The RayNeo
SDK `.aar` has no stated licence, which is flagged there. [COMMUNITY.md](COMMUNITY.md) credits the
other RayNeo X3 Pro community projects and says which of them can be reused. The same lists are
in the app (triple tap → Credits & licences).

## Notes

- The display is small (about 640×480 per eye), so the system prompt asks for answers under
  80 words in plain text. Swipe to scroll longer ones.
- Venice parameters sent with every chat: `strip_thinking_response: true`,
  `include_venice_system_prompt` (default true, changeable over adb) and `enable_web_search`
  (menu or adb).
- Release builds never contain a baked-in key; set it over adb after installing.
