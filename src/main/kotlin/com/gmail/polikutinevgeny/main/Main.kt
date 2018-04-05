package com.gmail.polikutinevgeny.main

import com.gmail.polikutinevgeny.files.FileReader
import com.gmail.polikutinevgeny.files.GeoBoundaries

fun main(args: Array<String>) {
    val file = FileReader("/home/polikutin/17.03.2018/gfs.t00z.pgrb2.0p25.anl")
    val temp = file.readIsobaricVariable("Temperature_isobaric", 90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0))
    val uwind = file.readIsobaricVariable("u-component_of_wind_isobaric", 90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0))
    val vwind = file.readIsobaricVariable("v-component_of_wind_isobaric", 90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0))
    val fileb = FileReader("/home/polikutin/17.03.2018/gfs.t00z.pgrb2b.0p25.anl")
    val spechum = fileb.readIsobaricVariable("Specific_humidity_isobaric", 90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0))
    println(temp.size)
    println(uwind.size)
    println(vwind.size)
    println(spechum.size)
}