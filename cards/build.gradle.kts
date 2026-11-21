// VeniceX3 cards: the pure-Kotlin core of the app's character features, published to
// GitHub Packages as dev.sweitz:venicex3-cards.
//   - CharacterCard: SillyTavern / TavernAI card reader (JSON V1/V2/V3, PNG tEXt/zTXt/iTXt)
//   - PromptBuilder, Conversation: SillyTavern-style prompt assembly and {{char}}/{{user}} macros
//   - ContentPolicy: mature-mode gating and the always-on minor safeguards
//   - ImageIntent, CardStores: picture requests and card-store link resolution
plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
}

group = "dev.sweitz"
// The release workflow passes -PcardsVersion=<app versionName>; local builds are snapshots.
version = (findProperty("cardsVersion") as String?) ?: "0.2.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    // org.json is part of Android, so the library doesn't bundle it (a second copy breaks
    // Android builds). Plain-JVM users add org.json:json themselves; see cards/README.md.
    compileOnly("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

publishing {
    publications {
        create<MavenPublication>("cards") {
            artifactId = "venicex3-cards"
            from(components["java"])
            pom {
                name.set("VeniceX3 cards")
                description.set("SillyTavern character card reader, prompt builder and content policy from VeniceX3.")
                url.set("https://github.com/Sweitz1/Backup")
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            // GitHub Packages needs a lowercase owner; GITHUB_REPOSITORY keeps this right after a rename.
            val repo = (System.getenv("GITHUB_REPOSITORY") ?: "Sweitz1/Backup").lowercase()
            url = uri("https://maven.pkg.github.com/$repo")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
