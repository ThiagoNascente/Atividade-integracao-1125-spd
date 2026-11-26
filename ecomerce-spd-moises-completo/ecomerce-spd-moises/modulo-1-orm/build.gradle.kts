plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common-events"))
    implementation("org.jetbrains.exposed:exposed-core:0.57.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.57.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("io.javalin:javalin:6.3.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("br.ufg.inf.orm.OrmAppKt")
}

tasks.test {
    useJUnitPlatform()
}
