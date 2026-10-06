// Standalone demo project: every Hibernate class/concept used by wmstdappdbimpl,
// shown with runnable Main classes, here on Hibernate 6.6. See README.md for the row -> Main index.
plugins {
    java
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val hibernateVersion = "6.6.58.Final"

dependencies {
    implementation("org.hibernate.orm:hibernate-core:$hibernateVersion")
    // Tools 6.6 (artifact renamed to hibernate-tools-orm) is released in lockstep with core 6.6.
    // Ant is not referenced by it; google-java-format is only used by a pretty printer the demos don't use.
    implementation("org.hibernate.tool:hibernate-tools-orm:$hibernateVersion") {
        exclude("org.apache.ant", "ant")
        exclude("com.google.googlejavaformat", "google-java-format")
    }
    implementation("org.freemarker:freemarker:2.3.35")
    implementation("org.hsqldb:hsqldb:2.7.4")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

// ./gradlew run                                  -> RunAllMain
// ./gradlew run -PmainClass=h5.concepts.BootstrapMain
application {
    mainClass = (findProperty("mainClass") as String?) ?: "h5.concepts.RunAllMain"
}
