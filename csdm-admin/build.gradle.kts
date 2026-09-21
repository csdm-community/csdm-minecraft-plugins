plugins {
    java
}

description = "Administracion y politicas persistentes del lobby CSDM"


dependencies {
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}
