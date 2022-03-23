plugins {
    kotlin("jvm")
}

repositories {
}

dependencies {
    val kotestVersion: String by rootProject
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")

    val mockkVersion: String by rootProject
    testImplementation("io.mockk:mockk:$mockkVersion")
}
