package shinigami


data class StateCounter(var nextStateCount:MutableMap<Char,Int> = mutableMapOf<Char,Int>()){
    val nextState by lazy{
        updatedState()
    }

    fun updatedState(): Map<Char, Double> {

        val count = nextStateCount.values.fold(0){ a, b-> a+b}
        var sum =0.0
        val map:MutableMap<Char,Double> =  nextStateCount.mapValues { entry -> entry.value.toDouble()/count.toDouble() }
            .toSortedMap(
                kotlin.Comparator {
                        a:Char,b:Char->
                    (nextStateCount[a]?:0).compareTo(nextStateCount[b]?:0).let { if(it==0) a.compareTo(b) else it}
                }
            )

        map.mapValuesTo(map){entry -> sum+=entry.value;sum}
        return map

    }

    fun addTo(key:Char, value:Int){
        nextStateCount.compute(key){_,v->(v?:0)+value}
    }
}
data class StateMap(var statemap:MutableMap<String,StateCounter> = mutableMapOf(),
                    var begginings:MutableList<Pair<String,Int>> = mutableListOf()){

}

fun getNext(stateMap:StateMap,key:String):Char?{
    val nextState: Map<Char, Double> = stateMap.statemap[key]?.nextState ?: return null
    var r = Math.random()

    val restmap = nextState.filterValues { p->p>r }.values
    r= restmap.max()!!
    if(nextState.values.contains(0.0))
        println(nextState)
    return nextState.filterValues { p->p==r }.keys.toList()[0]
}