package shinigami

import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.application
import jp.nephy.penicillin.core.session.config.token
import jp.nephy.penicillin.endpoints.trends
import jp.nephy.penicillin.endpoints.trends.availableAreas
import jp.nephy.penicillin.endpoints.trends.place
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.TrendArea

class TwitterDBUpdater(

    private val consumerKey: String,
    private val consumerSecret: String,
    private val accessToken: String,
    private val accessTokenSecret: String
) : DBUpdater {

    private var client: ApiClient = PenicillinClient {
        account {
            application(consumerKey, consumerSecret)
            token(accessToken, accessTokenSecret)
        }

    }

    var placeLimit = 10
    var types:Set<String> = setOf("Supername","Country")
    private val _filter =
        { trendArea: TrendArea ->types.contains(trendArea.placeType.name) }
    var timeout: Long = 60000

    override suspend fun updateDB(trends: MutableMap<String, Int>): HashtagDB {
        var limit = placeLimit

        client.trends.availableAreas().await()
            //process only areas meeting the criteria
            .filter(_filter)
            .filter { limit > 0 }
            //process all areas
            .forEach { trendArea ->
                //decrease the allowed areas
                limit--

                val code = trendArea.woeid.toLong()

                var added = false
                //retry until connection was made
                while (!added) {
                    try {
                        client.trends.place(code)
                            .await()
                            //processing all hash-tags
                            .forEach { trendPlace -> trendPlace.trends
                                    .filter { trend -> trend.name.startsWith("#") }
                                    //adding/updating the count of hash-tag
                                    .forEach { trend -> trends.compute(trend.name) { _, value ->
                                            //required to do this manually due to bugged json library
                                            val num = trend.json["tweet_volume"].primitive.intOrNull ?: settings.BASELINE
                                            //increasing the count
                                            //todo: investigate
                                        value?.plus(num) ?: num
                                        }
                                    }
                            }
                        added = true
                    } catch (e: PenicillinException) {
                        println("$e waiting ${timeout}ms")
                        Thread.sleep(timeout)


                    }
                }
            }

        //create a list of all hash-tags with associated volumes
        val list = trends.map{ entry ->
            (entry.key to entry.value) }
            //sort from biggest to lowest value
            .sortedBy { pair -> -pair.second }


        //build jsonFile
        return getHashtagDB(list)
    }

    override fun close() {
        client.close()
    }

}