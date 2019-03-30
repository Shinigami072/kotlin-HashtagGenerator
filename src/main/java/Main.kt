package shinigami
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.application
import jp.nephy.penicillin.core.session.config.token
import jp.nephy.penicillin.endpoints.trends
import jp.nephy.penicillin.endpoints.trends.availableAreas
import jp.nephy.penicillin.endpoints.trends.place
import jp.nephy.penicillin.extensions.await
import java.io.File
import java.util.*

data class HashtagValue(override val json:JsonObject) :JsonModel{
    val hashtag by string
    val volume by int

}


    fun getHashtag(hashtag:String,volume:Int):HashtagValue{
        return HashtagValue(jsonObjectOf("hashtag" to hashtag,"volume" to volume))
    }

    fun getHashtagDB(hashtags: Collection<Pair<String, Int>>):HashtagDB{
        return getHashtagDB(hashtags, Date().time)
    }
    fun getHashtagDB(hashtags: Collection<Pair<String, Int>>, date:Long):HashtagDB{
        var list = hashtags.map{pair-> jsonObjectOf("hashtag" to pair.first,"volume" to pair.second)}.toJsonArray()
        return HashtagDB(jsonObjectOf("date" to date,"list" to list))


    }

data class HashtagDB(override val json:JsonObject) : JsonModel{
    val hashtags by modelList<HashtagValue>()
    val date by long
    //reqiured due to bugged library
    val hashtag_fix by lazy { json["list"].parseList<HashtagValue>() }
}

//import khttp.get
val FILE = "hashtags.json"
val TIMELIMIT = 60000;



suspend fun loadFile(): HashtagDB{
    var file = File(FILE)
    if(file.exists() && file.canRead() ){
        var DB = file.parse<HashtagDB>()
        if((Date().time-DB.date)<TIMELIMIT){
            return DB;
        }
    }
    return updateDB()
}

suspend fun updateDB(): HashtagDB {
    var trends = mutableMapOf<String,Int>()
    //todo: set limiter
    var i =10

    //i dont really care about these credentials
    val client = PenicillinClient {
        account {
            application("", "")
            token("", "")
        }
    }
    //todo - cleanup
    client.trends.availableAreas().await().forEach { trendArea ->
        if(i>0) {
            i--
            println(trendArea.name)
            println(trendArea.toString())
            var code = trendArea.woeid
            var added = false
            while (!added)
                try {
                    client.trends.place(code.toLong()).await()
                        .forEach { trendPlace ->

                            trendPlace.trends.filter { trend -> trend.name.startsWith("#") }
                                .forEach { trend ->
                                    trends.compute(trend.name) { key, value ->
                                        //reqiured due to bugged library
                                        var num = trend.json.get("tweet_volume").primitive.intOrNull ?: 0
                                        value?.plus(num)?:num

                                    }
                                }
                        }
                    added = true
                } catch (e: PenicillinException) {
                    println("$e waiting a little")
                    Thread.sleep(60000 * 5)
                }
        }
    }
    client.close()


    var list = trends.map { entry-> (entry.key to entry.value) }.sortedBy { pair -> -pair.second }


    //build jsonFile and persist it
    var DB = getHashtagDB(list)
    saveFile(DB)

    return DB;
}

fun saveFile(hashtags: HashtagDB) {

    var output = File(FILE)
    output.writeText(hashtags.toJsonString())
}


suspend fun main(args: Array<out String>){
//    var resp =(get("https://api.twitter.com/1.1/trends/place.json",params = mapOf("id" to "1")).text);
    // Creates new ApiClient

    //        "${yestrday.year}-${yestrday.month.value.f}-${yestrday.dayOfMonth}"yestrday.year


    // Disposes ApiClient
    var db = loadFile()

    println(db)
    println(db.date)
    println(db.json)
    println(db.hashtag_fix)
    println(db.hashtag_fix)
    println(db.hashtag_fix)
//    println("ip = $resp");
}