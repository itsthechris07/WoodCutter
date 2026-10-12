plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.christian34.woodcutter"
version = "3.0.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API already ships Adventure
    // compiled against the oldest supported version (1.21), so newer api can't slip in;
    // the tests run on the current one (testImplementation below)
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("com.tchristofferson:ConfigUpdater:2.2")

    // tests run the plugin against a mocked Paper server
    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains:annotations:26.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    processResources {
        // read at configuration time, Task.project is not allowed while the task runs (Gradle 10)
        val pluginVersion = project.version.toString()
        inputs.property("version", pluginVersion)
        filesMatching("plugin.yml") {
            expand("version" to pluginVersion)
        }
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        // 1.21.x servers run on Java 21
        options.release = 21
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
    }
    compileTestJava {
        options.encoding = Charsets.UTF_8.name()
    }

    build {
        dependsOn(shadowJar)
    }
    // the plugin is only usable with its libraries, the plain jar would just be mistaken for it
    jar {
        enabled = false
    }

    test {
        useJUnitPlatform()
        // no bStats or update check from tests
        systemProperty("woodcutter.offline", "true")
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    // copies the plugin into the update folder of a local test server (swapped in on its next start);
    // set serverPluginsDir in the (git-ignored) gradle.properties to enable
    providers.gradleProperty("serverPluginsDir").orNull?.let { pluginsDir ->
        val deployPlugin = register<Copy>("deployPlugin") {
            from(shadowJar)
            into(file(pluginsDir).resolve("update"))
        }
        build {
            finalizedBy(deployPlugin)
        }
    }
    shadowJar {
        archiveClassifier = ""
        exclude("META-INF/maven/**")
        // release jars are kept outside of build/, so a clean does not remove them
        destinationDirectory = layout.projectDirectory.dir("release")
        // bStats refuses to start unless relocated
        relocate("org.bstats", "com.christian34.woodcutter.libs.bstats")
        relocate("com.tchristofferson", "com.christian34.woodcutter.libs.configupdater")
    }
}
