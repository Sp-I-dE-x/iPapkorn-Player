plugins {
    id("xplayer.android.library")
    id("xplayer.android.hilt")
}

android {
    namespace = "com.daljeet.xplayer.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.github.albfernandez.juniversalchardet)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
