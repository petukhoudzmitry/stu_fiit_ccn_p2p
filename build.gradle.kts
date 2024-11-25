plugins {
    alias(libs.plugins.java)
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
}

group = "com.pks.p2p"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.org.junit.junit.bom))
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    implementation(libs.kotlin.stdlib)
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.com.guardsquare.proguard.gradle)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("proguard", proguard.gradle.ProGuardTask::class) {
    description = "Runs ProGuard to obfuscate and optimize the JAR file"
    configuration("proguard.pro")
    dependsOn(tasks.build)
    outputs.upToDateWhen { false }
}

tasks.jar {
    archiveVersion = ""
    manifest {
        attributes["Main-Class"] = "com.pks.p2p.Main"
    }
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
}

tasks.build {
    finalizedBy("proguard")
}

kotlin {
    jvmToolchain(22)
}