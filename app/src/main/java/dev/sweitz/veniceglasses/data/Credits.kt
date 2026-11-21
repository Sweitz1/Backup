package dev.sweitz.veniceglasses.data

/**
 * Credits shown in the app (menu → Credits & licenses) and in THIRD_PARTY_NOTICES.md / COMMUNITY.md.
 *
 * [dependencies] is what VeniceX3 is built with. [community] lists other RayNeo X3 Pro projects
 * for credit and discovery only: VeniceX3 contains no code from any of them.
 */
object Credits {
    enum class LicenseKind(val label: String, val reuse: String) {
        PERMISSIVE("permissive", "reusable with attribution"),
        COPYLEFT("copyleft", "reuse would make VeniceX3 GPL/AGPL"),
        WEAK_COPYLEFT("weak copyleft", "changes to its own files stay open; fine to link"),
        CUSTOM("custom", "custom licence: read it before reusing"),
        NONE("no licence", "all rights reserved: don't copy, link only"),
        SERVICE("service", "used over the network under its terms"),
    }

    data class Dependency(val name: String, val by: String, val license: String, val kind: LicenseKind, val note: String = "")

    data class Project(val repo: String, val spdx: String, val description: String) {
        val url: String get() = "https://github.com/$repo"
        val kind: LicenseKind get() = kindOf(spdx)
    }

