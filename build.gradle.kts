plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.slidermc"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    // Netty 网络框架
    implementation("io.netty:netty-all:4.1.108.Final")

    // 日志框架
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")

    // JetBrains 注解（代码质量）
    implementation("org.jetbrains:annotations:24.1.0")

    // JSON 处理
    implementation("com.google.code.gson:gson:2.10.1")

    // 配置解析
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("org.yaml:snakeyaml:2.2")

    // 测试框架
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.netty:netty-transport-native-epoll:4.1.108.Final:linux-x86_64") // Linux原生传输

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("com.lmax:disruptor:3.4.4")

    implementation("net.kyori:adventure-api:4.24.0")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.24.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.24.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.24.0")
    implementation("net.kyori:adventure-nbt:4.24.0")

    // Brigadier 命令系统
    implementation("com.mojang:brigadier:1.0.18")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

application {
    mainClass.set("net.slidermc.sliderproxy.Main")
}

tasks.shadowJar {
    archiveBaseName.set("sliderproxy")
    archiveClassifier.set("")
    mergeServiceFiles()

    // 排除不必要的文件
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// 创建源码包任务
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
}

// 创建Javadoc任务
tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}