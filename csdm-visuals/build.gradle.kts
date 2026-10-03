plugins { java }
description = "Escala visual por observador en el lobby"
repositories { maven { url = uri("https://repo.codemc.io/repository/maven-releases/") } }
dependencies { compileOnly("com.github.retrooper:packetevents-spigot:2.14.0") }

repositories { maven { url = uri("https://repo.opencollab.dev/main/") } }
dependencies { compileOnly("org.geysermc.floodgate:api:2.2.5-20260917.145236-21") }
