import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val kotlinVersion = "1.6.10"

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
    id("com.github.gmazzo.buildconfig") version "3.0.3"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.aliyun.com/nexus/content/groups/public/")
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_15.toString()
    }
}
tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_15.toString()
    }
}

dependencies {
    implementation(files("libs\\bcprov-jdk15on-159.jar"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.0-native-mt")
    //implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "XLogDecoderTool"
            packageVersion = "1.0.0"
            vendor = "Virogu"
            windows {
                console = true
                menu = true
                dirChooser = true
                shortcut = true
                perUserInstall = false
                //menuGroup = myMenuGroup
                iconFile.set(project.file("src/main/resources/icon.ico"))
                //upgradeUuid = winUpgradeUuid
            }
        }
    }
}
