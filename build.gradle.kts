plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "me.zhang132212"
version = "3.3.14"

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

repositories { mavenCentral() }

tasks.test { useJUnitPlatform() }

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}
