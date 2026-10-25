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
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // OpenStreetMap Android Library (OSMDroid)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Social Authentication SDKs - Explicit configurations
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.facebook.android:facebook-login:17.0.0")

    testImplementation(libs.junit)
    // Real org.json for local unit tests (android.jar only has stubs); the app uses Android's built-in copy.
    testImplementation("org.json:json:20240303")
    // Local HTTP server for SupabaseClientTest (the JDK's com.sun.net.httpserver is not on Android's
    // unit-test compile classpath). Test-only: not packaged in the app.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}