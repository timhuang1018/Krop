plugins{
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    id("com.timhuang.gradleplu")
}

android {
    defaultConfig {
        applicationId = "com.timhuang.cropimagetest"
    }
}

dependencies {

    implementation(project(":cropper"))
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
}
