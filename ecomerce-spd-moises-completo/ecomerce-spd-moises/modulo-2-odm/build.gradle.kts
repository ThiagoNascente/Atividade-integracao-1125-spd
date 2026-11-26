plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-events"))
    implementation("org.mongodb:mongodb-driver-sync:5.2.0")
    implementation("io.javalin:javalin:6.3.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("br.ufg.inf.odm.OdmServerAppKt")
}

tasks.test {
    useJUnitPlatform()
}
