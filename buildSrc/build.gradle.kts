plugins{
    `kotlin-dsl`
}

repositories{
    mavenCentral()
    google()
    jcenter()
}

dependencies{
    implementation("com.android.tools.build:gradle:4.0.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")

    implementation(gradleApi())
    implementation(localGroovy())
}
//
//class GreetingPlugin : Plugin<Project> {
//    override fun apply(project: Project) {
//        project.configure()
//    }
//}
