import org.gradle.api.Plugin
import org.gradle.api.Project

open class GradlePlu : Plugin<Project>{
    override fun apply(target: Project) {
        target.configAndroid()
        target.configureDependencies()
    }
}

internal fun Project.configurePlugins(){
    plugins.apply("com.android.library")
}