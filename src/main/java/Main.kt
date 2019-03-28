package shinigami
import io.ktor.util.isLowerCase
import jp.nephy.penicillin.*
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.response.JsonObjectResponse
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.application
import jp.nephy.penicillin.core.session.config.token
import jp.nephy.penicillin.endpoints.search
import jp.nephy.penicillin.endpoints.search.search
import jp.nephy.penicillin.endpoints.trends
import jp.nephy.penicillin.endpoints.trends.availableAreas
import jp.nephy.penicillin.endpoints.trends.place
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.complete

//import khttp.get

suspend fun main(args: Array<out String>){
//    var resp =(get("https://api.twitter.com/1.1/trends/place.json",params = mapOf("id" to "1")).text);
    // Creates new ApiClient
    val client = PenicillinClient {
        account {
            application("bmwANB88eRfRYmfcbQrpI4El7", "xKX4kfE89r3FPJKJf361RYB1W7gInRMVdcWmdDOws2Dy68obtr")
            token("1111216607672651778-WAtxXGQunaCt9Fcyl5DM1SsGOaNguB", "wS4oTXq4gxnMDgpwt6ShAbalke0OiRog6MoII9pAApN4f")
        }
    }

    var trends = mutableSetOf<String>()
    var i =10
    var id:Long = 502075
    client.trends.place(id).await().forEach { trendPlace ->
        trendPlace.trends.forEach { trend ->
             println("${trend.name} ${trend.tweetVolume}")
        }
    }
    client.trends.availableAreas().await().forEach { trendArea ->
        if(i>0) {
            i--;
            println(trendArea.name)
            println(trendArea.toString())
            var code = trendArea.woeid
            var added = false;
            while (!added)
                try {
                    client.trends.place(code.toLong()).await().forEach { trendPlace ->
                        trendPlace.trends.forEach { trend ->
                            if(trend.name.startsWith("#"))
                                trends.add(trend.name)

                            var calculated = {
                                var curr = 0;
                                var maxId = -1L;
                                var count = 100;
                                do {
                                    println("searcging ${trend.name} $curr")
                                    var done =false
                                    while(!done)
                                    try {
                                        var results = (client.search.search(
                                            trend.name,
                                            geocode = trendArea.name,
                                            count = 100.toInt(),
                                            sinceId = maxId
                                        )).complete().result
                                        done= true

                                        curr += results.searchMetadata.count;
                                        maxId = results.searchMetadata.maxId;
                                        count=results.searchMetadata.count
                                    }catch (e: PenicillinException) {
                                        println("${e.error} waiting a little")
                                        Thread.sleep(60000 * 15);
                                    }
                                } while (count== 100)

                                 curr
                            }()
                            println("${trend.name} ${trend.tweetVolume} $calculated")
                        }
                    }
                    added = true;
                } catch (e: PenicillinException) {
                    println("$e waiting a little")
                    Thread.sleep(60000 * 5);
                }
        }
    }

    println(trends)

    // Disposes ApiClient
    client.close()
    println("hi")
//    println("ip = $resp");
}