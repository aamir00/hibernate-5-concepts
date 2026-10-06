// Standalone demo project: every Hibernate class/concept used by wmstdappdbimpl,
// shown with runnable Main classes, here on Hibernate 7.4. See README.md for the row -> Main index.
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

val hibernateVersion = "7.4.12.Final"

dependencies {
    implementation("org.hibernate.orm:hibernate-core:$hibernateVersion")
    // From 7.4 Hibernate Tools is part of the ORM project: org.hibernate.orm:hibernate-reveng, released with core
    // (it only has hibernate-core at runtime scope, hence the explicit dependency above).
    // Ant is not referenced by it; google-java-format is only used by a pretty printer the demos don't use;
    // JDT (runtime scope again in 7.4) is not needed by these demos.
    implementation("org.hibernate.orm:hibernate-reveng:$hibernateVersion") {
        exclude("org.apache.ant", "ant")
        exclude("com.google.googlejavaformat", "google-java-format")
        exclude("org.eclipse.jdt", "org.eclipse.jdt.core")
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
