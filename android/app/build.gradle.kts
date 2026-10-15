plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

/** Reads the required x.y.z from the repository-root VERSION file. */
data class AppVersion(val name: String, val code: Int)

val appVersion: AppVersion = run {
    val f = rootProject.file("../VERSION")
    require(f.exists()) { "VERSION file missing at ${f.absolutePath}" }
    val name = f.readText().trim()
    val m = Regex("^((?:0|[1-9][0-9]*))\\.((?:0|[1-9][0-9]*))\\.((?:0|[1-9][0-9]*))$").matchEntire(name)
        ?: error("VERSION must use exactly x.y.z with no zero-padded parts, got '$name'")
    val (x, y, z) = m.destructured.toList().map { it.toInt() }
    require(y < 1000 && z < 1000) { "y and z must stay below 1000 for versionCode" }
    AppVersion(name, x * 1_000_000 + y * 1_000 + z)
}

android {
    namespace = "com.krishna.infodeck"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.krishna.infodeck"
        minSdk = 26
        targetSdk = 34
        // Versioning (project-wide): x.y.z of plain integers, read from the repo-root VERSION file.
        // versionCode = x*1,000,000 + y*1,000 + z — computed, never typed, so it cannot drift.
        // CI may override the name for beta builds with -PversionNameOverride=1.12.0-beta.3
        versionCode = appVersion.code
        versionName = (project.findProperty("versionNameOverride") as String?) ?: appVersion.name
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    // Signing comes ONLY from the environment (GitHub secrets in CI, local env vars on a dev machine).
    // No keystore and no password ever live in this repository. Without them the release APK is
    // built unsigned and the workflow refuses to publish it.
    val keystorePath: String? = System.getenv("INFODECK_KEYSTORE")
    val signingReady = keystorePath != null && file(keystorePath).exists() &&
        System.getenv("INFODECK_STOREPASS") != null && System.getenv("INFODECK_KEYPASS") != null
    if (signingReady) {
        signingConfigs {
            create("infodeck") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("INFODECK_STOREPASS")
                // An unset optional GitHub secret arrives as "", not null - treat blank as unset.
                keyAlias = System.getenv("INFODECK_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "infodeck"
                keyPassword = System.getenv("INFODECK_KEYPASS")
            }
        }
    } else {
        logger.warn("InfoDeck: signing secrets not set - release APK will be UNSIGNED")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingReady) signingConfig = signingConfigs.getByName("infodeck")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    testOptions { unitTests { isReturnDefaultValues = true } }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-process:2.8.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
