package com.gmail.polikutinevgeny.main

import com.gmail.polikutinevgeny.fields.DoubleArrayField
import com.gmail.polikutinevgeny.files.FileReader
import com.gmail.polikutinevgeny.files.GeoBoundaries
import com.gmail.polikutinevgeny.parameters.HewsonParameterWithVorticity
import com.gmail.polikutinevgeny.ridgedetection.RidgeDetector
import com.gmail.polikutinevgeny.utility.earthDistance
import com.gmail.polikutinevgeny.utility.equivalentPotentialTemperature
import com.gmail.polikutinevgeny.utility.toCSV
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.File

class MyArgs(parser: ArgParser) {
    val input by parser.storing(
        "-i", "--input",
        help = "input file")

    val isobaric by parser.storing(
        "--isobaric",
        help = "isobaric surface"
    ) { toDouble() }
        .default(90000.0)

    val minLat by parser.storing(
        "--minlat",
        help = "minimum latitude"
    ) { toDouble() }

    val minLon by parser.storing(
        "--minlon",
        help = "minimum longitude"
    ) { toDouble() }

    val maxLat by parser.storing(
        "--maxlat",
        help = "maximum latitude"
    ) { toDouble() }

    val maxLon by parser.storing(
        "--maxlon",
        help = "maximum longitude"
    ) { toDouble() }

    val tfpThreshold by parser.storing(
        "--tfpt",
        help = "TFP threshold"
    ) { toDouble() }.default(0.75 / 10000)

    val gradientThreshold by parser.storing(
        "--gradt",
        help = "gradient threshold"
    ) { toDouble() }.default(1.0 / 100.0)

    val vorticityThreshold by parser.storing(
        "--vortt",
        help = "vorticity threshold"
    ) { toDouble() }.default(0.5)

    val output by parser.storing(
        "-o", "--output",
        help = "output input"
    )

    val searchRadius by parser.storing(
        "--searchradius",
        help = "search radius for fronts"
    ) { toInt() }.default(3)

    val minAngle by parser.storing(
        "--minAngle",
        help = "minimal possible angle of front line"
    ) { toDouble() }.default(90.0)
}

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::MyArgs)
    parsedArgs.run {
        val openedfile = FileReader(input)
        val temp = openedfile.readIsobaricVariable("Temperature_isobaric",
            isobaric,
            GeoBoundaries(minLat, maxLat, minLon, maxLon),
            DoubleArrayField.Factory::create)
        val uwind = openedfile.readIsobaricVariable(
            "u-component_of_wind_isobaric", isobaric,
            GeoBoundaries(minLat, maxLat, minLon, maxLon),
            DoubleArrayField.Factory::create)
        val vwind = openedfile.readIsobaricVariable(
            "v-component_of_wind_isobaric", isobaric,
            GeoBoundaries(minLat, maxLat, minLon, maxLon),
            DoubleArrayField.Factory::create)
        val spechum = openedfile.readIsobaricVariable(
            "Specific_humidity_isobaric", isobaric,
            GeoBoundaries(minLat, maxLat, minLon, maxLon),
            DoubleArrayField.Factory::create)
        val fp = HewsonParameterWithVorticity(
            equivalentPotentialTemperature(temp, spechum, isobaric / 100),
            uwind, vwind,
            tfpThreshold, gradientThreshold, vorticityThreshold)
        val rd = RidgeDetector(tfpThreshold * vorticityThreshold, 0.0, fp.value,
            fp.mask, searchRadius, minAngle)
        rd.detectedFronts2.removeAll {
            it.zipWithNext { a, b ->
                earthDistance(a, b)
            }.sum() <= 300 || earthDistance(it.first(), it.last()) <= 300
        }
        File(output).printWriter().use { out ->
            rd.detectedFronts2.forEach {
                out.println(it.toCSV())
            }
        }
    }
    //    val input = FileReader("/home/polikutin/17.03.2018/gfs.t00z.pgrb2.0p25.anl")
    //    val temp = input.readIsobaricVariable("Temperature_isobaric", 90000.0,
    //        GeoBoundaries(20.0, 70.0, 140.0, 210.0),
    //        DoubleArrayField.Factory::create)
    //    val uwind = input.readIsobaricVariable("u-component_of_wind_isobaric",
    //        90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0),
    //        DoubleArrayField.Factory::create)
    //    val vwind = input.readIsobaricVariable("v-component_of_wind_isobaric",
    //        90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0),
    //        DoubleArrayField.Factory::create)
    //    val fileb = FileReader(
    //        "/home/polikutin/17.03.2018/gfs.t00z.pgrb2b.0p25.anl")
    //    val spechum = fileb.readIsobaricVariable("Specific_humidity_isobaric",
    //        90000.0, GeoBoundaries(20.0, 70.0, 140.0, 210.0),
    //        DoubleArrayField.Factory::create)
    //
    //    //    val sigma = 0.5
    //    //    val radius = 2
    //    //    temp.gaussianBlur(sigma, radius)
    //    //    uwind.gaussianBlur(sigma, radius)
    //    //    vwind.gaussianBlur(sigma, radius)
    //    //    spechum.gaussianBlur(sigma, radius)
    //
    //    val fp = HewsonParameterWithVorticity(
    //        equivalentPotentialTemperature(temp, spechum, 900.0), uwind, vwind,
    //        0.75 / 10000.0, 1.0 / 100.0, 0.5)
    //    val rd = RidgeDetector(0.36 / 10000.0, 0.0, fp.value, fp.mask)
    //    rd.detectedFronts2.removeAll {
    //        it.zipWithNext { a, b ->
    //            earthDistance(a, b)
    //        }.sum() <= 300 || earthDistance(it.first(), it.last()) <= 300
    //    }
    //    File("/home/polikutin/NCL/fronts.csv").printWriter().use { out ->
    //        rd.detectedFronts2.forEach {
    //            out.println(it.toCSV())
    //        }
    //    }
}