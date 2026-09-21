plugins {
    java
}

description = "Terminal de verificacion de identidad Minecraft para CSDM"

dependencies {
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}
