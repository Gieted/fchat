plugins {
    java
}

allprojects {
    group = "pl.pawelkielb.fchat"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
