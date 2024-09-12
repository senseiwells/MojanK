# MojanK

A lightweight wrapper for the Mojang API written in kotlin supporting coroutines.

## Getting Started

You can add MojanK by adding the following to your `build.gradle.kts`:

```kts
repositories {
    maven("https://maven.supersanta.me/snapshots")
}

dependencies {
    implementation("me.senseiwells:mojank:1.0.2")
}
```

## Usage

You can access the Mojang API by creating an instance of `Mojank`, where you can specify a `HttpClient` to use, if you wish to use the default you can also just access the `Mojank` companion which has a singleton instance of the class.

```kt
suspend fun example() {
    val result = Mojank.usernameToSimpleProfile("sEnSeIwElLs")
    if (result.isSuccess) {
        val profile = result.get()
        println("Username: ${profile.name}, UUID: ${profile.id}")
    } else {
        println("Failed to fetch profile for 'sEnSeIwElLs'")
    }
}
```

It's highly recommended that you cache your results from the api, you can implement that yourself or you can use the built in `CachedMojank`, where you can specify how long the results are cached for, by default 20 minutes. Similar to `Mojank` if you want to use the default instance you can access the `CachedMojank` companion. 

```kt
suspend fun example() {
    val result = CachedMojank.uuidToUsername(UUID.fromString("58c21ca7-514f-4179-b8ce-79e6d985d452"))
    if (result.isSuccess) {
        val username = result.get()
        println("Username: $username")
    } else {
        println("Failed to fetch username for '58c21ca7-514f-4179-b8ce-79e6d985d452'")
    }
}
```