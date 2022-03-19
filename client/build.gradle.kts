plugins {
    java
    id("edu.sc.seis.launch4j") version "2.5.0"
}

launch4j {
    mainClassName = "pl.pawelkielb.fchat.client.Main"
    bundledJrePath = "./jre"
    bundledJre64Bit = true
    outfile = "fchat.exe"
    headerType = "console"
}

repositories {
}

dependencies {
    implementation(project(":shared"))
}

tasks.create("release") {
    group = "build"
    dependsOn("createExe")

    copy {
        from(zipTree("../jdks/openjdk-17.0.2_windows-x64_bin.zip"))
        into("build/tmp")
    }

    file("build/tmp/jdk-17.0.2").renameTo(file("build/launch4j/jre"))
}
