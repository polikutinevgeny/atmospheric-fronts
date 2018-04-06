package com.gmail.polikutinevgeny.fields

interface FieldInterface {
    val xCoordinates: DoubleArray
    val yCoordinates: DoubleArray
    operator fun get(i: Int, j: Int): Double
    operator fun set(i: Int, j: Int, value: Double)
    val size: Pair<Int, Int>
    fun clone(init: ((Int, Int) -> Double)? = null): FieldInterface
}