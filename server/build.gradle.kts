plugins {
    java
    application
}

application {
    mainClass.set("pl.pawelkielb.fchat.server.Main")
}

repositories {
}

dependencies {
    implementation(project(":shared"))
}
