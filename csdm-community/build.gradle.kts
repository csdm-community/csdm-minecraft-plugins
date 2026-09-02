plugins {
    java
}

description = "Rangos de staff y medallas de la comunidad CSDM"

dependencies {
    compileOnly("net.luckperms:api:${providers.gradleProperty("luckPermsApiVersion").get()}")
}

