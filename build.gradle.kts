// Standalone demo project: every Hibernate 5.6.15 class/concept used by wmstdappdbimpl,
// shown with runnable Main classes. See README.md for the row -> Main index.
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

val hibernateVersion = "5.6.15.Final"

dependencies {
    implementation("org.hibernate:hibernate-core-jakarta:$hibernateVersion")
    // Same excludes as wmstdappdbimpl: Tools 5.x drags in the javax hibernate-core, Ant and JDT.
    implementation("org.hibernate:hibernate-tools:$hibernateVersion") {
        exclude("org.hibernate", "hibernate-core")
        exclude("org.apache.ant", "ant")
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
