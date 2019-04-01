package shinigami

import jp.nephy.jsonkt.JsonObject
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.delegation.int
import jp.nephy.jsonkt.delegation.string
import jp.nephy.jsonkt.jsonObjectOf
import jp.nephy.jsonkt.toJsonArray
import java.util.*

data class HashtagValue(override val json:JsonObject) : JsonModel {
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
    val list = hashtags.map{pair-> jsonObjectOf("hashtag" to pair.first,"volume" to pair.second) }.toJsonArray()
    return HashtagDB(jsonObjectOf("date" to date,"list" to list))


}