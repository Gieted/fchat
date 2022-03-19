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

    copy {
        from(zipTree("../jdks/openjdk-17.0.2_windows-x64_bin.zip"))
        into("build/tmp")
    }

    doLast {
        file("build/tmp/jdk-17.0.2").renameTo(file("build/install/client/jre"))

        val bin = "build/install/client/bin"
        file("$bin/client").renameTo(file("$bin/fchat"))
        file("$bin/client.bat").renameTo(file("$bin/fchat.bat"))
        val scriptBat = file("$bin/fchat.bat").readText().split("\n").toMutableList()
        scriptBat.add(30, """set JAVA_HOME=%~dp0..\jre""")
        file("$bin/fchat.bat").writeText(scriptBat.joinToString("\n"))
    }
}
