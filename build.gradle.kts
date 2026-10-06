// Standalone demo project: every Hibernate class/concept used by wmstdappdbimpl (5.6.15 originally),
// ported to Hibernate ORM 7.3.13 + hibernate-tools-orm 7.3.13 and shown with runnable Main classes. See README.md for the row -> Main index.
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

val hibernateVersion = "7.3.13.Final"

dependencies {
    implementation("org.hibernate.orm:hibernate-core:$hibernateVersion")
    // tools-orm 7.3 depends on the same hibernate-core 7.3.13 (aligned pair). Ant is not referenced by
    // tools-orm, and google-java-format is only used by an unused pretty-printer.
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
