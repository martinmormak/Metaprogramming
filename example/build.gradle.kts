plugins {
    id("application")
}

group = "sk.tuke.meta"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":persistence"))
    annotationProcessor(project(":processor"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "sk.tuke.meta.example.Main"
}

tasks.test {
    useJUnitPlatform()
}
