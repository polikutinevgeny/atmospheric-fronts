package com.gmail.polikutinevgeny.fields

interface FieldInterface {
    val xCoordinates: DoubleArray
    val yCoordinates: DoubleArray
    operator fun get(i: Int, j: Int): Double
    operator fun set(i: Int, j: Int, value: Double)
    val size: Pair<Int, Int>
    fun clone(init: ((Int, Int) -> Double)? = null): FieldInterface
}

fun FieldInterface.maskToCSVWithClassification(
    classification: FieldInterface): String {
    var s = ""
    for (i in 0..xCoordinates.lastIndex) {
        for (j in 0..yCoordinates.lastIndex) {
            if (this[i, j] > 0 && classification[i, j] == -1.0) {
                s += "${xCoordinates[i]}, ${yCoordinates[j]},"
            }
        }
    }
    s = s.dropLast(1) + "\n"
    for (i in 0..xCoordinates.lastIndex) {
        for (j in 0..yCoordinates.lastIndex) {
            if (this[i, j] > 0 && classification[i, j] == 1.0) {
                s += "${xCoordinates[i]}, ${yCoordinates[j]},"
            }
        }
    }
    return s.dropLast(1)
}