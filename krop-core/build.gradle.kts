import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.mavenPublish)
    id("maven-publish")
}
//for publishToMavenLocal, can check ~/.m2 to find build artifacts
group = "io.keeppro"
version = "1.0.2"

kotlin {
    js(IR) {
        moduleName = "Krop"
        browser {
            commonWebpackConfig {
                outputFileName = "krop.js"
            }
        }
    }
    
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Krop"
            isStatic = true
        }
    }


    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid"){
                withIos()
                withJvm()
                withJs()
            }
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.coil)
        }
        desktopMain.dependencies {
        }
        iosMain.dependencies {
        }

        jsMain.dependencies {
        }
    }
}

android {
    namespace = "io.keeppro.krop"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {

    // Define coordinates for the published artifact
    coordinates(
        groupId = "io.keeppro",
        artifactId = "krop",
        version = "1.0.2"
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("KMP Library for Cropping Images (Krop)")
        description.set("Kotlin multiplatform library for cropping images by building on top of Coil. Can be used on Android, iOS, web, and desktop platforms. Easy to use and customize for image cropping in Compose Multiplatform applications.")
        inceptionYear.set("2024")
        url.set("https://github.com/timhuang1018/Krop")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("timhuang")
                name.set("Tim Huang")
                email.set("t8522192@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/timhuang1018/Krop")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}