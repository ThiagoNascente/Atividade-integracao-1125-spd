plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ecomerce-spd-moises"

include(
    "common-events",
    "modulo-1-orm",
    "modulo-2-odm",
    "modulo-3-integrador"
)
