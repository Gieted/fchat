import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20" apply false
}

allprojects {
    group = "pl.pawelkielb.fchat"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

project.extra["kotestVersion"] = "5.2.2"
project.extra["mockkVersion"] = "1.12.3"

tasks.register("stage") {
    group = "build"
    dependsOn(":server:build")
}
