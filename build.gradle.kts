plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "com.joshuacolvin"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        val props = mapOf(
            "name" to "AscendRelics",
            "version" to project.version,
            "main" to "com.joshuacolvin.ascendrelics.AscendRelics",
            "apiVersion" to "1.21"
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    reobfJar {
        outputJar.set(layout.buildDirectory.file("libs/AscendRelics-${project.version}.jar"))
    }
}
