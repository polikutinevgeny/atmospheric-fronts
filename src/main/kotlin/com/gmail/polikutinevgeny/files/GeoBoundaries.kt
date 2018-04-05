package com.gmail.polikutinevgeny.files

import ucar.ma2.Range
import ucar.nc2.Variable

internal fun getRange(variable: Variable, min: Double, max: Double): Range {
    if (variable.rank != 1) {
        throw IllegalArgumentException("Variable '$variable' must have rank == 1")
    }
    val arr = variable.read().get1DJavaArray(Double::class.java) as DoubleArray
    var imin = arr.indices.minBy { if (arr[it] >= min) arr[it] else Double.MAX_VALUE }!!
    var imax = arr.indices.maxBy { if (arr[it] <= max) arr[it] else Double.MIN_VALUE }!!
    if (imin > imax) {
        imax = imin.also { imin = imax }
    }
    return Range(imin, imax)
}

data class GeoBoundaries(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double) {
    fun getLatRange(lat: Variable): Range {
        return getRange(lat, minLat, maxLat)
    }
    fun getLonRange(lon: Variable): Range {
        return getRange(lon, minLon, maxLon)
    }
}