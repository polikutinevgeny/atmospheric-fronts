package com.gmail.polikutinevgeny.main

import com.gmail.polikutinevgeny.fields.DoubleArrayField
import com.gmail.polikutinevgeny.files.FileReader
import com.gmail.polikutinevgeny.files.GeoBoundaries
import com.gmail.polikutinevgeny.parameters.HewsonParameterWithVorticity
import com.gmail.polikutinevgeny.ridgedetection.RidgeDetector
import com.gmail.polikutinevgeny.utility.earthDistance
import com.gmail.polikutinevgeny.utility.equivalentPotentialTemperature
import com.gmail.polikutinevgeny.utility.toCSV
import java.io.File

fun main(args: Array<String>) {
    val file = FileReader("/home/polikutin/17.03.2018/gfs.t00z.pgrb2.0p25.anl")
    val temp = file.readIsobaricVariable("Temperature_isobaric", 90000.0,
        GeoBoundaries(20.0, 70.0, 140.0, 210.0),
        DoubleArrayField.Factory::create)
    val uwind = file.readIsobaricVariable("u-component_of_wind_isobaric",
        90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0),
        DoubleArrayField.Factory::create)
    val vwind = file.readIsobaricVariable("v-component_of_wind_isobaric",
        90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0),
        DoubleArrayField.Factory::create)
    val fileb = FileReader(
        "/home/polikutin/17.03.2018/gfs.t00z.pgrb2b.0p25.anl")
    val spechum = fileb.readIsobaricVariable("Specific_humidity_isobaric",
        90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0),
        DoubleArrayField.Factory::create)

    //    val sigma = 0.5
    //    val radius = 2
    //    temp.gaussianBlur(sigma, radius)
    //    uwind.gaussianBlur(sigma, radius)
    //    vwind.gaussianBlur(sigma, radius)
    //    spechum.gaussianBlur(sigma, radius)

    val fp = HewsonParameterWithVorticity(
        equivalentPotentialTemperature(temp, spechum, 900.0), uwind, vwind,
        0.75 / 10000.0, 1.0 / 100.0, 0.5)
    val rd = RidgeDetector(0.36 / 10000.0, 0.0, fp.value, fp.mask)
    //    val fp = HewsonParameterWithVorticity(
    //        equivalentPotentialTemperature(temp, spechum, 900.0), uwind, vwind,
    //        0.25 / 10000.0, 0.5 / 100.0, 0.5)
    //    val rd = RidgeDetector(0.15 / 10000, 0.0, fp.value, fp.mask)

    //    File("/home/polikutin/NCL/fronts.csv").printWriter().use { out ->
    //        rd.detectedFronts.forEach {
    //            out.println(it.toCSV())
    //        }
    //    }
    rd.detectedFronts2.removeAll {
        it.zipWithNext { a, b ->
            earthDistance(a, b)
        }.sum() <= 300 || earthDistance(it.first(), it.last()) <= 300
    }
    File("/home/polikutin/NCL/fronts.csv").printWriter().use { out ->
        rd.detectedFronts2.forEach {
            out.println(it.toCSV())
        }
    }
}