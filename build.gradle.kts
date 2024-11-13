plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    `maven-publish`
}

group = "me.senseiwells"
version = "1.0.3"

kotlin.explicitApi()

repositories {
    mavenCentral()
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.senseiwells"
            artifactId = "mojank"

            from(components["java"])
            artifact(tasks.kotlinSourcesJar) {
                classifier = "sources"
            }

            updateReadme("./README.md")
        }
    }

    repositories {
        val mavenUrl = System.getenv("MAVEN_URL")
        if (mavenUrl != null) {
            maven {
                url = uri(mavenUrl)
                val mavenUsername = System.getenv("MAVEN_USERNAME")
                val mavenPassword = System.getenv("MAVEN_PASSWORD")
                if (mavenUsername != null && mavenPassword != null) {
                    credentials {
                        username = mavenUsername
                        password = mavenPassword
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

private fun MavenPublication.updateReadme(vararg readmes: String) {
    val location = "${groupId}:${artifactId}"
    val regex = Regex("""${Regex.escape(location)}:[\d\.\-a-zA-Z+]+""")
    val locationWithVersion = "${location}:${version}"
    for (path in readmes) {
        val readme = file(path)
        readme.writeText(readme.readText().replace(regex, locationWithVersion))
    }
}