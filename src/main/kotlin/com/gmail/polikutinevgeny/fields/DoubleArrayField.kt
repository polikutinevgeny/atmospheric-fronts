package com.gmail.polikutinevgeny.fields

import boofcv.alg.filter.blur.GBlurImageOps
import boofcv.struct.image.GrayF64

class DoubleArrayField(override val xCoordinates: DoubleArray,
                       override val yCoordinates: DoubleArray,
                       private var data: DoubleArray) : FieldInterface {

    constructor(xCoordinates: DoubleArray,
                yCoordinates: DoubleArray,
                init: (Int, Int) -> Double) :
        this(xCoordinates, yCoordinates, DoubleArray(
            xCoordinates.size * yCoordinates.size,
            { i -> init(i / yCoordinates.size, i % yCoordinates.size) }))

    companion object Factory {
        fun create(xCoordinates: DoubleArray,
                   yCoordinates: DoubleArray,
                   init: (Int, Int) -> Double): DoubleArrayField {
            return DoubleArrayField(xCoordinates, yCoordinates, init)
        }

        fun create(xCoordinates: DoubleArray,
                   yCoordinates: DoubleArray,
                   data: DoubleArray): DoubleArrayField {
            return DoubleArrayField(xCoordinates, yCoordinates, data)
        }
    }

    override fun clone(init: ((Int, Int) -> Double)?): FieldInterface {
        return DoubleArrayField(xCoordinates, yCoordinates,
            init ?: { _, _ -> 0.0 })
    }

    override val size: Pair<Int, Int>
        get() = Pair(xCoordinates.size, yCoordinates.size)

    override operator fun get(i: Int, j: Int): Double {
        return data[i * yCoordinates.size + j]
    }

    override operator fun set(i: Int, j: Int, value: Double) {
        data[i * yCoordinates.size + j] = value
    }

    fun toBoofCVImage(): GrayF64 {
        val res = GrayF64(xCoordinates.size, yCoordinates.size)
        res.setData(data)
        return res
    }

    fun gaussianBlur(sigma: Double, radius: Int) {
        val temp = toBoofCVImage()
        val res = GBlurImageOps.gaussian(temp, null, sigma, radius, null)
        data = res.getData()
    }
}