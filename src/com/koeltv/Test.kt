import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

val blacklist = listOf(
    "Microsoft",
    "Windows",
    "Xbox",
    "Extensions",
    "MSN Météo",
    "Programme d'installation d'application",
    "Services de jeu",
    "Obtenir de l'aide",
    "Local Experience Pack",
    "Paquete de experiencia local",
    "Module d'exp",
    "Paint 3D",
    "Visionneuse",
    "Portail de réalité mixte",
    "Print 3D",
    "Capture d'écran et croquis",
    "Hub de commentaires",
    "Films et TV",
    "Courrier et calendrier"
)

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
        execute("winget", "import", "-i", "\"$fileName\"", "--accept-package-agreements", "--accept-source-agreements")
    reader.useLines { lineSequence ->
        lineSequence.forEach { line ->
            if (line.isNotBlank()) println(line)
        }
    }
}

fun main(args: Array<String>) {
    try {
        if (args.isNotEmpty()) {
            if (args.size != 2)
                throw Exception("Wrong format command, format is \"program -l/-s/--load/--save filePath\"")
            when (args[0]) {
                "-l", "--load" -> import(args[1])
                "-s", "--save" -> export(args[1])
            }
        } else {
            val scanner = Scanner(System.`in`)
            println("Welcome ! With this program you can save or load all the packages on your computer via a save file")
            println("Do you want to load or save a file ? (S/L)")
            print("> ")
            val choice = scanner.nextLine()

            println("Please enter the path of the file you want to save to/load")
            print("> ")
            val filePath = scanner.nextLine()

            when (choice) {
                "S" -> export(filePath)
                "L" -> import(filePath)
            }
        }
    } catch (exception: Exception) {
        System.err.println(exception.message)
    }
}