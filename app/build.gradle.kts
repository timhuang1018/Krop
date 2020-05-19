plugins{
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    id("com.timhuang.gradleplu")
}

android {
//    compileSdkVersion 29
//    buildToolsVersion "29.0.2"
    defaultConfig {
        applicationId = "com.timhuang.cropimagetest"
//        minSdkVersion 23
//        targetSdkVersion 29
//        versionCode 1
//        versionName "1.0"
//        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
//    buildTypes {
//        release {
//            minifyEnabled false
//            proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
//        }
//    }
}

dependencies {
//    implementation fileTree(dir: "libs", include: ["*.jar"])
//    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.72"
//    implementation "androidx.appcompat:appcompat:1.1.0"
//    implementation "androidx.core:core-ktx:1.2.0"
//    testImplementation "junit:junit:4.12"
//    androidTestImplementation "androidx.test.ext:junit:1.1.0"
//    androidTestImplementation "androidx.test.espresso:espresso-core:3.2.0"
    implementation(project(":cropper"))
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
}
