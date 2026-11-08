import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
  if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { load(it) }
  }
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
  if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { load(it) }
  }
}

val propVersionCode = versionProps.getProperty("VERSION_CODE")?.toIntOrNull() ?: 3
val envVersionCode =
  (project.findProperty("versionCode") as? String)?.toIntOrNull()
    ?: System.getenv("APP_VERSION_CODE")?.toIntOrNull()

val resolvedVersionCode: Int =
  if (envVersionCode != null) maxOf(envVersionCode, propVersionCode) else propVersionCode

val resolvedVersionName: String =
  (project.findProperty("versionName") as? String)?.takeIf { it.isNotBlank() }
    ?: System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() }
    ?: run {
      val major = versionProps.getProperty("VERSION_MAJOR") ?: "0"
      val minor = versionProps.getProperty("VERSION_MINOR") ?: "3"
      val patch = versionProps.getProperty("VERSION_PATCH") ?: "0"
      "$major.$minor.$patch"
    }

android {
  namespace = "com.managesoundmode.app"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.managesoundmode.app"
    minSdk = 29
    targetSdk = 36
    versionCode = resolvedVersionCode
    versionName = resolvedVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
    }
    create("release") {
      val configuredStorePath =
        keystoreProps.getProperty("storeFile")
          ?: System.getenv("RELEASE_STORE_FILE")
          ?: "keystore/manage-sound-release.jks"
      val keystoreFile = file(configuredStorePath)
      val resolvedStorePassword =
        keystoreProps.getProperty("storePassword")
          ?: System.getenv("RELEASE_STORE_PASSWORD")
      val resolvedKeyAlias =
        keystoreProps.getProperty("keyAlias")
          ?: System.getenv("RELEASE_KEY_ALIAS")
      val resolvedKeyPassword =
        keystoreProps.getProperty("keyPassword")
          ?: System.getenv("RELEASE_KEY_PASSWORD")

      if (keystoreFile.exists() && !resolvedStorePassword.isNullOrBlank() && !resolvedKeyAlias.isNullOrBlank() && !resolvedKeyPassword.isNullOrBlank()) {
        storeFile = keystoreFile
        storePassword = resolvedStorePassword
        keyAlias = resolvedKeyAlias
        keyPassword = resolvedKeyPassword
      } else {
        initWith(getByName("debug"))
      }
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      ndk {
        debugSymbolLevel = "SYMBOL_TABLE"
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android & Splashscreen
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Lifecycle & ViewModel for Compose
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Jetpack Compose & Material 3
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Jetpack Navigation 3 & Serialization
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.kotlinx.serialization.json)

  // Data Layer & Background Work (DataStore + WorkManager + Coroutines + Glance Widgets)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.kotlinx.coroutines.android)

  // Unit Testing
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)

  // Instrumented UI Testing
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
