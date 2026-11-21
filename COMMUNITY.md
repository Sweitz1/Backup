# RayNeo X3 Pro community and credits

This is a map of what other people have built for the RayNeo X3 Pro, with credit to each project
and the licence GitHub reports for it. **VeniceX3 contains no code from any project listed here.**
The same list is in the app (triple tap → Credits & licences) and in
[`Credits.kt`](app/src/main/java/dev/sweitz/veniceglasses/data/Credits.kt).

Sources: GitHub search (repository metadata only), RayNeo's own posts, reviews and guides, as of
November 2026. The project Discord needs a logged-in account, so it isn't covered.

## How apps get onto the glasses

- **No official third-party store.** RayNeo says the consumer version doesn't support installing
  third-party apps directly, citing "security restrictions and compatibility considerations"
  ([Gizmodo](https://gizmodo.com/rayneo-x3-pro-review-these-ar-glasses-may-haunt-my-dreams-2000699219)).
- **Sideloading with ADB works but is fiddly.** You need ADB mode, and on Windows often a USB
  driver swap with Zadig
  ([Notebookcheck](https://www.notebookcheck.net/Rayneo-X3-Pro-review-Promising-AR-smart-glasses-with-one-big-limitation.1190970.0.html),
  [RayDesk readme](https://github.com/Quad-Labs/RayDesk),
  [macOS guide](https://biggthed.substack.com/p/rayneo-x3pro-sideload-apps-using)). SideQuest helps.
- **Phone apps run poorly** unless they're adapted for the two eye screens
  ([PhoneArena](https://www.phonearena.com/reviews/rayneo-x3-pro-review-a-taste-of-the-ar-future_id7797)).
- **GBox and TapLink.** GBox, which brings in Google Play and Chrome, and the community browser
  TapLink are how most people get apps today
  ([Extentos](https://extentos.com/docs/ecosystem/platforms/rayneo)).
- **Official SDKs:** Android (MercuryAndroidSDK, which VeniceX3 uses) and Unity (ARDK/OpenXR)
  ([Qualcomm guide](https://www.qualcomm.com/developer/project/get-started-with-rayneo-x3-pro-ar-development),
  [RayNeo dev docs](https://rayneo.gitbook.io/rayneo-devdoc/x-xi-lie/android-kai-fa/kuai-su-kai-shi)).
- **RayNeo is courting developers.** Its MIT Reality Hack 2026 post describes an X3 Pro developer
  session and a RayNeo Innovators Program with hardware and technical support
  ([RayNeo](https://www.rayneo.com/blogs/news/rayneo-at-mit-reality-hack-2026-winning-big-with-the-elite-hacker-community)).

## What the community has built

- **[tropicalstream](https://github.com/tropicalstream?tab=repositories)** is by far the most
  prolific developer:
  - native touchpad games: chess, Tetris, rhythm, arcade remixes;
  - **X3Mira**: a phone's screen on the glasses over Wi-Fi;
  - **TapGem / X3Hub / X3Gemini**: Gemini Live voice displays;
  - **TapReader** (ebooks with TTS), **SmartTubular** (YouTube) and **TapMame** (arcade emulator).
- **Browsers:** [TAPLINKX3](https://github.com/informalTechCode/TAPLINKX3) is the most-starred
  X3 project. There's also RayNeoX3Tools, a browser you can drive over ADB.
- **AI agents on the glasses:** OpenClaw, ClawdAR, GlassClaw, two Gemini Live clients and a
  LiveKit voice client. These are the closest relatives of VeniceX3.
- **Platform tools:** [rayneo-x3-spatial-sdk](https://github.com/AlexSKuznetsov/rayneo-x3-spatial-sdk)
  (6DoF, world-locked display), [x3bluetooth](https://github.com/Dun-John/x3bluetooth) (Bluetooth
  pairing), a research repo on adapting apps for both eyes, and a React Native starter.
- **Media:** RayDesk (remote desktop), X3player (local video), an FPV camera stream viewer.
- **Research and demos:** [qdrant-glasses](https://github.com/DenisovAV/qdrant-glasses) (on-device
  object memory), a spatial memory prototype, a knee-rehab demo, a sensor platform and meshtalk
  (a walkie-talkie with no network).
- **RayNeo's developer relations (MaxManausa):** Unity sample projects.

## Licences: what can be reused

Of the 75 projects: **45 have no licence**, 15 are copyleft
(GPL/AGPL), 6 have a custom licence GitHub can't identify, and 9 are
permissive (MIT/Apache-2.0).

- **No licence** means all rights reserved. Their code can't be copied into VeniceX3, even with
  credit. Link to them, or ask the author to add a licence.
- **Copyleft (GPL/AGPL)**: copying code would put VeniceX3 under the same licence.
- **Custom (NOASSERTION)**: read the project's LICENSE file before reusing anything.
- **Permissive (MIT/Apache-2.0)**: reusable. Keep the copyright notice and, for Apache-2.0, any
  NOTICE file, and add the project to [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Good candidates if VeniceX3 ever borrows code: 6DoF head tracking from
`rayneo-x3-spatial-sdk` (Apache-2.0) and Bluetooth pairing from `x3bluetooth` (MIT).

## All projects

| Project | Licence (SPDX) | Reuse | ★ | Description |
| --- | --- | --- | --- | --- |
| [tropicalstream/Breakthrough](https://github.com/tropicalstream/Breakthrough) | none | **None: all rights reserved, link only** | 0 | Dan Troyka's Breakthrough (2000) for the RayNeo X3 Pro AR glasses — race a wall of pieces |
| [tropicalstream/Hammerklavier](https://github.com/tropicalstream/Hammerklavier) | none | **None: all rights reserved, link only** | 0 | Hammerklavier: a sampled grand, upright and Bach-era harpsichord MIDI player for the RayNe |
| [tropicalstream/InnerCosmos](https://github.com/tropicalstream/InnerCosmos) | none | **None: all rights reserved, link only** | 0 | A railed stereoscopic descent through the human body for the RayNeo X3 Pro AR glasses — tw |
| [tropicalstream/MathCosmos](https://github.com/tropicalstream/MathCosmos) | none | **None: all rights reserved, link only** | 0 | A railed stereoscopic tour of mathematics for the RayNeo X3 Pro AR glasses. Six tours, 76 |
| [tropicalstream/RayNeoSpyHunt](https://github.com/tropicalstream/RayNeoSpyHunt) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Spy Hunt XR: a fan recreation of the 1983 arcade game for RayNeo X3 Pro AR glasses — stere |
| [tropicalstream/SmartTubular](https://github.com/tropicalstream/SmartTubular) | MIT | Permissive: reusable with attribution | 0 | A fork of SmartTube for the RayNeo X3 Pro that includes a dim mode with closed caption sup |
| [tropicalstream/SpecSafari](https://github.com/tropicalstream/SpecSafari) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Real-world creature-hunting walking game for the RayNeo X3 Pro |
| [tropicalstream/SunsarraDrift](https://github.com/tropicalstream/SunsarraDrift) | none | **None: all rights reserved, link only** | 0 | Sunsarra Drift: RayNeo X3 Pro first-person space shooter on the Pale Blue galaxy engine, w |
| [tropicalstream/TapBubble](https://github.com/tropicalstream/TapBubble) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Launch different colored bubbles to match colors for the RayNeo X3 Pro |
| [tropicalstream/TapCheckers](https://github.com/tropicalstream/TapCheckers) | none | **None: all rights reserved, link only** | 0 | Checkers (English draughts) vs. a computer opponent for the RayNeo X3 Pro AR glasses — for |
| [tropicalstream/TapChess](https://github.com/tropicalstream/TapChess) | none | **None: all rights reserved, link only** | 0 | Chess vs. a computer opponent for the RayNeo X3 Pro AR glasses — explains illegal moves, 5 |
| [tropicalstream/tapforce](https://github.com/tropicalstream/tapforce) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | R-Type turned vertical for RayNeo X3 Pro AR glasses: a Force pod you dock front or rear, a |
| [tropicalstream/TapFractal](https://github.com/tropicalstream/TapFractal) | NOASSERTION | Custom: read its licence first | 0 | Head-swung fractal toy for the RayNeo X3 Pro — gaze navigation through live raymarched fra |
| [tropicalstream/TapGarden](https://github.com/tropicalstream/TapGarden) | none | **None: all rights reserved, link only** | 0 | Tap Garden is a web app that comfortably displays Radio Garden for the RayNeo X3 Pro Glass |
| [tropicalstream/tapgem](https://github.com/tropicalstream/tapgem) | none | **None: all rights reserved, link only** | 1 | TapGem — voice-driven HUD/desktop for RayNeo X3 Pro AR glasses, controlled by Gemini Live |
| [tropicalstream/TapInvaders](https://github.com/tropicalstream/TapInvaders) | none | **None: all rights reserved, link only** | 0 | Three-mode synthwave Space Invaders sequel to TapMeteors for the RayNeo X3 Pro — Classic, |
| [tropicalstream/TapKugelbahn](https://github.com/tropicalstream/TapKugelbahn) | none | **None: all rights reserved, link only** | 0 | Kinetic vector Kugelbahn (rolling-ball sculpture) for the RayNeo X3 Pro — panoramic 360° t |
| [tropicalstream/TapMahjong](https://github.com/tropicalstream/TapMahjong) | none | **None: all rights reserved, link only** | 0 | Mahjong solitaire (tile matching) for the RayNeo X3 Pro AR glasses — layered layouts, hint |
| [tropicalstream/TapMame](https://github.com/tropicalstream/TapMame) | GPL-2.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | RayNeo X3 Pro AR glasses port of MAME4droid (Current), a GPL-2.0 MAME emulator, with a pho |
| [tropicalstream/TapMemory](https://github.com/tropicalstream/TapMemory) | none | **None: all rights reserved, link only** | 0 | Sound + light memory game (Simon-style) for the RayNeo X3 Pro AR glasses |
| [tropicalstream/TapMeteors](https://github.com/tropicalstream/TapMeteors) | none | **None: all rights reserved, link only** | 0 | Synthwave isometric neon Asteroids remix for the RayNeo X3 Pro AR glasses — you're the gal |
| [tropicalstream/TapMiner](https://github.com/tropicalstream/TapMiner) | none | **None: all rights reserved, link only** | 0 | Moon Patrol lunar side-scroller — PART 1 prequel to TapMeteors and TapInvaders, for the Ra |
| [tropicalstream/TapMuncher](https://github.com/tropicalstream/TapMuncher) | none | **None: all rights reserved, link only** | 0 | X3 Pro neon Pac-Man remix for the RayNeo X3 Pro |
| [tropicalstream/Tapogotchi](https://github.com/tropicalstream/Tapogotchi) | none | **None: all rights reserved, link only** | 0 | Real-time virtual pet for the RayNeo X3 Pro |
| [tropicalstream/TapPong](https://github.com/tropicalstream/TapPong) | none | **None: all rights reserved, link only** | 0 | Canvas Pong remix for the RayNeo X3 Pro |
| [tropicalstream/tapreader](https://github.com/tropicalstream/tapreader) | NOASSERTION | Custom: read its licence first | 0 | TapReader — native ebook/document reader with fish.audio TTS for RayNeo X3 Pro AR glasses |
| [tropicalstream/TapRescue](https://github.com/tropicalstream/TapRescue) | none | **None: all rights reserved, link only** | 0 | X3 Pro remake of the 1982 arcade classic Pooyan — pop the wolves' balloons, rescue the pig |
| [tropicalstream/taprhythm](https://github.com/tropicalstream/taprhythm) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | A rhythm game for RayNeo X3 Pro AR glasses: three lanes mapped to the temple pad's three g |
| [tropicalstream/tapshot](https://github.com/tropicalstream/tapshot) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | A gallery shooter for RayNeo X3 Pro AR glasses: drag to slide the cannon, tap to fire, six |
| [tropicalstream/X3BrainGames](https://github.com/tropicalstream/X3BrainGames) | none | **None: all rights reserved, link only** | 0 | Sudoku, 2048 and Minesweeper natively for RayNeo X3 Pro AR glasses, with per-move autosave |
| [tropicalstream/x3breakout](https://github.com/tropicalstream/x3breakout) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Native RayNeo X3 Pro neon breakout: MCP story campaign, stereo vector visuals, explicit sc |
| [tropicalstream/x3circus](https://github.com/tropicalstream/x3circus) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Circus Atari for RayNeo X3 Pro AR glasses: a torque-accurate seesaw where catching the clo |
| [tropicalstream/x3cycles](https://github.com/tropicalstream/x3cycles) | none | **None: all rights reserved, link only** | 0 | Synthwave isometric light cycles for the RayNeo X3 Pro AR glasses — turn-relative-to-headi |
| [tropicalstream/x3dflappy](https://github.com/tropicalstream/x3dflappy) | none | **None: all rights reserved, link only** | 0 | 3D neon-vector flappy bird with bonus level, open gl es,  for the RayNeo X3 Pro |
| [tropicalstream/X3GalaxySim](https://github.com/tropicalstream/X3GalaxySim) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | This is an experimental alpha build. Science-focused RayNeo X3 Pro stereoscopic galaxy and |
| [tropicalstream/X3Gemini](https://github.com/tropicalstream/X3Gemini) | none | **None: all rights reserved, link only** | 1 | Minimal Gemini Live voice HUD for the RayNeo X3 Pro AR glasses |
| [tropicalstream/X3GeoLibre](https://github.com/tropicalstream/X3GeoLibre) | none | **None: all rights reserved, link only** | 0 | A native RayNeo X3 Pro shell around GeoLibre Web, the open-source cloud GIS platform |
| [tropicalstream/X3Hnefatafl](https://github.com/tropicalstream/X3Hnefatafl) | none | **None: all rights reserved, link only** | 0 | Hnefatafl (Viking chess) for the RayNeo X3 Pro AR glasses — a glowing besieged King, asymm |
| [tropicalstream/X3Hub](https://github.com/tropicalstream/X3Hub) | none | **None: all rights reserved, link only** | 0 | Gemini Live voice HUD with a built-in floating-window browser for RayNeo X3 Pro AR glasses |
| [tropicalstream/x3klax](https://github.com/tropicalstream/x3klax) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | KLAX (1990) remixed for the RayNeo X3 Pro: particle visuals, synthwave soundtrack, playabl |
| [tropicalstream/x3knockout](https://github.com/tropicalstream/x3knockout) | none | **None: all rights reserved, link only** | 0 | RayNeo X3 Pro arcade boxing (Punch-Out!! homage): tap-punch, lean-slip, look-down-duck; a |
| [tropicalstream/X3Mira](https://github.com/tropicalstream/X3Mira) | NOASSERTION | Custom: read its licence first | 0 | Your phone's screen on RayNeo X3 Pro glasses over Wi-Fi — video, audio, temple-pad input, |
| [tropicalstream/X3MiraPhone](https://github.com/tropicalstream/X3MiraPhone) | NOASSERTION | Custom: read its licence first | 1 | The phone half of X3Mira: MediaProjection capture, H.264 + audio streaming to RayNeo X3 Pr |
| [tropicalstream/X3Paranoids](https://github.com/tropicalstream/X3Paranoids) | none | **None: all rights reserved, link only** | 0 | First-person vector tank vs. Recognizers for the RayNeo X3 Pro — a 1982 arcade-cabinet hom |
| [tropicalstream/x3pong](https://github.com/tropicalstream/x3pong) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Pong for RayNeo X3 Pro AR glasses: a faithful 1972 classic mode and a synthwave remix with |
| [tropicalstream/x3science](https://github.com/tropicalstream/x3science) | none | **None: all rights reserved, link only** | 0 | x3science — invisible Gemini-vision + fish.audio scientist commentary for RayNeo X3 Pro |
| [tropicalstream/X3Tetris](https://github.com/tropicalstream/X3Tetris) | none | **None: all rights reserved, link only** | 0 | X3Tetris: RayNeo X3 Pro neon stereoscopic Tetris with panda/bamboo visuals, skill-based ch |
| [tropicalstream/X3Trainer](https://github.com/tropicalstream/X3Trainer) | NOASSERTION | Custom: read its licence first | 0 | AR fitness coach for the RayNeo X3 Pro: sports timer, live watch telemetry, voice coaching |
| [tropicalstream/X3Wars](https://github.com/tropicalstream/X3Wars) | none | **None: all rights reserved, link only** | 0 | First-person vector rail shooter for RayNeo X3 Pro AR glasses — 1983-arcade-style trench r |
| [AlexSKuznetsov/rayneo-x3-spatial-sdk](https://github.com/AlexSKuznetsov/rayneo-x3-spatial-sdk) | Apache-2.0 | Permissive: reusable with attribution | 1 | Unofficial 6DoF head tracking and world-locked rendering for RayNeo X3 Pro AR glasses in p |
| [d0luongkien/rayneo-x3-pro](https://github.com/d0luongkien/rayneo-x3-pro) | Apache-2.0 | Permissive: reusable with attribution | 0 | This is a web browser for the RayNeo X3 Pro |
| [DenisovAV/qdrant-glasses](https://github.com/DenisovAV/qdrant-glasses) | Apache-2.0 | Permissive: reusable with attribution | 0 | On-device multimodal-RAG object memory for RayNeo X3 Pro AR glasses (Snapdragon AR1): YOLO |
| [Dun-John/x3bluetooth](https://github.com/Dun-John/x3bluetooth) | MIT | Permissive: reusable with attribution | 0 | Pair and manage Bluetooth devices on the RayNeo X3 Pro glasses |
| [f1rede/ClawdAR](https://github.com/f1rede/ClawdAR) | none | **None: all rights reserved, link only** | 0 | ClawdBot client for RayNeo X3 Pro AR glasses |
| [informalTechCode/TAPLINKX3](https://github.com/informalTechCode/TAPLINKX3) | Apache-2.0 | Permissive: reusable with attribution | 26 | This is an informal web browser for the RayNeo X3 Pro |
| [JetsonTrial/rayneo_x3_pro_FPV_stream](https://github.com/JetsonTrial/rayneo_x3_pro_FPV_stream) | MIT | Permissive: reusable with attribution | 0 | – |
| [JoshuaTowne8-spec/ACL-RayNeo-Rehab-Demo](https://github.com/JoshuaTowne8-spec/ACL-RayNeo-Rehab-Demo) | none | **None: all rights reserved, link only** | 0 | RayNeo X3 Pro AR running rehabilitation prototype with ESP32 pressure and knee-angle sensi |
| [Lambozhuang/rayneo-x3-pro-gemini-live](https://github.com/Lambozhuang/rayneo-x3-pro-gemini-live) | none | **None: all rights reserved, link only** | 0 | – |
| [Lambozhuang/rayneo-x3-pro-livekit](https://github.com/Lambozhuang/rayneo-x3-pro-livekit) | none | **None: all rights reserved, link only** | 1 | – |
| [MaxManausa/Metaverse-Max-RayNeo-X3-Pro-Set-Up](https://github.com/MaxManausa/Metaverse-Max-RayNeo-X3-Pro-Set-Up) | none | **None: all rights reserved, link only** | 1 | December 7 - This is the RayNeo X3 Pro sample project set-up and some of my own projects |
| [MaxManausa/RayNeo-X3-Pro-Apps](https://github.com/MaxManausa/RayNeo-X3-Pro-Apps) | none | **None: all rights reserved, link only** | 0 | Collection of Unity apks and sample scenes for RayNeo X3 Pro |
| [MaxManausa/RayNeoX3Pro-MITSample](https://github.com/MaxManausa/RayNeoX3Pro-MITSample) | none | **None: all rights reserved, link only** | 1 | – |
| [op1255566-wq/rayneo-x3pro-community](https://github.com/op1255566-wq/rayneo-x3pro-community) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | RayNeo X3 Pro 双目应用适配：BBLL helper、Lemuroid 补丁、轻量浏览器与蓝牙/启动器研究；保留来源与许可声明。 |
| [paradoxinversion/shintai-os](https://github.com/paradoxinversion/shintai-os) | AGPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 0 | Wearable ESP32-S3 multi-sensor platform (Shintai-OS): Python ground-station + RayNeo X3 Pr |
| [Quad-Labs/RayDesk](https://github.com/Quad-Labs/RayDesk) | GPL-3.0 | Copyleft: reuse makes this app GPL/AGPL | 6 | Remote desktop streaming client for RayNeo X3 Pro AR glasses |
| [RayNeo-AI-2025/OpenClaw](https://github.com/RayNeo-AI-2025/OpenClaw) | NOASSERTION | Custom: read its licence first | 12 | Your OpenClaw on RayNeo X3 Pro. |
| [shyam-barange/MultiSet-RayNeoSDK](https://github.com/shyam-barange/MultiSet-RayNeoSDK) | none | **None: all rights reserved, link only** | 1 | MultiSet SDK for RayNeo X3 Pro glasses |
| [TheophileGaudin/Everyday](https://github.com/TheophileGaudin/Everyday) | none | **None: all rights reserved, link only** | 6 | "Everyday", a dashboard for AR glasses (developed on RayNeo X3Pro) |
| [TheophileGaudin/TapLink](https://github.com/TheophileGaudin/TapLink) | none | **None: all rights reserved, link only** | 0 | Browser developed for RayNeo X2 with 3dof support. Still working on X3Pro. A fork has been |
| [TheophileGaudin/X3player](https://github.com/TheophileGaudin/X3player) | MIT | Permissive: reusable with attribution | 0 | Standalone local media player for RayNeo X3 Pro AR glasses |
| [tmad4000/RayNeoX3Tools](https://github.com/tmad4000/RayNeoX3Tools) | none | **None: all rights reserved, link only** | 0 | CLI-controllable Android browser for RayNeo X3 Pro AR glasses — lets you drive it over ADB |
| [TZHU64/spatial-memory-ar](https://github.com/TZHU64/spatial-memory-ar) | none | **None: all rights reserved, link only** | 1 | Source-only visual memory and voice recall prototype for RayNeo X3 Pro AR glasses |
| [urbanonymous/rayneo-x3-pro-react-native](https://github.com/urbanonymous/rayneo-x3-pro-react-native) | Apache-2.0 | Permissive: reusable with attribution | 0 | Boilerplate app for integrating a react native android app with the rayneo x3 pro |
| [vincentwi/meshtalk](https://github.com/vincentwi/meshtalk) | none | **None: all rights reserved, link only** | 0 | Infrastructure-free mesh walkie-talkie for RayNeo X3 Pro AR glasses |
| [wingsfly/glassclaw](https://github.com/wingsfly/glassclaw) | none | **None: all rights reserved, link only** | 0 | GlassClaw: OpenClaw Gateway client for RayNeo X3 Pro AR glasses |

*Licences are as GitHub reports them and can change: check the repository before reusing anything.*
