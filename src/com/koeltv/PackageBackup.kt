package com.koeltv

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

fun execute(vararg command: String?): BufferedReader {
    val processBuilder = ProcessBuilder(*command)
    processBuilder.redirectErrorStream(true)
    return BufferedReader(InputStreamReader(processBuilder.start().inputStream))
}

fun export(fileName: String) {
    val (name, extension) = Regex("(\\w+)(\\..+)?").find(fileName)!!.destructured
    File("./${name}_temp${extension}").delete()
    File("./$fileName").delete()
    File("./${name}_unavailable.txt").delete()

    val availablePackages = File("./$fileName")
    val unavailablePackages = File("./${name}_unavailable.txt")
    unavailablePackages.appendText(
        """
        ===================================================
        This file contains all packages that can't be found
              You will have to manually download them
        ===================================================
        
    """.trimIndent()
    )

    println("Exporting packages to temporary file...")

    val blackListFile = File("./app/blacklist.txt")
    val blacklist = if (blackListFile.exists()) {
        blackListFile.readLines()
    } else {
        println("blacklist.txt not found, proceeding without blacklist")
        listOf()
    }

    val reader = execute("winget", "export", "-o", "\"${name}_temp${extension}\"")
    val tempAvailablePackages = reader.useLines { lineSequence ->
        lineSequence
            .mapNotNull { line ->
                Regex(": (.+)").find(line)?.destructured?.component1()
            }.filter { pack ->
                !pack.containsAny(blacklist, true)
            }.mapNotNull { pack ->
                val packageName =
                    Regex("(([a-zA-Z][\\w':]*)( [a-zA-Z][\\w':]*)*)").find(pack)!!.destructured.component1()
                val result = execute("winget", "show", packageName, "-s", "winget")

                print("\tCan't find $packageName, retrying...".padEnd(62, ' '))

                val id = result.lineSequence()
                    .firstOrNull { line ->
                        line.matches(Regex(".+ \\[(.+)]"))
                    }?.let { line ->
                        Regex("\\[(.+)]").find(line)!!.destructured.component1()
                    }

                if (id != null) {
                    println("Success!")
                    id
                } else {
                    println("Miss")
                    unavailablePackages.appendText(pack + "\n")
                    null
                }
            }.toList()
    }

    val tempFile = File("./${name}_temp${extension}")
    while (!tempFile.exists()) Thread.sleep(20)
    val tempFileLines = tempFile.readLines()

    println("Saving results in ${availablePackages.name} and ${unavailablePackages.name}...")

    var i = availablePackages.insertAllUntil(tempFileLines) { line -> line.contains("],") }
    tempAvailablePackages.forEach { id ->
        availablePackages.appendText(
            """,
				{
					"PackageIdentifier" : "$id"
				}
                    """.trimIndent()
        )
    }
    while (i in tempFileLines.indices)
        availablePackages.appendText("\n" + tempFileLines[i++])

    tempFile.delete()
}

fun import(fileName: String) {
    val reader =
        execute(
            "winget",
            "import",
            "-i",
            "\"$fileName\"",
            "--accept-package-agreements",
            "--accept-source-agreements"
        )
    reader.useLines { lineSequence ->
        lineSequence.forEach { line ->
            if (line.isNotBlank()) println(line)
        }
    }
}

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    val operation: String
    val filePath: String

    if (args.isNotEmpty()) {
        if (args.size != 2) {
            System.err.println("Wrong format command, format is \"./packageBackup.exe -l/-s/--load/--save filePath\"")
            return
        } else {
            operation = args[0]
            filePath = args[1]
        }
    } else {
        println("Welcome ! With this program you can save or load all the packages on your computer via a save file")
        println("Do you want to save or load a file ? (S/L)")
        print("> ")
        operation = scanner.nextLine()

        println("Please enter the path of the file you want to save to/load")
        print("> ")
        filePath = scanner.nextLine()
    }

    if (operation.isBlank())
        System.err.println("Operation incorrect")
    else if (filePath.isBlank())
        System.err.println("Please input a valid file path")
    else {
        when (operation) {
            "-l", "--load", "L" -> import(filePath)
            "-s", "--save", "S" -> export(filePath)
        }
    }

    print("\nPress enter to exit")
    scanner.nextLine()
}