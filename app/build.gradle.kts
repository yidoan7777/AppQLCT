plugins {
    alias(libs.plugins.android.application)
    // Plugin Google Services - cần file google-services.json trong thư mục app/
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.appqlct"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.appqlct"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Hỗ trợ các kiến trúc cho BlueStacks (x86, x86_64) và thiết bị thật (arm64-v8a, armeabi-v7a)
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Ký APK release bằng debug keystore để có thể cài trên BlueStacks
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.cardview)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    
    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    
    // Lifecycle components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // ViewPager2
    implementation(libs.viewpager2)
    
    // Third-party libraries
    implementation(libs.mpandroidchart)
    implementation(libs.circleimageview)
    implementation(libs.picasso)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Social Login
    implementation(libs.googleSignIn)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}