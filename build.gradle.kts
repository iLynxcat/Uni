import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import io.papermc.paperweight.tasks.CreateBundlerJar
import io.papermc.paperweight.tasks.CreatePaperclipJar
import org.gradle.kotlin.dsl.named

plugins {
    java
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperweight.patcher)
}

paperweight {
    upstreams.paper {
        ref = providers.gradleProperty("paperRef")

        patchFile {
            path = "paper-server/build.gradle.kts"
            outputFile = file("unisrc-server/build.gradle.kts")
            patchFile = file("unisrc-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "paper-api/build.gradle.kts"
            outputFile = file("unisrc-api/build.gradle.kts")
            patchFile = file("unisrc-api/build.gradle.kts.patch")
        }

        patchDir("paperApi") {
            upstreamPath = "paper-api"
            excludes = setOf("build.gradle.kts")
            patchesDir = file("unisrc-api/paper-patches")
            outputDir = file("paper-api")
        }
    }
}

val unisrc = subprojects.filter { it.path.startsWith(":unisrc-") }

configure(unisrc) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get())
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = rootProject.libs.versions.java.get().toInt()
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }
}

val plugins = subprojects.filter { it.path.startsWith(":plugins:") }
val libraries = subprojects.filter { it.path.startsWith(":libs:") }

configure(plugins + libraries) {
    apply(plugin = "java")

    group = "net.computermeowse.uni"
    version = "0.0.0"

    java {
        toolchain.languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get().toInt())
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = libs.versions.java.get().toInt()
    }

    dependencies {
        compileOnly(project(":unisrc-api"))
        // it's a godsend
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)
    }
}

configure(plugins) {
    apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)

    dependencies {
        compileOnly(project(":libs:utils"))
    }

    tasks.build {
        dependsOn(tasks.named("shadowJar"))
    }

    tasks.processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft_version" to libs.versions.minecraft.get()
        )
        inputs.properties(props)

        filteringCharset = "UTF-8"
        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(props)
        }
    }
}

tasks.jar {
    enabled = false
}

val serverProject = project(":unisrc-server")

val finalOutDir = rootProject.layout.projectDirectory.dir("out")
val copyToFinalOut by tasks.registering(Copy::class) {
    description = "Copy all built artifacts to final output directory."

    val bundlerTask = serverProject.tasks.named<CreateBundlerJar>("createBundlerJar")
    val launcherTask = serverProject.tasks.named<CreatePaperclipJar>("createPaperclipJar")

    plugins.forEach { plugin ->
        val pluginName = plugin.name
        from(plugin.tasks.shadowJar) {
            into("plugins")
            rename { "$pluginName.jar" }
        }
    }
    from(launcherTask.map { it.outputs.files }) {
        include("*.jar")
        rename { "unisrc-server-launcher.jar" }
    }
    from(bundlerTask.map { it.outputs.files }) {
        include("*.jar")
        rename { "unisrc-server.jar" }
    }

    into(finalOutDir)
}

tasks.build {
    finalizedBy(copyToFinalOut)
}

tasks.clean {
    delete(finalOutDir)
}

runPaper {
    disablePluginJarDetection()
}

tasks.runServer {
    val launcherTask = serverProject.tasks.named<CreateBundlerJar>("createBundlerJar")
    dependsOn(launcherTask)

    minecraftVersion(libs.versions.minecraft.get())
    serverJar(launcherTask.get().outputZip.get().asFile)

    downloadPlugins {
        modrinth("luckperms", "v${libs.versions.luckperms.plugin.get()}-bukkit")
        modrinth("placeholderapi", libs.versions.placeholderapi.get())
        modrinth("viaversion", libs.versions.via.get())
        modrinth("viabackwards", libs.versions.via.get())
        modrinth("oldgenerator", libs.versions.oldgenerator.get())
    }

    plugins.forEach { plugin ->
        val shadowJar = plugin.tasks.named<ShadowJar>("shadowJar")
        dependsOn(shadowJar)

        pluginJars.from(shadowJar.flatMap { it.archiveFile })
    }
}
