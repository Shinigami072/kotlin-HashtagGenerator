package shinigami

import jp.nephy.jsonkt.JsonObject
import jp.nephy.jsonkt.delegation.*
import jp.nephy.jsonkt.jsonObjectOf
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonString
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.set

suspend fun loadFile(updater: DBUpdater): HashtagDB {
    //try to load tge file
    val file = File(settings.file)
    val map = mutableMapOf<String, Int>()

    //if file exists and can be read
    if (file.exists() && file.canRead()) {

        val database = file.parse<HashtagDB>()

        //check if it is recent enough
        if ((Date().time - database.date) < settings.timeLimit) {
            return database
        }

        //make the map
        database.hashtagFix.forEach { map[it.hashtag] = it.volume }
    }

    //get Updated database
    return updater.updateDB(map)
}

fun saveFile(hashtags: HashtagDB) {

    val output = File(settings.file)
    output.writeText(hashtags.toJsonString())
}

data class Settings(override val json: JsonObject) : JsonModel {
    val file by string
    val modelfile by string
    val timeLimit by int
    val timeout by long
    val baseLine by int
    val placeLimit by int
    val consumerKey by string
    val consumerPrivate by string
    val accessToken by string
    val accessTokenSecret by string
    val types by stringList
    val length: Int by int
    val maxVolume: Int by int
    val endBonus: Double by double
    val maxLength: Int by int
}

var settings: Settings = {
    val file = File("settings.json")
    //if file exists and can be read
    if (file.exists() && file.canRead()) {
        file.parse()
    } else {
        //save default config
        val set1 = Settings(
            jsonObjectOf(
                "file" to "hashtags.json",
                "timeLimit" to 10 * 60 * 1_000,
                "timeout" to 60 * 1_000,
                "baseLine" to 5_000,
                "placeLimit" to 25,
                "consumerKey" to "",
                "consumerPrivate" to "",
                "accessToken" to "",
                "accessTokenSecret" to "",
                "types" to setOf(
                    "Country", "Supername"
                ),
                "length" to 3,
                "maxVolume" to 30_000,
                "endBonus" to 5.0 / 8.0,
                "maxLength" to 255,
                "modelfile" to "model.json"
            )
        )
        file.writeText(set1.toJsonString())
        set1
    }
}()

@ImplicitReflectionSerializer
suspend fun main(args: Array<out String>) {

    val client = TwitterDBUpdater(
        settings.consumerKey,
        settings.consumerPrivate,
        settings.accessToken,
        settings.accessTokenSecret
    )
    client.timeout = settings.timeout
    client.placeLimit = settings.placeLimit
    client.types = settings.types.toSet()

    println("Loading/Creating Database")
    //load Files
    val db = loadFile(client)
    println("cleaningUp Connection")
    //cleanup client
    client.close()
    //persist File
    saveFile(db)

    println("Loading/Creating Model")
    val map =
        if (Files.exists(Paths.get(settings.modelfile)) &&
            Files.isReadable(Paths.get(settings.modelfile)) &&
            (Date().time - db.date) < settings.timeLimit
        ) {
            Json.parse(StateMap.serializer(), File(settings.modelfile).readText())
        } else {
            StateMap(
                list = db.hashtagFix,
                length = settings.length,
                maxVolume = settings.maxVolume,
                endBonus = settings.endBonus
            ).also {
                println("saving model")
                it.saveTofile(settings.modelfile)
                println("saved model")
            }
        }

    println("creating Hashtag")
    for (i in 1..50) {
        val hashTag = map.getString(settings.maxLength)
        println(hashTag)
    }


}
//    println("ip = $resp");
