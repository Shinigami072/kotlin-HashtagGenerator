package shinigami

import jp.nephy.jsonkt.JsonObject
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.delegation.long
import jp.nephy.jsonkt.delegation.modelList
import jp.nephy.jsonkt.parseList

data class HashtagDB(override val json:JsonObject) : JsonModel {
    val hashtags by modelList<HashtagValue>()
    val date by long
    //reqiured due to bugged library
    val hashtagFix by lazy { json["list"].parseList<HashtagValue>() }
}
