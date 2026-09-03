plugins {
    java
}

description = "Terminal de verificacion de identidad Minecraft para CSDM"

dependencies {
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}
