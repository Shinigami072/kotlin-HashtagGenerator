package shinigami
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import java.io.File
import java.util.*

suspend fun loadFile(updater:DBUpdater): HashtagDB{
    //try to load tge file
    val file = File(settings.FILE)
    val map = mutableMapOf<String,Int>()

    //if file exists and can be read
    if(file.exists() && file.canRead() ){

        val database = file.parse<HashtagDB>()

        //check if it is recent enough
        if((Date().time-database.date)<settings.TIMELIMIT){
            return database;
        }

        //make the map
        database.hashtagFix.forEach{ map[it.hashtag] = it.volume }
    }

    //get Updated database
    return updater.updateDB(map)
}

fun saveFile(hashtags: HashtagDB) {

    var output = File(settings.FILE)
    output.writeText(hashtags.toJsonString())
}

data class Settings(override val json:JsonObject) : JsonModel{
    val FILE  by string
    val TIMELIMIT by int
    val timeout by long
    val BASELINE by int
    val PLACELIMIT by int
    val consumerKey by string
    val consumerPrivate by string
    val accessToken by string
    val accessTokenSecret by string
    val types by stringList
}

var settings:Settings = {
    val file = File("settings.json")
    //if file exists and can be read
    if(file.exists() && file.canRead() ){
         file.parse<Settings>()
    }
    else{
        //save default config
        val  set1 = Settings(jsonObjectOf(
            "FILE" to "hashtags.json",
            "TIMELIMIT" to 10*60*1_000,
            "timeout" to 60*1_000,
            "BASELINE" to 5_000,
            "PLACELIMIT" to 25,
            "consumerKey" to "",
            "consumerPrivate" to "",
            "accessToken" to "",
            "accessTokenSecret" to "",
            "types" to setOf("Country","Supername")
        ))
        file.writeText(set1.toJsonString())
         set1
    }
}()

suspend fun main(args: Array<out String>){

    println("Settings:")
    println(settings)

    val client = TwitterDBUpdater(
        settings.consumerKey,
        settings.consumerPrivate,
        settings.accessToken,
        settings.accessTokenSecret)
    client.timeout = settings.timeout
    client.placeLimit = settings.PLACELIMIT
    client.types = settings.types.toSet()
    println("Loading/Creating Database")
    //load Files
    val db = loadFile(client)
    println("cleaningUp Connection")
    //cleanup client
    client.close()
    //persist File
    saveFile(db)


    val n = 3
    println(db)
    println(db.date)
    println(db.json)
    println(db.hashtagFix)
    var map:StateMap= StateMap();
    db.hashtagFix.forEach{
        var v = it.volume
        if(v>30_000)
            v=30_000
        (it.hashtag+"#").windowed(n,1)
        {key->
            map.statemap.compute(key.take(n-1).toString())
            {_,vol->

                    var states = vol?:StateCounter()
                    states.addTo(key[n-1],if(key[n-1]=='#'){(v*5)/8}else{v})
                states
            }
        }
        map.begginings.add(it.hashtag.take(n-1) to v)
    }


    println(map)
    println(map.begginings[0].first)
    println(map.statemap[map.begginings[0].first]?.nextState)
    var k = map.begginings.random().first
    do {
      var c = getNext(map,k.takeLast(n-1))
        if(k.length>255)
            break;
        if(c!=null&&c!='#')
        k+=(c);
    println(k)
    }while (c!=null&&c!='#')

    println(k)

}
//    println("ip = $resp");
