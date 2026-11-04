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

## Gestures

| Temple gesture | Chat screen | Menu |
| --- | --- | --- |
| Tap | Start speaking / stop and send | Select |
| Double tap | Stop listening or answering; when idle, exit | Back |
| Long press | New chat | – |
| Triple tap | Open menu | – |
| Swipe forward / back | Scroll the answer | Move up / down |

The menu has: **Read answers aloud** (on/off), **Venice web search** (off / auto / on) and the
list of Venice text models available to your key.

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
  data/Settings.kt                Private preferences (key, model, toggles)
  audio/VoiceRecorder.kt          Records questions as AAC .m4a
  audio/Speaker.kt                On-device text-to-speech
  ui/ChatActivity.kt              Main mirrored screen and temple gestures
  ui/ChatViewModel.kt             Listening → transcribing → answering state machine
  ui/ModelPickerActivity.kt       Menu: read aloud, web search, model list
  ui/ConfigActivity.kt            adb-only settings
app/src/test/                     Unit tests for the parser, filter, history and log
```

## Notes

- The display is small (about 640×480 per eye), so the system prompt asks for answers under
  80 words in plain text. Swipe to scroll longer ones.
- Venice parameters sent with every chat: `strip_thinking_response: true`,
  `include_venice_system_prompt` (default true, changeable over adb) and `enable_web_search`
  (menu or adb).
- Release builds never contain a baked-in key; set it over adb after installing.
