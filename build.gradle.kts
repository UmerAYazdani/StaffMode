plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.example"
version = "1.0.4"

repositories {
    mavenCentral()

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://mvn.wesjd.net/")
    }
}

dependencies {

    // Paper 26.3 API
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")

    // Anvil GUI used by the punishment system
    implementation("net.wesjd:anvilgui:1.10.13-SNAPSHOT")
}

tasks {

    shadowJar {

        archiveClassifier.set("")

        relocate(
            "net.wesjd.anvilgui",
            "com.example.staffmode.anvilgui"
        )
    }

    build {
        dependsOn(shadowJar)
    }
}

java {

    // Paper 26.3 requires the newer Java toolchain.
    toolchain.languageVersion.set(
        JavaLanguageVersion.of(25)
    )
}