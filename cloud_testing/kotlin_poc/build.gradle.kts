import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines

group = "kotlin_poc"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath(Plugins.KOTLIN)
    }
}

plugins {
    application
    jacoco
    kotlin("jvm") version Versions.KOTLIN
}

// http://www.eclemma.org/jacoco/
jacoco {
    toolVersion = "0.8.0"
}

// Html report created in kotlin_poc/build/reports/jacoco/test/html/index.html
// ./gradlew clean check jacocoTestReport
tasks.withType<JacocoReport> {
    reports {
        html.isEnabled = true
    }
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

apply {
    plugin("kotlin")
}

application {
    mainClassName = "main"
}

repositories {
    maven(url = "http://dl.bintray.com/kotlin/ktor")
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
    jcenter()
}

// Output full test results to console
// Avoids having to read the HTML report
tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

dependencies {
    compile(Libs.KOTLIN_STD_LIB)
    compile(Libs.KOTLIN_COROUTINES_CORE)

    // https://github.com/remkop/picocli
    compile("info.picocli:picocli:2.2.1")

    compile("com.google.code.gson:gson:2.8.2")
    compile("com.googlecode.plist:dd-plist:1.20")

    // https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.google.cloud%22%20AND%20a%3A%22google-cloud-storage%22
    compile("com.google.cloud:google-cloud-storage:1.31.0")

    // https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.google.apis%22%20AND%20a%3A%22google-api-services-toolresults%22
    compile("com.google.apis:google-api-services-toolresults:v1beta3-rev374-1.23.0")

    // https://github.com/linkedin/dex-test-parser/releases
    compile("com.linkedin.dextestparser:parser:1.1.0")

    // NOTE: iOS support isn't in the public artifact. Use testing jar generated from the private gcloud CLI json
    // https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.google.apis%22%20AND%20a%3A%22google-api-services-testing%22
    // compile("com.google.apis:google-api-services-testing:v1-rev30-1.23.0")
    compile(project(":testing"))

    // yaml config
    compile("com.fasterxml.jackson.core:jackson-databind:${Versions.JACKSON}")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.JACKSON}")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.JACKSON}")

    testImplementation("org.jsoup:jsoup:1.11.3")
    testImplementation(Libs.JUNIT)
    testImplementation("com.google.truth:truth:0.40")
    // http://stefanbirkner.github.io/system-rules/index.html
    testImplementation("com.github.stefanbirkner:system-rules:1.17.1")

    // https://bintray.com/kotlin/ktor/ktor
    val ktorVersion = "0.9.3"
    testImplementation("io.ktor:ktor-server-core:$ktorVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-gson:$ktorVersion")

    // https://www.slf4j.org/download.html
    val slf4jVersion = "1.8.0-beta1"
    testImplementation("org.slf4j:slf4j-log4j12:$slf4jVersion")
    testImplementation("org.slf4j:jul-to-slf4j:$slf4jVersion")

    // todo: move to testImplementation once dagger is implemented https://github.com/TestArmada/flank/issues/248
    compile("com.google.cloud:google-cloud-nio:0.30.0-alpha")
}

// Fix Exception in thread "main" java.lang.NoSuchMethodError: com.google.common.hash.Hashing.crc32c()Lcom/google/common/hash/HashFunction;
// https://stackoverflow.com/a/45286710
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:23.6-jre")
        force(Libs.KOTLIN_REFLECT)
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
}

val javaVersion = "1.8"
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = javaVersion
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = javaVersion
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

task("fatJar", type = Jar::class) {
    baseName = "${project.name}-all"
    manifest {
        attributes.apply {
            put("Main-Class", "ftl.Main")
        }
    }
    from(configurations.runtime.map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks["jar"] as CopySpec)
}
