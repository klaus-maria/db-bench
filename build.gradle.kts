plugins {
    java
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-cli:commons-cli:1.9.0")
    implementation("com.couchbase.client:java-client:3.8.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}
//sourceCompatibility = JavaVersion.VERSION_18
application {
    mainClass.set("DBBench")
}

