// import org.apache.commons.io.output.ByteArrayOutputStream
// import java.nio.charset.Charset

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"

    `maven-publish`
}

group = "me.senseiwells"
version = "1.0.0"

kotlin.explicitApi()

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.coroutines)
    implementation(libs.json)

    api(libs.ktor.core)
    api(libs.ktor.cio)

    implementation(libs.cache4k)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "17"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.senseiwells"
            artifactId = "mojank"
            version = project.version.toString()

            from(components["java"])
            artifact(tasks.kotlinSourcesJar) {
                classifier = "sources"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// fun getGitHash(): String {
//     val out = ByteArrayOutputStream()
//     exec {
//         commandLine("git", "rev-parse", "HEAD")
//         standardOutput = out
//     }
//     return out.toString(Charset.defaultCharset()).trim()
// }