import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.undercouch.gradle.tasks.download.Download
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

plugins {
    kotlin("jvm") version "1.9.22"
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("de.undercouch.download") version "5.6.0"
}

val githubToken: String by project


group = "br.com.sankhya.ce"
version = "1.0"
val javaVersion = JavaLanguageVersion.of(8)
val skwVersion = "master"

sourceSets {
    main {
        kotlin.srcDirs("src/main/java", "src/main/kotlin")
        java.srcDirs("src/main/java", "src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
}
repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus-repository.sankhya.com.br/repository/maven-public/")
    }
    maven {
        url = uri("https://nexus-repository.sankhya.com.br/repository/maven-devcenter-releases")
    }
    maven {
        url = uri("https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/")
    }
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/devsankhya/utils-snk")
        credentials(HttpHeaderCredentials::class) {
            name = "Authorization"
            value = "Bearer $githubToken"
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
    maven {
        url = uri("https://maven.oracle.com")
    }


}

val dependenciesToKeep: MutableList<String> = mutableListOf()

/**
 * Implementa e mantem a dependencia na gera��o do arquivo jar
 * @param path caminho da dependencia
 */
fun implementAndKeep(path: String): Dependency? {
    dependenciesToKeep.add(path)
    return project.dependencies.implementation(path)
}

val jar = project.tasks.jar.get()

val manifestValues: Jar = jar.also {
    it.manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Vendor"] = "DevSankhya"
        attributes["Created-By"] = "Luis Ricardo Alves Santos"
        attributes["Created-At"] = "2024-12-05T20:51:26.522"
        attributes["Update-Date"] = LocalDateTime.now().toString()
        attributes["Repository-Url"] = getRepoName()
        attributes["Build-Jdk"] = System.getProperty("java.version")
    }
}


dependencies {
    replaceInGradleFile("Created-By", getCreatorName())
    replaceInGradleFile("Created-At", LocalDateTime.now().toString())
    replaceInGradleFile("Repository-Url", getRepoName())

    // Utilitarios sankhya
    implementAndKeep("com.sankhya.ce:utils-snk:1.0.1")

    // Nativo Sankhya
    implementation("br.com.sankhya", "mge-modelcore", skwVersion)
    implementation("br.com.sankhya", "jape", skwVersion)
    implementation("br.com.sankhya", "dwf", skwVersion)
    implementation("br.com.sankhya", "sanws", skwVersion)
    implementation("br.com.sankhya", "mge-param", skwVersion)
    implementation("br.com.sankhya", "skw-environment", skwVersion)
    implementation("br.com.sankhya", "sanutil", skwVersion)
    implementation("br.com.sankhya", "cuckoo", skwVersion)
    implementation("br.com.sankhya", "mgecom-model", skwVersion)
    implementation("br.com.sankhya", "mgefin-model", skwVersion)


    implementation("net.sf.jasperreports", "jasperreports", "4.0.0")
    implementation("org.apache.poi:poi:5.3.0")
    // https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml
    implementation("org.apache.poi:poi-ooxml:5.3.0")


    // Status HTTP / Apoio as Servlets
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Manipulador de JSON
    implementation("com.google.code.gson", "gson", "2.1")

    // EJB / Escrever no container wildfly
    implementation("org.wildfly:wildfly-spec-api:16.0.0.Final")
    implementation("org.jdom", "jdom", "1.1.3")
    implementation("com.oracle.database.jdbc:ojdbc8:19.11.0.0")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = javaVersion
        targetCompatibility = JavaVersion.toVersion(javaVersion.asInt())
        sourceCompatibility = JavaVersion.toVersion(javaVersion.asInt())
    }
}

fun replaceInGradleFile(key: String, value: String) {
    val gradleFile = file("build.gradle.kts")
    val content = gradleFile.readText()
    val newContent = content.replace("{{$key}}", value)
    gradleFile.writeText(newContent)
}

fun getCreatorName(): String {
    val gitConfig = "git config --get user.name"
    val value = runCommand(gitConfig)
    return value
}

fun getRepoName(): String {
    val gitConfig = "git config --get remote.origin.url"
    val value = runCommand(gitConfig)
    return value
}

fun runCommand(str: String): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = str.split(" ")
            standardOutput = byteOut
        }
        String(byteOut.toByteArray()).trim().also {
            if (it == "HEAD") logger.warn("Unable to determine current branch: Project is checked out with detached head!")
        }
    } catch (e: Exception) {
        logger.warn("Unable to determine current branch: ${e.message}")
        "Unknown Branch"
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    dependsOn("downloadFile")
    manifestValues
    archiveClassifier.set("fat")

    dependencies {
        // Get kotlin dependecy string
        dependenciesToKeep.map {
            include(dependency(it))
        }
    }
    // Get libs folder and add to jar
    from(files("build/tmp")) {
        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib:1.3.50"))
            include(dependency("org.jetbrains.kotlin:kotlin-reflect:1.3.50"))
        }
    }
    mergeServiceFiles()
}

tasks.register<Download>("downloadFile") {
    onlyIf { !file("build/tmp/kotlin-stdlib-1.3.50.jar").exists() }
    src("https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.3.50/kotlin-stdlib-1.3.50.jar")
    dest("build/tmp/kotlin-stdlib-1.3.50.jar")
    overwrite(true)

}
tasks.withType<JavaCompile> {
    options.encoding = "windows-1252"
}

// Add java version to manifest
tasks.withType<Jar> {
    manifestValues
}