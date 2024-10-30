import proguard.gradle.ProGuardTask

plugins {
    id("java")
    kotlin("jvm")
}

group = "com.pks.p2p"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.guardsquare:proguard-gradle:7.6.0")
}


buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.0")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn.add(tasks.getByName("proguard"))
}

tasks.register("proguard", ProGuardTask::class) {
    group = "build"
    configuration(File("proguard.pro"))
}

tasks.jar {
    archiveVersion = ""
    manifest {
        attributes["Main-Class"] = "com.pks.p2p.Main"
    }
}

kotlin {
    jvmToolchain(22)
}