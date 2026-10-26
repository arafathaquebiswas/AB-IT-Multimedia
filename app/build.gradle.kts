plugins {
    alias(libs.plugins.android.application)
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

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // Generates a type-safe binding class for every XML layout (e.g. FragmentAudioBinding).
        viewBinding = true
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
