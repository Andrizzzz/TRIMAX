plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.trimax"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.trimax"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Social Authentication SDKs - Explicit configurations
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.facebook.android:facebook-login:17.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}