# venicex3-cards

The pure-Kotlin core of VeniceX3's character features, as a small library:

| Class | What it does |
| --- | --- |
| `CharacterCard` | Reads SillyTavern / TavernAI cards: JSON (V1, V2, V3, Pygmalion fields) and PNG (`ccv3` / `chara` in tEXt, zTXt or iTXt) |
| `Character` | The parsed card, plus the built-in assistant and companion |
| `PromptBuilder`, `Conversation` | SillyTavern-style prompt order (main prompt, description, personality, scenario, examples, greeting, history, post-history) and `{{char}}` / `{{user}}` macros |
| `ContentPolicy`, `MatureAccess` | Mature-mode gating, minor detection, and refusing mature cards that involve minors |
| `ImageIntent` | Spots "draw… / show me a picture of… / send me a selfie" requests |
| `CardStores` | Card store directory and store link → card download URL |

Package: `dev.sweitz.veniceglasses.data`. Runs on Android and the plain JVM (Java 17+).

## Install from GitHub Packages

GitHub Packages needs a personal access token (classic) with `read:packages`, even for public
packages. In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/sweitz1/backup")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Put `gpr.user=<your GitHub username>` and `gpr.key=<token>` in `~/.gradle/gradle.properties`
(never in the project). Then:

```kotlin
dependencies {
    implementation("dev.sweitz:venicex3-cards:0.2.0")
    // Plain JVM only: Android already includes org.json.
    implementation("org.json:json:20240303")
}
```

Maven users: add the same repository URL and a `<server>` with your token to `~/.m2/settings.xml`
(see GitHub's "Working with the Apache Maven registry"), then depend on
`dev.sweitz:venicex3-cards:0.2.0`.

## Example

```kotlin
val card = CharacterCard.parse(File("Rin.png").readBytes(), id = "rin")
val access = MatureAccess() // everything off
if (!ContentPolicy.importRefused(card) && ContentPolicy.characterVisible(card, access)) {
    val system = PromptBuilder.systemPrompt(card, userName = "Sam", matureText = ContentPolicy.textAllowed(card, access))
    val history = Conversation().apply { greeting = PromptBuilder.greeting(card, "Sam") }
    history.addUser("Hi!")
    val messages = history.forRequest(system, PromptBuilder.postHistory(card, "Sam"))
}
```

## Publishing

The Release workflow publishes it on each version tag, with the same version as the app.
To publish by hand: `GITHUB_ACTOR=… GITHUB_TOKEN=… ./gradlew :cards:publish -PcardsVersion=0.2.0`.
