plugins {
    java
    application
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "dev.mudkip"
version = "0.1.0"
application.mainClass = "jam.Server"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("net.minestom:minestom-snapshots:461c56e749")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("org.slf4j:slf4j-simple:2.0.14")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}