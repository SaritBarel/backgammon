plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.backgammon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.backgammon"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Firebase Authentication
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Firebase Firestore - for online game data
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Firebase Realtime Database - alternative to Firestore
    implementation("com.google.firebase:firebase-database-ktx")
    
    // Firebase Cloud Messaging - for notifications
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // RecyclerView - for game list
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // SwipeRefreshLayout - for refreshing game list
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}