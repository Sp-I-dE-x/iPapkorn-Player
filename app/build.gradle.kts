plugins {
    id("xplayer.android.application")
    id("xplayer.android.application.compose")
    id("xplayer.android.hilt")
}

android {
    namespace = "com.daljeet.xplayer"

    defaultConfig {
        applicationId = "com.daljeet.xplayer"
        versionCode = 22
        versionName = "0.10.8"
        targetSdk = 34
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":feature:videopicker"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))
    implementation(libs.play.services.ads)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.material)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.accompanist.permissions)

    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.timber)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose.v277)


    implementation("androidx.lifecycle:lifecycle-runtime:2.3.1")
    implementation("androidx.lifecycle:lifecycle-compiler:2.3.1")


    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
