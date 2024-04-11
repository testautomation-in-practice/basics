import org.gradle.api.file.DuplicatesStrategy.INCLUDE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.1"
    id("io.spring.dependency-management") version "1.0.12.RELEASE"
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.spring") version "1.8.20"
    kotlin("kapt") version "1.8.20"
}

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("io.arrow-kt:arrow-core:1.2.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("com.amazonaws:aws-java-sdk-s3:1.12.272")
    implementation("com.amazonaws:aws-java-sdk-sts:1.12.272")
    implementation("org.testcontainers:localstack")
    implementation("org.testcontainers:mongodb")

    testImplementation("io.github.logrecorder:logrecorder-junit5:2.9.0")
    testImplementation("io.github.logrecorder:logrecorder-logback:2.9.0")
    testImplementation("io.github.logrecorder:logrecorder-assertions:2.9.0")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.ninja-squad:springmockk:3.1.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions:4.5.1") // sadly not managed by Spring
    testImplementation("org.testcontainers:junit-jupiter:1.18.1")
    testImplementation("org.testcontainers:mongodb:1.18.1")

    kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks {

    withType<Copy> { duplicatesStrategy = INCLUDE }
    withType<Jar> { duplicatesStrategy = INCLUDE }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
            javaParameters = true
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

}
