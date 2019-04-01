package shinigami

interface DBUpdater{

    fun close(){}

    suspend fun updateDB(trends:MutableMap<String,Int> = mutableMapOf<String,Int>() ):HashtagDB
}