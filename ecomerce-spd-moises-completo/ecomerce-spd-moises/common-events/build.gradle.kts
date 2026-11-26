plugins {
    kotlin("jvm")
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // 'api': quem depende de common-events enxerga Lettuce, Gson e SLF4J/Logback
    api("io.lettuce:lettuce-core:6.7.1.RELEASE")
    api("com.google.code.gson:gson:2.11.0")
    api("org.slf4j:slf4j-api:2.0.12")
    api("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
