plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.lagradost.cloudstream3.gradle")
}

version = "1"

cloudstream {
    setRepo("https://github.com/KorhanWithSunglasses/AethelionCS")
}

android {
    namespace = "com.aethelioncs.dizibox"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xskip-metadata-version-check"
        )
    }
}

dependencies {
    compileOnly("com.github.recloudstream:cloudstream:master-SNAPSHOT")
    compileOnly("com.github.Blatzar:NiceHttp:0.4.11")
    compileOnly("org.jsoup:jsoup:1.18.3")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jsoup:jsoup:1.18.3")
}
