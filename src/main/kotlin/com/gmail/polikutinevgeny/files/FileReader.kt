package com.gmail.polikutinevgeny.files

import com.gmail.polikutinevgeny.fields.FieldInterface
import ucar.ma2.ArrayFloat
import ucar.ma2.Range
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable

class FileReader(filename: String) {
    private val file: NetcdfFile = NetcdfFile.open(filename)
    private val variables: MutableList<Variable> = file.variables

    fun <Field : FieldInterface>
        readIsobaricVariable(name: String,
                             isobaric: Double,
                             boundaries: GeoBoundaries,
                             factory: (DoubleArray, DoubleArray, (Int, Int) -> Double) -> Field): Field {
        val variable: Variable = variables.find { it -> it.fullName == name }!!
        if (variable.rank != 4) {
            throw IllegalArgumentException(
                "Variable '$variable' doesn't have 4 dimensions")
        }
        val dimNames = variable.dimensionsString.split(" ")
        val timeName = dimNames.find { it.matches(Regex("time.*")) }
        val isobaricName = dimNames.find { it.matches(Regex("isobaric.*")) }
        val latName = dimNames.find { it.matches(Regex("lat.*")) }
        val lonName = dimNames.find { it.matches(Regex("lon.*")) }
        val time = variables.find { it -> it.fullName == timeName }!!
        val iso = variables.find { it -> it.fullName == isobaricName }!!
        val lat = variables.find { it -> it.fullName == latName }!!
        val lon = variables.find { it -> it.fullName == lonName }!!
        val params = mutableListOf<Range>().apply {
            add(variable.findDimensionIndex(timeName), getRange(time, 0.0, 0.0))
            val range = getRange(iso, isobaric, isobaric)
            if (range.first() != range.last()) {
                throw IllegalArgumentException(
                    "Isobaric surface $isobaric not found")
            }
            add(variable.findDimensionIndex(isobaricName), range)
            add(variable.findDimensionIndex(latName),
                boundaries.getLatRange(lat))
            add(variable.findDimensionIndex(lonName),
                boundaries.getLonRange(lon))
        }
        val latData = lat.read(
            mutableListOf(boundaries.getLatRange(lat))).get1DJavaArray(
            Double::class.java) as DoubleArray
        val lonData = lon.read(
            mutableListOf(boundaries.getLonRange(lon))).get1DJavaArray(
            Double::class.java) as DoubleArray
        val result = variable.read(params)
        return factory(latData, lonData, { i, j ->
            (result as ArrayFloat.D4).get(0, 0, i, j).toDouble()
        })
    }
}