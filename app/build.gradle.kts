plugins {
    id("com.android.application")
}

android {
    namespace = "df.root"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sysopbp.fragx"
        minSdk = 32
        targetSdk = 37
        versionCode = 34
        versionName = "1.17-preview.1"

        // Pinned: an unpinned NDK means AGP's default, which moves under you on an
        // AGP bump and silently changes the shipped native code. If CI names a
        // different version on its first run, use that one.
        ndkVersion = "27.2.12479018"

        // Every UI string is hardcoded in Java, so the library locale resources
        // are dead weight.
        resourceConfigurations += listOf("en")

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("keystore") {
            storeFile = file("keystore.jks")
            storeType = "PKCS12"
            storePassword = System.getenv("FRAGX_KEYSTORE_PASSWORD") ?: "dirtyfrag"
            keyAlias = System.getenv("FRAGX_KEY_ALIAS") ?: "dirtyfrag"
            keyPassword = System.getenv("FRAGX_KEYSTORE_PASSWORD") ?: "dirtyfrag"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("keystore")
            // So a pasted run log says which build produced it. The update
            // check's numeric compare ignores the suffix.
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.getByName("keystore")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

// AGP 9 removed the old applicationVariants API; this is the replacement.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("fragx.apk")
        }
    }
}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        viewBinding = true
        // The toolbar version pill reads BuildConfig.VERSION_NAME, so the pill
        // can never fall behind versionName again.
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    testImplementation("junit:junit:4.13.2")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
}
