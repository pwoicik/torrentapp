import com.android.build.api.dsl.ApkSigningConfig

plugins {
    alias(libs.plugins.agp)
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
    id("kotlin-parcelize")
}

android {
    namespace = "com.github.pwoicik.torrentapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.pwoicik.torrentapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        debug.apply {
            storeFile = file("debug-key.jks")
            storePassword = "debugkey"
            keyAlias = "debugkey"
            keyPassword = "debugkey"
        }

        release.apply {
            storeFile = rootProject.file("secret/upload-key.jks")
            storePassword = "uploadkey"
            keyAlias = "uploadkey"
            keyPassword = "uploadkey"
        }
    }

    buildTypes {
        val release by getting {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.release
        }

        create("staging") {
            initWith(release)
            applicationIdSuffix = ".staging"
            isDebuggable = true
            proguardFile("staging-rules.pro")
            signingConfig = signingConfigs.debug
        }

        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        options.freeCompilerArgs.addAll(
            "-Xcontext-receivers",
        )
        options.optIn.addAll(
            "kotlinx.coroutines.FlowPreview",
            "androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/{INDEX.LIST,io.netty.versions.properties}"
        }
    }
}

val NamedDomainObjectContainer<out ApkSigningConfig>.debug get() = maybeCreate("debug")
val NamedDomainObjectContainer<out ApkSigningConfig>.release get() = maybeCreate("release")

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.github.pwoicik.torrentapp.db")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            verifyMigrations.set(true)
            generateAsync.set(true)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.android.core)
    implementation(libs.android.lifecycle)
    implementation(libs.android.lifecycle.service)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.android.test.espresso)

    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.lifecycle)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.tooling.data)
    implementation(libs.compose.tooling.preview)
    implementation(libs.compose.m3)
    implementation(libs.compose.icons)
    androidTestImplementation(libs.compose.test)

    implementation(libs.circuit.foundation)
    implementation(libs.circuit.android)
    implementation(libs.circuit.gestureNavigation)
    implementation(libs.circuit.overlay)
    testImplementation(libs.circuit.test)

    testImplementation(libs.junit)

    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(libs.libtorrent)
    implementation(libs.libtorrent.arm32)
    implementation(libs.libtorrent.arm64)
    implementation(libs.libtorrent.amd64)

    implementation(libs.ktinject)
    ksp(libs.ktinject.compiler)

    implementation(libs.sqldelight.android)
    implementation(libs.sqldelight.coroutines)

    implementation(libs.arrow)
    implementation(libs.arrow.coroutines)
}
