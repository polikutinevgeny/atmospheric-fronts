package com.gmail.polikutinevgeny.fields

class DoubleArrayField(override val xCoordinates: DoubleArray,
                       override val yCoordinates: DoubleArray,
                       private val dataArray: Array<DoubleArray>) :
    FieldInterface {
    companion object Factory {
        fun create(xCoordinates: DoubleArray,
                   yCoordinates: DoubleArray,
                   init: (Int, Int) -> Double): DoubleArrayField {
            return DoubleArrayField(xCoordinates, yCoordinates, init)
        }
    }

    override fun clone(init: ((Int, Int) -> Double)?): FieldInterface {
        return DoubleArrayField(xCoordinates, yCoordinates,
            init ?: { _, _ -> 0.0 })
    }

    constructor(xCoordinates: DoubleArray,
                yCoordinates: DoubleArray,
                init: (Int, Int) -> Double)
        : this(xCoordinates, yCoordinates, Array(xCoordinates.size,
        { i -> DoubleArray(yCoordinates.size, { j -> init(i, j) }) }))

    override val size: Pair<Int, Int>
        get() = Pair(dataArray.size, dataArray[0].size)

    override operator fun get(i: Int, j: Int): Double {
        return dataArray[i][j]
    }

    override operator fun set(i: Int, j: Int, value: Double) {
        dataArray[i][j] = value
    }
}