plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("pl.pawelkielb.fchat.server.Main")
}

repositories {
}

dependencies {
    implementation(project(":shared"))

    val kotestVersion: String by rootProject
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}
