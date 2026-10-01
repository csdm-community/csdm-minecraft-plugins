plugins { java }
description = "Parkour por sesiones con recuperación persistente del inventario"
dependencies { testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}") }
