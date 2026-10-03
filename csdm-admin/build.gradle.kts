plugins {
    java
}

description = "Administracion y politicas persistentes del lobby CSDM"


dependencies {
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}

// Java 25: load Mockito at JVM startup instead of relying on dynamic self-attachment.
val mockitoAgent = configurations.create("mockitoAgent")
dependencies { mockitoAgent("org.mockito:mockito-core:5.20.0") { isTransitive = false } }
tasks.test { jvmArgs("-javaagent:${mockitoAgent.asPath}") }
