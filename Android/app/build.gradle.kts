import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}
// Trigger Force Sync

android {
    namespace = "com.katchy.focuslive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.brishwlaz"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "1.2.22"
        setProperty("archivesBaseName", "Brish_v$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Evita RuntimeException en android.util.Log durante tests JVM puros
            isReturnDefaultValues = true
        }
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { stream ->
                    localProps.load(stream)
                }
            }

            val keystorePath = localProps.getProperty("RELEASE_STORE_FILE")
                ?: (if (project.hasProperty("RELEASE_STORE_FILE")) project.property("RELEASE_STORE_FILE") as String else "")
            val keystorePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                ?: (if (project.hasProperty("RELEASE_STORE_PASSWORD")) project.property("RELEASE_STORE_PASSWORD") as String else "")
            val keyAliasName = localProps.getProperty("RELEASE_KEY_ALIAS")
                ?: (if (project.hasProperty("RELEASE_KEY_ALIAS")) project.property("RELEASE_KEY_ALIAS") as String else "")
            val keyPasswordVal = localProps.getProperty("RELEASE_KEY_PASSWORD")
                ?: (if (project.hasProperty("RELEASE_KEY_PASSWORD")) project.property("RELEASE_KEY_PASSWORD") as String else "")

            if (keystorePath.isNotEmpty()) {
                val keystoreFile = file(keystorePath)
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = keystorePassword
                    keyAlias = keyAliasName
                    keyPassword = keyPasswordVal
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

gradle.taskGraph.whenReady {
    val needsReleaseKeystore = gradle.taskGraph.allTasks.any { task ->
        val name = task.name
        name.equals("assembleRelease", ignoreCase = true) ||
            name.equals("bundleRelease", ignoreCase = true) ||
            name.startsWith("packageRelease")
    }
    if (needsReleaseKeystore) {
        val store = android.signingConfigs.getByName("release").storeFile
        if (store == null || !store.exists()) {
            throw org.gradle.api.GradleException(
                "Release keystore missing. Set RELEASE_STORE_FILE in local.properties. Debug signing fallback is disabled."
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    // Navigation
    implementation(libs.androidx.navigation.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Glance Widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)
    kapt(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // Google ML Kit Text Recognition
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation(libs.androidx.benchmark.macro.junit4)
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
    }
}

// Auto-copy only the APK this task just built. Matching assemble* / bundle* also
// fired on unit-test helper tasks and recopied leftover 1.2.20 APKs.
tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.configureEach {
    doLast {
        val isRelease = name.contains("Release", ignoreCase = true)
        val variant = if (isRelease) "release" else "debug"
        val subFolder = if (isRelease) "releases" else "debugs"
        val destDir = rootProject.file("../apks/$subFolder")
        destDir.mkdirs()
        destDir.listFiles()
            ?.filter { it.isFile && it.extension == "apk" && it.name.startsWith("Brish_") }
            ?.forEach { it.delete() }

        val apkOutputDir = file("${layout.buildDirectory.get()}/outputs/apk/$variant")
        if (apkOutputDir.exists()) {
            apkOutputDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "apk") {
                    file.copyTo(destDir.resolve(file.name), overwrite = true)
                    logger.lifecycle("Auto-copied APK to root: apks/$subFolder/${file.name}")
                }
            }
        }
        
        val bundleOutputDir = file("${layout.buildDirectory.get()}/outputs/bundle")
        if (bundleOutputDir.exists()) {
            bundleOutputDir.walkBottomUp().forEach { file ->
                if (file.isFile && file.extension == "aab") {
                    val isRelease = file.absolutePath.contains("release", ignoreCase = true)
                    val isDebug = file.absolutePath.contains("debug", ignoreCase = true)
                    
                    val subFolder = when {
                        isRelease -> "releases"
                        isDebug -> "debugs"
                        else -> "others"
                    }
                    
                    val destDir = rootProject.file("../apks/bundles/$subFolder")
                    destDir.mkdirs()
                    file.copyTo(rootProject.file("../apks/bundles/$subFolder/${file.name}"), overwrite = true)
                    logger.lifecycle("Auto-copied Bundle to root: apks/bundles/$subFolder/${file.name}")
                }
            }
        }
    }
}
