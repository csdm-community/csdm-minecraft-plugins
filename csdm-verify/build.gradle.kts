plugins {
    java
}

description = "Terminal de verificacion de identidad Minecraft para CSDM"

dependencies {
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
}

// Java 25: load Mockito at JVM startup instead of relying on dynamic self-attachment.
val mockitoAgent = configurations.create("mockitoAgent")
dependencies { mockitoAgent("org.mockito:mockito-core:5.20.0") { isTransitive = false } }
tasks.test { jvmArgs("-javaagent:${mockitoAgent.asPath}") }

repositories { maven { url = uri("https://repo.opencollab.dev/main/") } }
dependencies { compileOnly("org.geysermc.floodgate:api:2.2.5-20260917.145236-21") }
