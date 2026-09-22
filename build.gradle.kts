plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.example"
version = "1.0.3"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://mvn.wesjd.net/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
    implementation("net.wesjd:anvilgui:1.10.13-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("net.wesjd.anvilgui", "com.example.staffmode.anvilgui")
    }

    build {
        dependsOn(shadowJar)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
