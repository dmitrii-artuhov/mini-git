plugins {
    java
}

group = "org.itmo.java"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("commons-codec:commons-codec:1.11") // утилиты для хеширования
    implementation("commons-io:commons-io:2.6") // утилиты для работы с IO
    implementation("commons-cli:commons-cli:1.4") // фреймворк для создания CLI
    testImplementation(platform("org.junit:junit-bom:5.8.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

tasks.test {
    useJUnitPlatform()
}
