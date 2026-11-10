// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.21"
    id("com.android.application") version "8.7.3" apply false
    kotlin("android") version "2.0.21" apply false
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
