﻿import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

val gitCommitCount: Int = with(ByteArrayOutputStream()) {
    use { os ->
        exec {
            executable = "git"
            args = listOf("rev-list", "--all", "--count")
            standardOutput = os
        }
        val revision = os.toString().trim()
        return@with revision.toInt()
    }
}
val buildFormatDate: String = with(SimpleDateFormat("yyMMdd")) {
    format(Date())
}
val gitCommitShortid: String = with(ByteArrayOutputStream()) {
    use { os ->
        exec {
            executable = "git"
            args = listOf("rev-parse", "--short", "HEAD")
            standardOutput = os
        }
        return@with os.toString().trim()
    }
}

val packageVersionTriple by lazy {
    val MAJOR = (gitCommitCount / 1000 / 255).coerceAtLeast(1)
    val MINOR = (gitCommitCount / 1000 % 255)
    val PATCH = gitCommitCount % 1000
    Triple(MAJOR, MINOR, PATCH)
}

val myPackageVersion: String by lazy {
    with(packageVersionTriple) {
        "${first}.${second}.${third}"
    }
}

val kotlinVersion: String by project
val myPackageVendor: String by project
val winUpgradeUuid: String by project

val programName = rootProject.name

project.extra["gitCommitCount"] = gitCommitCount
project.extra["programName"] = programName
project.extra["buildFormatDate"] = buildFormatDate
project.extra["gitCommitShortid"] = gitCommitShortid
project.extra["packageVersionTriple"] = packageVersionTriple
project.extra["myPackageVersion"] = myPackageVersion

tasks.create("packageMsiAndRename") {
    group = "publish"
    dependsOn("packageMsi")
    doLast {
        println("do rename task")
        project.rootDir.resolve("out/packages/main/msi").listFiles()?.filter {
            it.name.endsWith(".msi")
        }?.forEach {
            val newName = "$programName-${myPackageVersion}_${gitCommitShortid}.msi"
            println("rename [${it.name}] to [$newName]")
            it.renameTo(File(it.parentFile, newName))
        }
    }
}

task("zipPackageFiles", Zip::class) {
    rootProject.rootDir.resolve("out/zip").apply {
        println("clear path:[${this.path}]")
        this.deleteRecursively()
    }
    group = "publish"
    from("C:\\Program Files\\$programName") {
        //include {
        //    println("found file [${it.path}]")
        //    true
        //}
    }
    // programName-myPackageVersion-gitCommitShortid.zip
    archiveBaseName.set(programName)
    archiveAppendix.set(myPackageVersion)
    archiveVersion.set(gitCommitShortid)
    archiveExtension.set("zip")
    destinationDirectory.set(rootProject.rootDir.resolve("out/zip"))
    doLast {
        val zipFile = archiveFile.get().asFile
        val size = DecimalFormat(".##").format(zipFile.length() / (1024 * 1024f))
        println("zip file [${zipFile.path}] success, size: ${size}MB")
    }
}