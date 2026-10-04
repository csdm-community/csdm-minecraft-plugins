plugins { java }
description = "Escala visual por observador en el lobby"
repositories { maven { url = uri("https://repo.codemc.io/repository/maven-releases/") } }
dependencies { compileOnly("com.github.retrooper:packetevents-spigot:2.14.0") }

repositories { maven { url = uri("https://repo.opencollab.dev/main/") } }
dependencies { compileOnly("org.geysermc.floodgate:api:2.2.5-20260917.145236-21") }

dependencies {
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
    testImplementation("com.github.retrooper:packetevents-spigot:2.14.0")
}

// Paper supplies Adventure 5; the installed PacketEvents distribution also carries
// Adventure 4 compatibility classes omitted from its Maven API artifact. Append
// those classes after Paper's dependencies to reproduce that runtime in tests.
val packetEventsBundledAdventure by configurations.creating { isTransitive = false }
dependencies { packetEventsBundledAdventure("net.kyori:adventure-api:4.26.1") }
tasks.test { classpath += packetEventsBundledAdventure }
