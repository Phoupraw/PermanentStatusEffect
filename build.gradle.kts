plugins {
    kotlin("jvm")
    id("fabric-loom")
    `maven-publish`
}
version = property("mod_version")!!
group = property("maven_group")!!
val minecraftVersion = property("minecraft_version")!!
val baseName = property("archives_base_name").toString()

repositories {
    mavenLocal {
        content {
            includeGroup("phoupraw.mcmod")
        }
    }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://minecraft.curseforge.com/api/maven/") {
        name = "CurseForge"
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://maven.ladysnake.org/releases") {
        name = "Ladysnake Mods"
        content {
            includeGroup("dev.onyxstudios.cardinal-components-api")
        }
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
        content {
            includeGroup("dev.isxander.yacl")//yet-another-config-lib-fabric
        }
    }
    maven("https://maven.quiltmc.org/repository/release") {
        content {
            includeGroup("org.quiltmc.parsers")
            includeGroup("com.twelvemonkeys.imageio")
            includeGroup("com.twelvemonkeys.common")
        }
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots") {
        content {

        }
    }
    maven("https://maven.terraformersmc.com/") {
        name = "TerraformersMC"
        content {
            includeGroup("com.terraformersmc")//modmenu
            includeGroup("dev.emi")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modApi("net.fabricmc:fabric-loader:${property("loader_version")}")
    modApi("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modApi("dev.isxander.yacl:yet-another-config-lib-fabric:${property("yet_another_config_lib")}")
    modApi("com.terraformersmc:modmenu:${property("modmenu")}")
    compileOnlyApi(annotationProcessor("org.projectlombok:lombok:${property("lombok")}")!!)

    //由于KT自带的与java互操作不太好用，所以我自己写了一个
    modCompileOnlyApi("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}") {
        exclude(module = "fabric-transfer-api-v1")
    }
    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modCompileOnly("phoupraw.mcmod:FabricAPIKotlinOverwrite:+")

    //旅行者背包
    modCompileOnly("maven.modrinth:travelersbackpack:${property("travelersbackpack")}")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-api:${property("cardinal_components_api")}")
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-api:${property("cardinal_components_api")}")

    //扩展炼药
    //modCompileOnly("curse.maven:extra-alchemy-247357:4673088")//403 Forbidden
    //源代码："D:\CCC\Documents\local_maven\ExtraAlchemy-fabric-main.zip"
    modCompileOnly(files("D:\\CCC\\Documents\\local_maven\\extraalchemy-fabric-1.20.1-1.10.0.jar"))
}

tasks {
    processResources {
        inputs.property("version", version)
        filesMatching("fabric.mod.json") {
            expand("version" to version)
        }
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
        kotlinOptions.freeCompilerArgs += "-Xextended-compiler-checks"
        kotlinOptions.freeCompilerArgs += "-Xlambdas=indy"
        kotlinOptions.freeCompilerArgs += "-Xjdk-release=17"
    }
    compileJava {
        targetCompatibility = "17"
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
java {
//    withJavadocJar()
    withSourcesJar()
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = baseName
            version = version
            println(getComponents().toList())
            from(getComponents()["java"])
        }

    }
//    repositories {
//        repositories.mavenLocal()
//    }
}
fabricApi {
    configureDataGeneration()
}
kotlin {
    jvmToolchain(17)
}