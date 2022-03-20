plugins {
    java
    application
}

application {
    mainClass.set("pl.pawelkielb.fchat.client.Main")
}

repositories {
}

dependencies {
    implementation(project(":shared"))
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
