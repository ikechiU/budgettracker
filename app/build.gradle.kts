plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.iykeafrica.budgettracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.iykeafrica.budgettracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "APPS_SCRIPT_URL", "\"https://script.google.com/macros/s/AKfycby0ZKk-vx9IILHx47ZxUpMc5TanIAdvQ2UPHK29qEuMR8GAJrIG3EWfW51FT8lBGs5CVw/exec\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room (local database)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Retrofit + Gson (HTTP calls to Google Sheets)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp logging interceptor (for debugging network calls)
    implementation(libs.logging.interceptor)

    // ViewModel + LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // ExecutorService helper (runs background work in Java)
    implementation(libs.lifecycle.runtime)

    implementation(libs.recyclerview)
    implementation(libs.cardview)
}