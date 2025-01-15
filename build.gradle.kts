import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    idea
    eclipse
    kotlin("jvm") version "1.7.10"
}

apply {
    plugin("project-report")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "test"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}

dependencies {
    implementation("io.github.bonigarcia:webdrivermanager:5.7.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.18.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.6")
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("ru.yandex.qatools.htmlelements:htmlelements-matchers:1.20.0")
    implementation("org.zalando:logbook-core:1.7.3")
    implementation("org.zalando:logbook-httpclient:1.7.3")

    implementation("org.junit.platform:junit-platform-engine:1.9.0")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
}

tasks.getByName<Test>("test") {
    val junitProperties = mapOf(
        "junit.jupiter.extensions.autodetection.enabled" to "true",
        "jna.encoding" to "cp1251"
    )
    systemProperties(junitProperties)
    useJUnitPlatform { }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

}
