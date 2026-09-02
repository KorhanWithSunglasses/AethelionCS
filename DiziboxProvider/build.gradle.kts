version = "7"

cloudstream {
    setRepo("https://github.com/KorhanWithSunglasses/AethelionCS")
}

android {
    namespace = "com.aethelioncs.dizibox"
}

dependencies {
    compileOnly("com.github.recloudstream:cloudstream:master-SNAPSHOT")
    compileOnly("com.github.Blatzar:NiceHttp:0.4.11")
    compileOnly("org.jsoup:jsoup:1.18.3")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jsoup:jsoup:1.18.3")
}
