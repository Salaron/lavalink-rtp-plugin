plugins {
    java
    alias(libs.plugins.lavalink)
}

group = "com.github.salaron"
version = "0.1.1"

lavalinkPlugin {
    name = "lavalink-rtp-plugin"
    apiVersion = libs.versions.lavalink.api
    serverVersion = libs.versions.lavalink.server
}

base {
    archivesName = "lavalink-rtp-plugin"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}
