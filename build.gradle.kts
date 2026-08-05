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
        languageVersion.set(JavaLanguageVersion.of(17))
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
