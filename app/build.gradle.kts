import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release builds are signed with an *upload key* that is kept OUTSIDE this project folder, so the
// key and its passwords can never be committed to git or end up in a ZIP of the project.
// The properties file holds storeFile, storePassword, keyAlias and keyPassword. It defaults to
// ~/.android-keystores/ab-it-multimedia/keystore.properties; another file can be passed with
// -Pabit.signing.properties=/path/to/keystore.properties. Without it, release builds are unsigned.
val signingPropertiesFile: File = providers.gradleProperty("abit.signing.properties")
    .map { File(it) }
    .getOrElse(File(System.getProperty("user.home"), ".android-keystores/ab-it-multimedia/keystore.properties"))
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) signingPropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.abit.multimedia"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.abit.multimedia"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (signingProperties.isNotEmpty()) {
            create("upload") {
                storeFile = File(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // null (unsigned) when the upload key's properties file is missing
            signingConfig = signingConfigs.findByName("upload")
            // R8 shrinks and optimizes the whole app. (The template also limited optimization to
            // packageScope = androidx/kotlin/kotlinx; that produced an IllegalAccessError crash in
            // the release build when sending a custom broadcast message, so it was removed.)
            optimization {
                enable = true
            }
        }
    }
    // Espresso runs against the debug build by default. Run them against the R8-optimized
    // release build with: ./gradlew connectedReleaseAndroidTest -PtestBuildType=release
    testBuildType = providers.gradleProperty("testBuildType").getOrElse("debug")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // Generates a type-safe binding class for every XML layout (e.g. FragmentAudioBinding).
        viewBinding = true
        // BuildConfig.VERSION_NAME is shown on the About screen.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)

    // Jetpack architecture components: ViewModel, LiveData and lifecycle-aware coroutines
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation Component (NavHostFragment, NavigationUI for the drawer and toolbar)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Coil loads the Image Scale photo from the internet (OkHttp is its network layer)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
}
