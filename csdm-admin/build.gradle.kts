plugins {
    java
}

description = "Administracion y politicas persistentes del lobby CSDM"


dependencies {
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}
