# Third-party notices

What VeniceX3 is built with, and under what terms. The same list is in the app under
triple tap → **Credits & licences**, generated from
[`Credits.kt`](app/src/main/java/dev/sweitz/veniceglasses/data/Credits.kt).

## ⚠ Dependency with no stated licence

| Dependency | By | Status |
| --- | --- | --- |
| RayNeo Mercury Android SDK 0.2.6 (`app/libs/MercuryAndroidSDK-v0.2.6.aar`) | RayNeo / FFalcon | **No licence found.** The `.aar` has no LICENSE or NOTICE file, and RayNeo's developer docs, where it's distributed, don't give redistribution terms. The app needs it to run on the glasses. |

What this means: this repository is public and commits RayNeo's binary. Until RayNeo publishes
terms, the safer choice is to stop committing the `.aar` and have builders download it from
[RayNeo's developer docs](https://rayneo.gitbook.io/rayneo-devdoc/) into `app/libs/`, or to ask
RayNeo (for example through the RayNeo Innovators Program) for permission to redistribute it.
No change has been made yet; this is flagged for the owner to decide.

## Open-source libraries (shipped in the APK)

| Library | By | Licence |
| --- | --- | --- |
| AndroidX core, appcompat, activity, fragment, recyclerview, lifecycle | Google / AOSP | Apache-2.0 |
| Kotlin standard library | JetBrains | Apache-2.0 |
| kotlinx.coroutines | JetBrains | Apache-2.0 |
| OkHttp 4.12 and Okio | Square | Apache-2.0 |

Apache-2.0 text: https://www.apache.org/licenses/LICENSE-2.0

## Test-only (not shipped)

| Library | By | Licence |
| --- | --- | --- |
| JUnit 4.13.2 | JUnit team | EPL-1.0 |
| JSON-java 20240303 | Sean Leary | Public domain |

## Services and formats (no code included)

| What | By | Terms |
| --- | --- | --- |
| Venice AI API: chat, speech-to-text, images | Venice.ai | Venice terms of service |
| Character Card V1/V2/V3 format | TavernAI / SillyTavern community | Open spec. VeniceX3 reads the format with its own code; no SillyTavern (AGPL-3.0) code is used. |

## Community projects

Other RayNeo X3 Pro projects are credited in [COMMUNITY.md](COMMUNITY.md). VeniceX3 uses no code
from them; 45 of the 75 have no licence, so their code can't be reused.