    fun kindOf(spdx: String): LicenseKind = when {
        spdx in setOf("MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "Unlicense", "0BSD", "Public-Domain") -> LicenseKind.PERMISSIVE
        spdx.startsWith("LGPL") || spdx.startsWith("MPL") || spdx.startsWith("EPL") -> LicenseKind.WEAK_COPYLEFT
        spdx.startsWith("GPL") || spdx.startsWith("AGPL") -> LicenseKind.COPYLEFT
        spdx == "NOASSERTION" -> LicenseKind.CUSTOM
        else -> LicenseKind.NONE
    }

    val dependencies = listOf(
        Dependency(
            "RayNeo Mercury Android SDK 0.2.6", "RayNeo / FFalcon", "not stated", LicenseKind.NONE,
            "Bundled as app/libs/*.aar. The .aar carries no licence file; it's RayNeo's official SDK from " +
                "their developer docs. Redistribution terms are unconfirmed.",
        ),
        Dependency("AndroidX (core, appcompat, activity, fragment, recyclerview, lifecycle)", "Google / AOSP", "Apache-2.0", LicenseKind.PERMISSIVE),
        Dependency("Kotlin standard library", "JetBrains", "Apache-2.0", LicenseKind.PERMISSIVE),
        Dependency("kotlinx.coroutines", "JetBrains", "Apache-2.0", LicenseKind.PERMISSIVE),
        Dependency("OkHttp 4.12 and Okio", "Square", "Apache-2.0", LicenseKind.PERMISSIVE),
        Dependency("JUnit 4.13.2 (tests only)", "JUnit team", "EPL-1.0", LicenseKind.WEAK_COPYLEFT, "Test-only, not shipped in the APK."),
        Dependency("JSON-java 20240303 (tests only)", "Sean Leary", "Public Domain", LicenseKind.PERMISSIVE, "Test-only, not shipped in the APK."),
        Dependency("Venice AI API (chat, speech-to-text, images)", "Venice.ai", "Terms of service", LicenseKind.SERVICE),
        Dependency(
            "Character Card V1/V2/V3 format", "TavernAI / SillyTavern community", "open spec", LicenseKind.SERVICE,
            "Only the file format is implemented, in original code. No SillyTavern (AGPL-3.0) code is used.",
        ),
    )

    /** Public RayNeo X3 Pro projects on GitHub, with the licence GitHub reports (SPDX id). */
    val community = listOf(
        Project("AlexSKuznetsov/rayneo-x3-spatial-sdk", "Apache-2.0", "Unofficial 6DoF head tracking and world-locked rendering for RayNeo X3 Pro AR glasses in p"),
        Project("d0luongkien/rayneo-x3-pro", "Apache-2.0", "This is a web browser for the RayNeo X3 Pro"),
        Project("DenisovAV/qdrant-glasses", "Apache-2.0", "On-device multimodal-RAG object memory for RayNeo X3 Pro AR glasses (Snapdragon AR1): YOLO"),
        Project("Dun-John/x3bluetooth", "MIT", "Pair and manage Bluetooth devices on the RayNeo X3 Pro glasses"),
        Project("f1rede/ClawdAR", "NONE", "ClawdBot client for RayNeo X3 Pro AR glasses"),
        Project("informalTechCode/TAPLINKX3", "Apache-2.0", "This is an informal web browser for the RayNeo X3 Pro"),
        Project("JetsonTrial/rayneo_x3_pro_FPV_stream", "MIT", ""),
        Project("JoshuaTowne8-spec/ACL-RayNeo-Rehab-Demo", "NONE", "RayNeo X3 Pro AR running rehabilitation prototype with ESP32 pressure and knee-angle sensi"),
        Project("Lambozhuang/rayneo-x3-pro-gemini-live", "NONE", ""),
        Project("Lambozhuang/rayneo-x3-pro-livekit", "NONE", ""),
        Project("MaxManausa/Metaverse-Max-RayNeo-X3-Pro-Set-Up", "NONE", "December 7 - This is the RayNeo X3 Pro sample project set-up and some of my own projects"),
        Project("MaxManausa/RayNeo-X3-Pro-Apps", "NONE", "Collection of Unity apks and sample scenes for RayNeo X3 Pro"),
        Project("MaxManausa/RayNeoX3Pro-MITSample", "NONE", ""),
        Project("op1255566-wq/rayneo-x3pro-community", "GPL-3.0", "RayNeo X3 Pro 双目应用适配：BBLL helper、Lemuroid 补丁、轻量浏览器与蓝牙/启动器研究；保留来源与许可声明。"),
        Project("paradoxinversion/shintai-os", "AGPL-3.0", "Wearable ESP32-S3 multi-sensor platform (Shintai-OS): Python ground-station + RayNeo X3 Pr"),
        Project("Quad-Labs/RayDesk", "GPL-3.0", "Remote desktop streaming client for RayNeo X3 Pro AR glasses"),
        Project("RayNeo-AI-2025/OpenClaw", "NOASSERTION", "Your OpenClaw on RayNeo X3 Pro."),
        Project("shyam-barange/MultiSet-RayNeoSDK", "NONE", "MultiSet SDK for RayNeo X3 Pro glasses"),
        Project("TheophileGaudin/Everyday", "NONE", "\"Everyday\", a dashboard for AR glasses (developed on RayNeo X3Pro)"),
        Project("TheophileGaudin/TapLink", "NONE", "Browser developed for RayNeo X2 with 3dof support. Still working on X3Pro. A fork has been"),
        Project("TheophileGaudin/X3player", "MIT", "Standalone local media player for RayNeo X3 Pro AR glasses"),
        Project("tmad4000/RayNeoX3Tools", "NONE", "CLI-controllable Android browser for RayNeo X3 Pro AR glasses — lets you drive it over ADB"),
        Project("tropicalstream/Breakthrough", "NONE", "Dan Troyka's Breakthrough (2000) for the RayNeo X3 Pro AR glasses — race a wall of pieces"),
        Project("tropicalstream/Hammerklavier", "NONE", "Hammerklavier: a sampled grand, upright and Bach-era harpsichord MIDI player for the RayNe"),
        Project("tropicalstream/InnerCosmos", "NONE", "A railed stereoscopic descent through the human body for the RayNeo X3 Pro AR glasses — tw"),
        Project("tropicalstream/MathCosmos", "NONE", "A railed stereoscopic tour of mathematics for the RayNeo X3 Pro AR glasses. Six tours, 76"),
        Project("tropicalstream/RayNeoSpyHunt", "GPL-3.0", "Spy Hunt XR: a fan recreation of the 1983 arcade game for RayNeo X3 Pro AR glasses — stere"),
        Project("tropicalstream/SmartTubular", "MIT", "A fork of SmartTube for the RayNeo X3 Pro that includes a dim mode with closed caption sup"),
        Project("tropicalstream/SpecSafari", "GPL-3.0", "Real-world creature-hunting walking game for the RayNeo X3 Pro"),
        Project("tropicalstream/SunsarraDrift", "NONE", "Sunsarra Drift: RayNeo X3 Pro first-person space shooter on the Pale Blue galaxy engine, w"),
        Project("tropicalstream/TapBubble", "GPL-3.0", "Launch different colored bubbles to match colors for the RayNeo X3 Pro"),
        Project("tropicalstream/TapCheckers", "NONE", "Checkers (English draughts) vs. a computer opponent for the RayNeo X3 Pro AR glasses — for"),
        Project("tropicalstream/TapChess", "NONE", "Chess vs. a computer opponent for the RayNeo X3 Pro AR glasses — explains illegal moves, 5"),
        Project("tropicalstream/tapforce", "GPL-3.0", "R-Type turned vertical for RayNeo X3 Pro AR glasses: a Force pod you dock front or rear, a"),
        Project("tropicalstream/TapFractal", "NOASSERTION", "Head-swung fractal toy for the RayNeo X3 Pro — gaze navigation through live raymarched fra"),
        Project("tropicalstream/TapGarden", "NONE", "Tap Garden is a web app that comfortably displays Radio Garden for the RayNeo X3 Pro Glass"),
        Project("tropicalstream/tapgem", "NONE", "TapGem — voice-driven HUD/desktop for RayNeo X3 Pro AR glasses, controlled by Gemini Live"),
        Project("tropicalstream/TapInvaders", "NONE", "Three-mode synthwave Space Invaders sequel to TapMeteors for the RayNeo X3 Pro — Classic,"),
        Project("tropicalstream/TapKugelbahn", "NONE", "Kinetic vector Kugelbahn (rolling-ball sculpture) for the RayNeo X3 Pro — panoramic 360° t"),
        Project("tropicalstream/TapMahjong", "NONE", "Mahjong solitaire (tile matching) for the RayNeo X3 Pro AR glasses — layered layouts, hint"),
        Project("tropicalstream/TapMame", "GPL-2.0", "RayNeo X3 Pro AR glasses port of MAME4droid (Current), a GPL-2.0 MAME emulator, with a pho"),
        Project("tropicalstream/TapMemory", "NONE", "Sound + light memory game (Simon-style) for the RayNeo X3 Pro AR glasses"),
        Project("tropicalstream/TapMeteors", "NONE", "Synthwave isometric neon Asteroids remix for the RayNeo X3 Pro AR glasses — you're the gal"),
        Project("tropicalstream/TapMiner", "NONE", "Moon Patrol lunar side-scroller — PART 1 prequel to TapMeteors and TapInvaders, for the Ra"),
        Project("tropicalstream/TapMuncher", "NONE", "X3 Pro neon Pac-Man remix for the RayNeo X3 Pro"),
        Project("tropicalstream/Tapogotchi", "NONE", "Real-time virtual pet for the RayNeo X3 Pro"),
        Project("tropicalstream/TapPong", "NONE", "Canvas Pong remix for the RayNeo X3 Pro"),
        Project("tropicalstream/tapreader", "NOASSERTION", "TapReader — native ebook/document reader with fish.audio TTS for RayNeo X3 Pro AR glasses"),
        Project("tropicalstream/TapRescue", "NONE", "X3 Pro remake of the 1982 arcade classic Pooyan — pop the wolves' balloons, rescue the pig"),
        Project("tropicalstream/taprhythm", "GPL-3.0", "A rhythm game for RayNeo X3 Pro AR glasses: three lanes mapped to the temple pad's three g"),
        Project("tropicalstream/tapshot", "GPL-3.0", "A gallery shooter for RayNeo X3 Pro AR glasses: drag to slide the cannon, tap to fire, six"),
        Project("tropicalstream/X3BrainGames", "NONE", "Sudoku, 2048 and Minesweeper natively for RayNeo X3 Pro AR glasses, with per-move autosave"),
        Project("tropicalstream/x3breakout", "GPL-3.0", "Native RayNeo X3 Pro neon breakout: MCP story campaign, stereo vector visuals, explicit sc"),
        Project("tropicalstream/x3circus", "GPL-3.0", "Circus Atari for RayNeo X3 Pro AR glasses: a torque-accurate seesaw where catching the clo"),
        Project("tropicalstream/x3cycles", "NONE", "Synthwave isometric light cycles for the RayNeo X3 Pro AR glasses — turn-relative-to-headi"),
        Project("tropicalstream/x3dflappy", "NONE", "3D neon-vector flappy bird with bonus level, open gl es,  for the RayNeo X3 Pro"),
        Project("tropicalstream/X3GalaxySim", "GPL-3.0", "This is an experimental alpha build. Science-focused RayNeo X3 Pro stereoscopic galaxy and"),
        Project("tropicalstream/X3Gemini", "NONE", "Minimal Gemini Live voice HUD for the RayNeo X3 Pro AR glasses"),
        Project("tropicalstream/X3GeoLibre", "NONE", "A native RayNeo X3 Pro shell around GeoLibre Web, the open-source cloud GIS platform"),
        Project("tropicalstream/X3Hnefatafl", "NONE", "Hnefatafl (Viking chess) for the RayNeo X3 Pro AR glasses — a glowing besieged King, asymm"),
        Project("tropicalstream/X3Hub", "NONE", "Gemini Live voice HUD with a built-in floating-window browser for RayNeo X3 Pro AR glasses"),
        Project("tropicalstream/x3klax", "GPL-3.0", "KLAX (1990) remixed for the RayNeo X3 Pro: particle visuals, synthwave soundtrack, playabl"),
        Project("tropicalstream/x3knockout", "NONE", "RayNeo X3 Pro arcade boxing (Punch-Out!! homage): tap-punch, lean-slip, look-down-duck; a"),
        Project("tropicalstream/X3Mira", "NOASSERTION", "Your phone's screen on RayNeo X3 Pro glasses over Wi-Fi — video, audio, temple-pad input,"),
        Project("tropicalstream/X3MiraPhone", "NOASSERTION", "The phone half of X3Mira: MediaProjection capture, H.264 + audio streaming to RayNeo X3 Pr"),
        Project("tropicalstream/X3Paranoids", "NONE", "First-person vector tank vs. Recognizers for the RayNeo X3 Pro — a 1982 arcade-cabinet hom"),
        Project("tropicalstream/x3pong", "GPL-3.0", "Pong for RayNeo X3 Pro AR glasses: a faithful 1972 classic mode and a synthwave remix with"),
        Project("tropicalstream/x3science", "NONE", "x3science — invisible Gemini-vision + fish.audio scientist commentary for RayNeo X3 Pro"),
        Project("tropicalstream/X3Tetris", "NONE", "X3Tetris: RayNeo X3 Pro neon stereoscopic Tetris with panda/bamboo visuals, skill-based ch"),
        Project("tropicalstream/X3Trainer", "NOASSERTION", "AR fitness coach for the RayNeo X3 Pro: sports timer, live watch telemetry, voice coaching"),
        Project("tropicalstream/X3Wars", "NONE", "First-person vector rail shooter for RayNeo X3 Pro AR glasses — 1983-arcade-style trench r"),
        Project("TZHU64/spatial-memory-ar", "NONE", "Source-only visual memory and voice recall prototype for RayNeo X3 Pro AR glasses"),
        Project("urbanonymous/rayneo-x3-pro-react-native", "Apache-2.0", "Boilerplate app for integrating a react native android app with the rayneo x3 pro"),
        Project("vincentwi/meshtalk", "NONE", "Infrastructure-free mesh walkie-talkie for RayNeo X3 Pro AR glasses"),
        Project("wingsfly/glassclaw", "NONE", "GlassClaw: OpenClaw Gateway client for RayNeo X3 Pro AR glasses"),
    )

    /** Dependencies that aren't under a clear licence: called out in the app and the notices. */
    val unlicensedDependencies: List<Dependency> get() = dependencies.filter { it.kind == LicenseKind.NONE }
}
