plugins {
    id("xplayer.android.library")
    id("xplayer.android.hilt")
}

android {
    namespace = "com.daljeet.xplayer.core.media"
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:database"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.github.anilbeesetti.nextlib.mediainfo)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
