plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("pl.pawelkielb.fchat.client.Main")
}

repositories {
}


dependencies {
    implementation(project(":shared"))

    val kotestVersion: String by rootProject
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}


tasks.create("release") {
    dependsOn("installDist")
    group = "build"

    doLast {
        val bin = "build/install/client/bin"
        file("$bin/client").renameTo(file("$bin/fchat"))
        file("$bin/client.bat").renameTo(file("$bin/fchat.bat"))
    }
}
