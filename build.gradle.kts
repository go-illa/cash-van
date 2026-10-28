import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.illa.cashvan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.illa.cashvan"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://dsbs-api.illa.blue/api/v2/\"")
        // Roles to assume when the login response has no `roles` field yet, e.g. -PmockRoles=sales,fulfillment.
        // Staging only; production always falls back to fulfillment.
        buildConfigField("String", "MOCK_ROLES", "\"\"")
        // Google Maps SDK key for the Sales coverage map: -PmapsApiKey=... or MAPS_API_KEY in local.properties.
        manifestPlaceholders["MAPS_API_KEY"] = (project.findProperty("mapsApiKey") as String?)
            ?: gradleLocalProperties(rootDir, providers).getProperty("MAPS_API_KEY")
            ?: ""
    }

    flavorDimensions += "environment"

    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "BASE_URL", "\"https://dsbs-api-staging.illa.blue/api/\"")
            // Same Supabase project as the staging sales PWA (sales-agent-pwa.pages.dev).
            buildConfigField("String", "SUPABASE_URL", "\"https://hlctyyyhrdiredcsfioy.supabase.co\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhsY3R5eXlocmRpcmVkY3NmaW95Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQxNTQwMTgsImV4cCI6MjA3OTczMDAxOH0.6b4vaz-Cu-Z8f4CLPpGzgLLQ3lJEY5571p-1YzqcERE\"")
            buildConfigField("String", "MIXPANEL_TOKEN", "\"6d02d1dca65e73bd04392f0febacbdb9\"")
            // QA traffic stays out of the sales dashboards.
            buildConfigField("String", "SALES_MIXPANEL_TOKEN", "\"6d02d1dca65e73bd04392f0febacbdb9\"")
            buildConfigField("String", "MOCK_ROLES", "\"${project.findProperty("mockRoles") ?: ""}\"")
        }

        create("production") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://dsbs-api.illa.blue/api/\"")
            buildConfigField("String", "SUPABASE_URL", "\"https://hlzgbivgdcesiaymbijn.supabase.co\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhsemdiaXZnZGNlc2lheW1iaWpuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4NzY4MDksImV4cCI6MjA2OTQ1MjgwOX0.Cf79tTPSeSsvQT5rTuxiuUJ5b_yl2TD2MHxLzm8xedE\"")
            buildConfigField("String", "MIXPANEL_TOKEN", "\"09e23c835bbc75a1eac63a9fa79e533d\"")
            // The sales PWA's project (EU), so its dashboards and funnels keep working during and after the pilot.
            buildConfigField("String", "SALES_MIXPANEL_TOKEN", "\"29f750dd53a77db594f0fb15179b3d01\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }

        release {
            isDebuggable = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time on minSdk 24
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Coverage map
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    // Koin DI
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")
    implementation("io.insert-koin:koin-androidx-navigation:3.5.3")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Ktor Client
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-client-auth:2.3.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")

    // Security for encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Permissions handling
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase services
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // MixPanel Analytics
    implementation("com.mixpanel.android:mixpanel-android:7.5.2")

    // Timber Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Chucker - HTTP Inspector (staging only)
    "stagingImplementation"("com.github.chuckerteam.chucker:library:4.0.0")
    "productionImplementation"("com.github.chuckerteam.chucker:library-no-op:4.0.0")

    // Honeywell Printer SDK - Multiple Command Languages
    implementation(files("libs/printer_library.jar"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}