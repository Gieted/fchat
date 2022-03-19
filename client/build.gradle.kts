plugins {
    java
    id("edu.sc.seis.launch4j") version "2.5.0"
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


tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

launch4j {
    mainClassName = "pl.pawelkielb.fchat.client.Main"
    bundledJrePath = "./jre"
    bundledJre64Bit = true
    outfile = "fchat.exe"
    headerType = "console"
    chdir = ""
    jarTask = tasks["uberJar"]
}
