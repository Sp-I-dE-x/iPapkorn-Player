plugins {
    id("xplayer.android.library")
    id("xplayer.android.hilt")
}

android {
    namespace = "com.daljeet.xplayer.feature.player"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewModel.ktx)
    implementation(libs.google.android.material)
    implementation(libs.androidx.constraintlayout)

    // Media3
    implementation(libs.bundles.media3)
    implementation(libs.github.anilbeesetti.nextlib.media3ext)
    implementation(libs.github.anilbeesetti.nextlib.mediainfo)

    implementation(libs.timber)
    implementation(libs.play.services.ads.lite)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
