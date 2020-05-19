import com.android.build.gradle.api.AndroidBasePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

const val jUnit = "junit:junit:4.12"
//const val androidTestRunner = ""
val fileTree = (mapOf("dir" to "libs", "include" to listOf("*.jar")))

const val kotlinStd = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.72"
const val appCompat = "androidx.appcompat:appcompat:1.1.0"
const val coreX = "androidx.core:core-ktx:1.2.0"
const val extJUnit = "androidx.test.ext:junit:1.1.0"
const val espresso = "androidx.test.espresso:espresso-core:3.2.0"


internal fun Project.configureDependencies() = dependencies {
    add("testImplementation",jUnit)
    add("implementation",fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    add("implementation",kotlinStd)
    add("implementation",appCompat)
    add("implementation",coreX)

    if (project.containsAndroidPlugin()){
        add("androidTestImplementation",extJUnit)
        add("androidTestImplementation",espresso)
    }
}

internal fun Project.containsAndroidPlugin() :Boolean{
    return project.plugins.toList().any { plugin-> plugin is AndroidBasePlugin }
}
