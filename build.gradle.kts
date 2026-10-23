plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.christian34.woodcutter"
version = "3.1.1"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // WorldGuard
    maven("https://maven.enginehub.org/repo/")
    // PlaceholderAPI
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // Paper API already ships Adventure
    // compiled against the oldest supported version (1.21), so newer api can't slip in;
    // the tests run on the current one (testImplementation below)
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // optional plugins (softdepend), their classes are only used if they are installed
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("org.bstats:bstats-bukkit:3.2.1")

    // tests run the plugin against a mocked Paper server
    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation("me.clip:placeholderapi:2.11.6")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains:annotations:26.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// the same tests once more on other Minecraft versions (Paper api and MockBukkit of that version):
// the oldest supported one (1.21.x) and the newest one, so the plugin keeps working on all of them
val versionTests = mapOf(
    "test121" to listOf("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT", "org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.116.3"),
    "test263" to listOf("io.papermc.paper:paper-api:26.3.build.177-stable", "org.mockbukkit.mockbukkit:mockbukkit-v26.3:4.118.0"),
)
versionTests.forEach { (name, libraries) ->
    sourceSets.create(name) {
        java.setSrcDirs(sourceSets.test.get().java.srcDirs)
        resources.setSrcDirs(sourceSets.test.get().resources.srcDirs)
        // the plugin first, else the config.yml of PlaceholderAPI would be found as its resource
        compileClasspath = sourceSets.main.get().output + compileClasspath
        runtimeClasspath = sourceSets.main.get().output + runtimeClasspath
    }
    dependencies {
        libraries.forEach { "${name}Implementation"(it) }
        "${name}Implementation"("me.clip:placeholderapi:2.11.6")
        "${name}Implementation"(platform("org.junit:junit-bom:6.1.3"))
        "${name}Implementation"("org.junit.jupiter:junit-jupiter")
        "${name}Implementation"("org.jetbrains:annotations:26.1.0")
        "${name}RuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "${name}RuntimeOnly"("org.bstats:bstats-bukkit:3.2.1")
    }
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
    versionTests.forEach { (name, libraries) ->
        val versionTest = register<Test>(name) {
            description = "Runs the tests with ${libraries.first().substringAfterLast(':')}."
            group = "verification"
            testClassesDirs = sourceSets[name].output.classesDirs
            classpath = sourceSets[name].runtimeClasspath
            useJUnitPlatform()
            systemProperty("woodcutter.offline", "true")
            testLogging {
                events("failed", "skipped")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
        check {
            dependsOn(versionTest)
        }
        named<JavaCompile>("compile${name.replaceFirstChar { it.uppercase() }}Java") {
            options.encoding = Charsets.UTF_8.name()
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
    }
}
