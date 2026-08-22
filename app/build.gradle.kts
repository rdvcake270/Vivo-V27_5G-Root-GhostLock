import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningProperties = Properties().apply {
    val signingFile = project.file("release-signing.txt")
    if (signingFile.isFile) {
        signingFile.inputStream().use { load(it) }
    }
}

fun signingValue(name: String): String? =
    System.getenv(name) ?: releaseSigningProperties.getProperty(name)

android {
    namespace = "com.ankitrawatgit.iqoo9rootghostlock"
    compileSdk = 37
    // Pinned rather than left to AGP's default so a checkout and CI compile the
    // native probe with the same toolchain. This is what AGP 9.2.1 selects on
    // its own today; CI installs exactly it.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.ankitrawatgit.iqoo9rootghostlock"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += "arm64-v8a"
        }

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=none"
            }
        }
    }

    signingConfigs {
        // release.jks and release-signing.txt are local signing material and
        // are ignored by Git. Environment variables override the values in
        // release-signing.txt when a CI or a different local key is needed.
        if (file("release.jks").exists()) {
            create("release") {
                storeFile = file("release.jks")
                storePassword = signingValue("STORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (file("release.jks").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Required, not a preference: the APK ships libcve43499root.so, which
        // is an executable rather than a library. InstallViewModel runs it with
        // ProcessBuilder out of nativeLibraryDir, and only legacy packaging
        // extracts it there as a real file. Uncompressed-and-mapped leaves
        // nothing to exec.
        jniLibs.useLegacyPackaging = true
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
