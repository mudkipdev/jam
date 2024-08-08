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
    implementation("com.github.TogAr2:MinestomPvP:7b5cadcad4")
    implementation("dev.hollowcube:polar:1.11.1")
    implementation("org.slf4j:slf4j-simple:2.0.14")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}