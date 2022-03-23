plugins {
    kotlin("jvm") version "1.6.10" apply false
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

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

project.extra["kotestVersion"] = "5.2.1"
project.extra["mockkVersion"] = "1.12.3"
