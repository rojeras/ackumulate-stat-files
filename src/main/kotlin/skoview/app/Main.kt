package skoview.app

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileDescriptor.out
import java.io.PrintWriter
import kotlin.math.log
import kotlin.system.exitProcess

@Serializable
data class StatFile(
    val plattform: String,
    val startDate: String,
    val endDate: String,
    val statistics: List<StatItem>
) {
    fun printJson(targetDir: String) {
        val fname = "${plattform}_$startDate-$endDate.COMBINED.json"
        val fileOut = File("$targetDir/$fname")
        fileOut.printWriter().use { out ->
            out.println(

                """
{
  "plattform": "$plattform",
  "startDate": "$startDate",
  "endDate": "$endDate",
  "statistics": 
  [
        """.trimIndent()
            )

            val statLen = statistics.size
            statistics.forEachIndexed { index, statItem ->
                val addComma = (index < statLen - 1)
                statItem.printJson(out, addComma)
            }

            out.println(
                """
  ]
}
"""
            )
        }
    }
}

@Serializable
data class StatItem(
    val originalConsumerId: String,
    val consumerId: String,
    val calls: Int,
    val namespace: String,
    val logicalAddress: String,
    val averageResponseTime: Int
) {
    fun printJson(fileOut: PrintWriter, addComma: Boolean) {
        var comma = ","
        if (!addComma) {
            comma = ""
        }
        fileOut.println(
            """    {
      "originalConsumerId": "$originalConsumerId",
      "consumerId": "$consumerId",
      "calls": $calls,
      "namespace": "$namespace",
      "logicalAddress": "${logicalAddress}",
      "averageResponseTime": $averageResponseTime
    } $comma """//.trimIndent()
        )
    }

    fun getId(): String {
        return "$originalConsumerId#$consumerId#$namespace#$logicalAddress"
    }

    fun combine(other: StatItem): StatItem {
        if (
            this.originalConsumerId != other.originalConsumerId ||
            this.consumerId != other.consumerId ||
            this.namespace != other.namespace ||
            this.logicalAddress != other.logicalAddress
        ) {
            println("INTERNAL ERROR in combine() - objects do not match")
            println(this)
            println(other)
            exitProcess(1)
        }

        return StatItem(
            this.originalConsumerId,
            this.consumerId,
            this.calls + other.calls,
            this.namespace,
            this.logicalAddress,
            (this.averageResponseTime + other.averageResponseTime) / 2
        )

    }
}

fun main(args: Array<String>) {

    if (args.size != 3) {
        println("ERROR: This program requires target directory and two filenames as parameters")
        exitProcess(1)
    }

    val targetDir = args[0]
    val fileNameA = args[1]
    val fileNameB = args[2]

    /*
    val fileNameA =
        "/home/leo/Documents/data/Eternal/development/AckumulateStatFiles/SLL-QA_2020-05-27--2020-05-27.K8S.json"
    val fileNameB =
        "/home/leo/Documents/data/Eternal/development/AckumulateStatFiles/SLL-QA_2020-05-27--2020-05-27.STAT.json"
    */

    if (fileNameA == fileNameB) {
        println("ERROR: Same file specified twice!")
        exitProcess(1)
    }

    val fileA = File(fileNameA)
    val fileB = File(fileNameB)

    if (!fileA.exists()) {
        println("ERROR: $fileNameA does not exist!")
        exitProcess(1)
    }

    if (!fileB.exists()) {
        println("ERROR: $fileNameB does not exist!")
        exitProcess(1)
    }

    val jsonDataA = fileA.readText()
    val jsonDataB = fileB.readText()

    val json = Json(JsonConfiguration.Stable)

    val statA = json.parse(StatFile.serializer(), jsonDataA)
    val statB = json.parse(StatFile.serializer(), jsonDataB)

    // Verify that it is the same TP time span
    if (statA.plattform != statB.plattform) {
        println("ERROR: The two files represent different plattforms")
        exitProcess(1)
    }
    if ((statA.startDate != statB.startDate) || (statA.endDate != statB.endDate)) {
        println("ERROR: The two files represent different time spans")
        exitProcess(1)
    }

    // Populate the maps
    val statMapA = statA.statistics.associateBy({ it.getId() }, { it })
    val statMapB = statB.statistics.associateBy({ it.getId() }, { it })

    // Loop through statistics in A och add the statistics from B
    val resultStatItemList = mutableListOf<StatItem>()

    for ((key, statItemA) in statMapA) {
        if (statMapB[key] != null) {
            val statMapR = statItemA.combine(statMapB[key]!!)
            resultStatItemList.add(statMapR)
        } else {
            resultStatItemList.add(statItemA)
        }
    }
    // Finally we need to add the items in B which is not part of A
    for ((key, statItemB) in statMapB) {
        if (statMapA[key] == null) {
            resultStatItemList.add(statItemB)
        }
    }

    val resultStatFile = statA.copy(
        statistics = resultStatItemList
    )

    //println(resultStatFile)
        resultStatFile.printJson(targetDir)
}
