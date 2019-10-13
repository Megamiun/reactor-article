import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
}

group = "br.com.gabryel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.projectreactor:reactor-core:3.3.0.RELEASE")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}