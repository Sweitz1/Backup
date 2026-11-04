import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Optional: put VENICE_API_KEY=... in local.properties (never committed) to bake a key into
// debug builds. You can also set or change it later over adb; see README.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.sweitz.veniceglasses"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.sweitz.veniceglasses"
        minSdk = 26 // RayNeo Mercury SDK minimum
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "DEFAULT_API_KEY", "\"${localProps.getProperty("VENICE_API_KEY", "")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Don't ship a baked-in key in release builds; provision it over adb instead.
            buildConfigField("String", "DEFAULT_API_KEY", "\"\"")
        }
    }

    buildFeatures {
        viewBinding = true // BaseMirrorActivity inflates the layout twice (left + right eye) via its binding
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // RayNeo X3 Pro SDK (binocular mirroring, temple touch gestures, glasses toasts)
    implementation(files("libs/MercuryAndroidSDK-v0.2.6.aar"))

    // What the SDK is compiled against
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Venice API (streaming chat + audio upload)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
}
