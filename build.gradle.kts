plugins {
    id("java")
    id("antlr")
    id("application")
}

group = "com.github.besok.foresterlsp"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.0")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.21.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.github.besok.foresterlsp.ForesterLanguageServer")
}

tasks.generateGrammarSource {
    arguments = arguments.plus("-package")
    arguments = arguments.plus("com.github.besok.foresterlsp.grammar")
    outputDirectory = file("$buildDir/generated-src/antlr/main/com/github/besok/foresterlsp/grammar/")
}

sourceSets {
    main {
        java {
            srcDir(tasks.generateGrammarSource)
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// Self-contained server jar (all dependencies bundled), runnable with `java -jar`.
val fatJar = tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.github.besok.foresterlsp.ForesterLanguageServer"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    }
}

// Copy the fat jar into the VS Code extension so it is bundled in the .vsix.
val copyServer = tasks.register<Copy>("copyServer") {
    dependsOn(fatJar)
    from(fatJar)
    into(layout.projectDirectory.dir("editors/vscode/server"))
    rename { "forester-lsp.jar" }
}

// Build the .vsix, bundling the server jar (requires npm on PATH).
tasks.register<Exec>("buildVsix") {
    group = "distribution"
    description = "Builds the VS Code extension (.vsix) with the server jar bundled"
    dependsOn(copyServer)
    workingDir = layout.projectDirectory.dir("editors/vscode").asFile
    commandLine("npm", "run", "package")
}
