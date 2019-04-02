package shinigami

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Math.min


@Serializable
data class StateCounter(var nextStateCount: MutableMap<Char, Int> = mutableMapOf()) {
    //calculate actual nextState only if nescessary
    @Transient
    val nextState by lazy {


        //get sum off all states in this counter
        val count = nextStateCount.values.sum()


        var sum = 0
        val map = nextStateCount.toSortedMap(
            //sort by chance - or alphabetically if the same chance
            kotlin.Comparator { a: Char, b: Char ->

                //compare chance
                (nextStateCount[a] ?: 0).compareTo(nextStateCount[b] ?: 0)

                    .let {
                        if (it == 0)
                        //compare alphabetically
                            a.compareTo(b)
                        else
                            it
                    }
            }
            //remap values so they are sequential and normalize them
        ).mapValues { entry ->
            sum += entry.value
            sum.toDouble() / count.toDouble()
        }


        map
    }

    fun getClosest(key: Double): Double {
        val restmap = nextState.filterValues { p -> p > key }.values
        return restmap.min()!!
    }

    fun addTo(key: Char, value: Int) {
        nextStateCount.compute(key) { _, v -> (v ?: 0) + value }//todo: maybe change update
    }
}

@Serializer(forClass = CharSequence::class)
object CharSequenceSerializer : KSerializer<MutableMap<CharSequence, StateCounter>> {

    val p = Pair(String.serializer(), StateCounter.serializer())

    override fun serialize(encoder: Encoder, obj: MutableMap<CharSequence, StateCounter>) {


        p.map.serialize(encoder, obj.mapKeys { k -> k.key.toString() })

    }

    override fun deserialize(decoder: Decoder): MutableMap<CharSequence, StateCounter> {

        return p.map.deserialize(decoder).toMutableMap()
    }
}

@Serializable
data class StateMap(
    @Optional
    private val length: Int = 3,
    @Serializable(with = CharSequenceSerializer::class)
    private var statemap: MutableMap<CharSequence, StateCounter> = mutableMapOf(),
    private var begginings: MutableMap<String, Int> = mutableMapOf()
) {

    constructor(
        list: List<HashtagValue>,
        maxVolume: Int = 30_000,
        endBonus: Double = 5.0 / 8.0,
        length: Int = 3
    ) : this(length = length) {

        list.forEach {
            val effectiveVolume = min(it.volume, maxVolume)

            //add End signifier - each hashtag can only contain 1 # so it is a safe end deliminer
            //and porcess each sequence of character one by one
            (it.hashtag + "#").windowed(length, 1)
            { sequence ->
                //get corresponding state
                statemap.compute(sequence.take(length - 1))
                { _, vol ->

                    val states = vol ?: StateCounter(mutableMapOf())
                    val endState = sequence[length - 1]
                    states.addTo(
                        endState,

                        //end "bonus"
                        if (endState == '#') {
                            (effectiveVolume * endBonus).toInt()
                        } else {
                            effectiveVolume
                        }
                    )

                    states
                }
            }

            //add to list of possible has-tag begin states
            begginings[it.hashtag.take(length - 1)] = effectiveVolume
        }
    }

    fun getNext(key: CharSequence): Char? {

        val nextStates = statemap[key] ?: return null

        val state = nextStates.getClosest(Math.random())

        return nextStates.nextState.filterValues { p -> p == state }.keys.first()

    }

    fun getString(maxLength: Int = 255): String {


        val hashTagBuilder = StringBuilder(begginings.keys.random())
        do {

            //getNextState
            val nextState = getNext(hashTagBuilder.takeLast(length - 1))

            //true condition of the loop
            if (nextState != null && nextState != '#' && hashTagBuilder.length < maxLength)
                hashTagBuilder.append(nextState)
            else
                break

        } while (true)

        return hashTagBuilder.toString()
    }


    @ImplicitReflectionSerializer
    fun saveTofile(filename: String): File {
        val file = File(filename)
        file.writeText(Json.stringify(this))
        return file
    }

}