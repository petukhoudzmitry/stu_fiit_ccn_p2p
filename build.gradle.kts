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
    implementation(libs.kotlin.stdlib.jdk8)
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
    dependsOn(tasks.jar)
    outputs.upToDateWhen { false }
}

tasks.jar {
    archiveVersion = ""
    manifest {
        attributes["Main-Class"] = "com.pks.p2p.Main"
    }
    finalizedBy("proguard")
}

kotlin {
    jvmToolchain(22)
}