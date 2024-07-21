plugins {
    id("xplayer.android.library")
    id("xplayer.android.hilt")
}

android {
    namespace = "com.daljeet.xplayer.core.data"
}

dependencies {

    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))

    implementation(libs.timber)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
