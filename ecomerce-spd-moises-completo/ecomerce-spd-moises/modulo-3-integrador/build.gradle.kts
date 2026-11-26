plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-events"))

    implementation("org.apache.camel:camel-core:4.8.0")
    implementation("org.apache.camel:camel-direct:4.8.0")
    implementation("org.apache.camel:camel-http:4.8.0")

    implementation("org.jetbrains.exposed:exposed-core:0.57.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.57.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")

    testImplementation(kotlin("test"))
    // Servidores HTTP falsos (ODM e ORM) usados no teste da rota Camel
    testImplementation("io.javalin:javalin:6.3.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("br.ufg.inf.integrador.IntegradorAppKt")
}

tasks.test {
    useJUnitPlatform()
}
