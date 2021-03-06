package com.gmail.polikutinevgeny.main

import com.gmail.polikutinevgeny.fields.DoubleArrayField
import com.gmail.polikutinevgeny.files.FileReader
import com.gmail.polikutinevgeny.files.GeoBoundaries
import com.gmail.polikutinevgeny.parameters.HewsonParameterWithVorticity
import com.gmail.polikutinevgeny.ridgedetection.ModifiedRidgeDetector
import com.gmail.polikutinevgeny.utility.earthDistance
import com.gmail.polikutinevgeny.utility.equivalentPotentialTemperature
import com.gmail.polikutinevgeny.utility.maskAreas
import com.gmail.polikutinevgeny.utility.maskToCSVWithClassification
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import java.io.File

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::MyArgs)
    parsedArgs.run {
        val openedfile = FileReader(input)
        val boundaries = GeoBoundaries(minLat, maxLat, minLon, maxLon)
        val temp = openedfile.readIsobaricVariable("Temperature_isobaric",
            isobaric,
            boundaries,
            ::DoubleArrayField)
        val uwind = openedfile.readIsobaricVariable(
            "u-component_of_wind_isobaric", isobaric,
            boundaries,
            ::DoubleArrayField)
        val vwind = openedfile.readIsobaricVariable(
            "v-component_of_wind_isobaric", isobaric,
            boundaries,
            ::DoubleArrayField)
        val spechum = openedfile.readIsobaricVariable(
            "Specific_humidity_isobaric", isobaric,
            boundaries,
            ::DoubleArrayField)
        val fp = HewsonParameterWithVorticity(
            equivalentPotentialTemperature(temp, spechum, isobaric / 100),
            uwind, vwind,
            tfpThreshold, gradientThreshold, vorticityThreshold)

        val rd = ModifiedRidgeDetector(fp.value, fp.mask, fp.classification,
            tfpThreshold * vorticityThreshold,
            tfpThreshold * vorticityThreshold / 2, minAngle, searchRadius,
            lookback)

        rd.detectedFronts.removeAll {
            it.zipWithNext { a, b ->
                earthDistance(a, b)
            }.sum() <= minLength || earthDistance(it.first(),
                it.last()) <= minLength
        }

        File(output).printWriter().use { out ->
            rd.detectedFronts.forEach {
                out.println(it.toCSV())
            }
        }
        if (maskOutput != "") {
            File(maskOutput).printWriter().use { out ->
                out.println(
                    fp.mask.maskToCSVWithClassification(fp.classification))
            }
        }
        if (maskAreasOutput != "") {
            val maskAreas = maskAreas(fp.mask, fp.classification)
            File(maskAreasOutput).printWriter().use { out ->
                maskAreas.forEach {
                    out.println(it.toCSV())
                }
            }
        }

        //        //        val fp = HewsonParameterWithVorticity(
        //        //            temp,
        //        //            uwind, vwind,
        //        //            tfpThreshold, gradientThreshold, vorticityThreshold)
        //        val rd = OldRidgeDetector(tfpThreshold * vorticityThreshold,
        //            tfpThreshold * vorticityThreshold / 2, fp.value,
        //            fp.mask, searchRadius, minAngle, lookback, fp.classification)
        //        //        rd.ridgeDetectionGradient(equivalentPotentialTemperature(temp, spechum, isobaric / 100))
        //
        //        rd.detectedFronts2.removeAll {
        //            it.zipWithNext { a, b ->
        //                earthDistance(a, b)
        //            }.sum() <= minLength || earthDistance(it.first(),
        //                it.last()) <= minLength
        //        }
        //
        //        //        rd.detectedFronts.removeAll {
        //        //            it.zipWithNext { a, b ->
        //        //                earthDistance(a, b)
        //        //            }.sum() <= minLength || earthDistance(it.first(),
        //        //                it.last()) <= minLength
        //        //        }
        //        //
        //        //        rd.detectedFrontsThinning.removeAll {
        //        //            it.zipWithNext { a, b ->
        //        //                earthDistance(a, b)
        //        //            }.sum() <= minLength || earthDistance(it.first(),
        //        //                it.last()) <= minLength
        //        //        }
        //
        //        //        rd.detectedFrontsGradient.removeAll {
        //        //            it.zipWithNext { a, b ->
        //        //                earthDistance(a, b)
        //        //            }.sum() <= minLength || earthDistance(it.first(),
        //        //                it.last()) <= minLength
        //        //        }
        //
        //        File(output).printWriter().use { out ->
        //            //            rd.detectedFronts.forEach {
        //            //                out.println(it.toCSV())
        //            //            }
        //            //            out.println("0.0, 0.0")
        //            //            rd.detectedFrontsThinning.forEach {
        //            //                out.print("1.0, ")
        //            //                out.println(it.toCSV())
        //            //            }
        //            //            out.println("1.0, 1.0")
        //
        //            rd.detectedFronts2.forEach {
        //                out.println(it.toCSV())
        //            }
        //
        //            //            rd.detectedFrontsGradient.forEach {
        //            //                out.println(it.toCSV())
        //            //            }
        //        }
        //
        //        if (maskOutput != "") {
        ////            val ts = ThinningService()
        ////            val m = fp.mask
        ////            val c = fp.classification
        ////            val temp1 = Array(m.xCoordinates.size, { i ->
        ////                IntArray(m.yCoordinates.size,
        ////                    { j -> if (m[i, j] > 0 && c[i, j] > 0) 1 else 0 })
        ////            })
        ////            val temp2 = Array(m.xCoordinates.size, { i ->
        ////                IntArray(m.yCoordinates.size,
        ////                    { j -> if (m[i, j] > 0 && c[i, j] < 0) 1 else 0 })
        ////            })
        ////            val thinned1 = ts.doBSTThinning(temp1)
        ////            val thinned2 = ts.doBSTThinning(temp2)
        ////            val mask = fp.mask.clone { i, j -> thinned1[i][j].toDouble() + thinned2[i][j].toDouble() }
        ////            File(maskOutput).printWriter().use { out ->
        ////                out.println(
        ////                    mask.maskToCSVWithClassification(fp.classification))
        ////            }
        //
        //            File(maskOutput).printWriter().use { out ->
        //                out.println(
        //                    fp.mask.maskToCSVWithClassification(fp.classification))
        //            }
        //        }
        //
        //        if (maskAreasOutput != "") {
        //            val maskAreas = maskAreas(fp.mask, fp.classification)
        //            File(maskAreasOutput).printWriter().use { out ->
        //                maskAreas.forEach {
        //                    out.println(it.toCSV())
        //                }
        //            }
        //        }
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
    //    val rd = OldRidgeDetector(0.36 / 10000.0, 0.0, fp.value, fp.mask)
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
