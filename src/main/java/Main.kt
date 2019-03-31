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
import jp.nephy.penicillin.extensions.RateLimit
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.TrendArea
import java.io.File
import java.lang.Math.random
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
val TIMELIMIT = 10*60*1_000;
val BASELINE= 5_000
val PLACELIMIT = 25


suspend fun loadFile(): HashtagDB{
    var file = File(FILE)
    var map = mutableMapOf<String,Int>()
    if(file.exists() && file.canRead() ){
        var DB = file.parse<HashtagDB>()
        if((Date().time-DB.date)<TIMELIMIT){
            return DB;
        }
        DB.hashtag_fix.forEach{map.put(it.hashtag,it.volume)}
    }
    return updateDB()
}

suspend fun updateDB( trends:MutableMap<String,Int> = mutableMapOf<String,Int>()): HashtagDB {

    //todo: set limiter
    var i =PLACELIMIT

    //i dont really care about these credentials
    val client = PenicillinClient {
        account {
            application("bmwANB88eRfRYmfcbQrpI4El7", "xKX4kfE89r3FPJKJf361RYB1W7gInRMVdcWmdDOws2Dy68obtr")
            token("1111216607672651778-WAtxXGQunaCt9Fcyl5DM1SsGOaNguB", "wS4oTXq4gxnMDgpwt6ShAbalke0OiRog6MoII9pAApN4f")
        }
    }
    //todo - cleanup
    client.trends.availableAreas().await()
        .filter { trendArea -> trendArea.placeType.name=="Supername"||trendArea.placeType.name=="Country"   }
        .forEach { trendArea ->
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
                                        var num = trend.json.get("tweet_volume").primitive.intOrNull ?: BASELINE
                                        value?.plus(num)?:num

                                    }
                                }
                        }
                    added = true
                } catch (e: PenicillinException) {
                    println("$e waiting a little")
                    Thread.sleep(60000)


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


data class StateCounter(var nextStateCount:MutableMap<Char,Int> = mutableMapOf<Char,Int>()){
    val nextState by lazy{
        updatedState()
    }

    fun updatedState(): Map<Char, Double> {
        val count = nextStateCount.values.fold(0){ a, b-> a+b}
        var sum:Double =0.0
        println("update map: $count")
        val map:MutableMap<Char,Double> =  nextStateCount.mapValues { entry -> entry.value.toDouble()/count.toDouble() }
            .toSortedMap(
            kotlin.Comparator {
                    a:Char,b:Char->
                (nextStateCount[a]?:0).compareTo(nextStateCount[b]?:0).let { if(it==0) a.compareTo(b) else it}
            }
        )
        println("values: ${nextStateCount}")
        println("values: ${map}")
        map.mapValuesTo(map){entry -> sum+=entry.value;sum}
        println("values: ${map}")
        return map
    }

    fun addTo(key:Char, value:Int){
        nextStateCount.compute(key){_,v->(v?:0)+value}
    }
}
data class StateMap(var statemap:MutableMap<String,StateCounter> = mutableMapOf(),
                    var begginings:MutableList<Pair<String,Int>> = mutableListOf()){

}

/*


n=3
[ccc]{
# : 0.1 // start/end
a : 0.2 // cca state
b : 0.3
c : 0.4
...
Z : 0.99<1.0
}


*/

fun getNext(stateMap:StateMap,key:String):Char?{
    val nextState: Map<Char, Double> = stateMap.statemap[key]?.nextState ?: return null
    var r = random()

    val restmap = nextState.filterValues { p->p>r }.values
    r= restmap.max()!!
    if(nextState.values.contains(0.0))
        println(nextState)
    return nextState.filterValues { p->p==r }.keys.toList()[0]
}
suspend fun main(args: Array<out String>){
//    var resp =(get("https://api.twitter.com/1.1/trends/place.json",params = mapOf("id" to "1")).text);
    // Creates new ApiClient

    //        "${yestrday.year}-${yestrday.month.value.f}-${yestrday.dayOfMonth}"yestrday.year


    // Disposes ApiClient
    var db = loadFile()

    val n = 3
    println(db)
    println(db.date)
    println(db.json)
    println(db.hashtag_fix)
    var map:StateMap= StateMap();
    db.hashtag_fix.forEach{
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
