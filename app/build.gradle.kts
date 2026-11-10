// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.m8.keyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.m8.keyboard"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Bundle only dictionaries/en.txt (not the raw download) as an asset.
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.get().asFile.resolve("generated/dictionary-assets"))
}

kotlin {
    jvmToolchain(17)
}

val copyDictionaries = tasks.register<Copy>("copyDictionaries") {
    from(rootProject.layout.projectDirectory.dir("dictionaries")) {
        include("en.txt")
    }
    into(layout.buildDirectory.get().asFile.resolve("generated/dictionary-assets/dictionaries"))
}

tasks.named("preBuild") {
    dependsOn(copyDictionaries)
}

dependencies {
    implementation(project(":"))
}
