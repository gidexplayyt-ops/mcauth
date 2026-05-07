import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
}

allprojects {
    group = "com.gidexplayyt.mcauth"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.velocitypowered.com/releases/")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
}

project(":core") {
    dependencies {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        implementation("commons-codec:commons-codec:1.16.0")
    }
}

project(":paper") {
    dependencies {
        implementation(project(":core"))
        compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    }

    tasks.named<Jar>("jar") {
        archiveBaseName.set("McAuth-Paper")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project(":core").extensions.getByType<SourceSetContainer>().named("main").get().output)
        from({
            configurations.compileClasspath.get().filter { it.path.contains("jackson") || it.path.contains("commons-codec") }.map { zipTree(it) }
        })
    }
}

project(":velocity") {
    dependencies {
        implementation(project(":core"))
        compileOnly("com.velocitypowered:velocity-api:3.1.1")
    }

    tasks.named<Jar>("jar") {
        archiveBaseName.set("McAuth-Velocity")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project(":core").extensions.getByType<SourceSetContainer>().named("main").get().output)
        from({
            configurations.runtimeClasspath.get().filter { it.path.contains("jackson") || it.path.contains("commons-codec") }.map { zipTree(it) }
        })
    }

    tasks.named("compileJava") {
        dependsOn(":core:classes")
    }
}

project(":bungee") {
    dependencies {
        implementation(project(":core"))
        compileOnly("net.md-5:bungeecord-api:1.20-R0.2")
    }

    tasks.named<Jar>("jar") {
        archiveBaseName.set("McAuth-Bungee")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project(":core").extensions.getByType<SourceSetContainer>().named("main").get().output)
        from({
            configurations.runtimeClasspath.get().filter { it.path.contains("jackson") || it.path.contains("commons-codec") }.map { zipTree(it) }
        })
    }

    tasks.named("compileJava") {
        dependsOn(":core:classes")
    }
}

tasks.register("buildPlugins") {
    dependsOn(":paper:jar", ":velocity:jar", ":bungee:jar")
}

tasks.register<Copy>("assemblePlugins") {
    dependsOn("buildPlugins")
    from("paper/build/libs/McAuth-Paper-${project.version}.jar")
    from("velocity/build/libs/McAuth-Velocity-${project.version}.jar")
    from("bungee/build/libs/McAuth-Bungee-${project.version}.jar")
    into(layout.projectDirectory.dir("dist"))
}
