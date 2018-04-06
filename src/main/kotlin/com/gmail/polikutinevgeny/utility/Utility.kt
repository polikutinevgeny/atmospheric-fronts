package com.gmail.polikutinevgeny.utility

import com.gmail.polikutinevgeny.fields.FieldInterface
import java.awt.geom.Point2D
import kotlin.math.*

object Constants {
    /**
     * Mean radius of the Earth in kilometers.
     *
     * */
    const val EARTH_MEAN_RADIUS: Double = 6371.0

    /**
     * Scale for the temperature gradient.
     *
     * @see <a href="http://dx.doi.org/10.1002/2017GL073662">
     *     A simple diagnostic for the detection of atmospheric fronts<\a>
     *
     * */
    const val TEMP_GRADIENT_SCALE: Double = 0.45 / 100

    /**
     * Earth rotation rate.
     *
     *
     * */
    const val EARTH_ROTATION_RATE: Double = 7.2921e-5
}

fun equivalentPotentialTemperature(temperature: FieldInterface,
                                   specHumidity: FieldInterface,
                                   pressure: Double): FieldInterface {
    return temperature.clone { i, j ->
        val e = pressure / (622 + specHumidity[i, j])
        val tl = 2840.0 / (3.5 * ln(temperature[i, j]) - ln(
            e) - 4.805) + 55.0
        temperature[i, j] * (1000.0 / pressure).pow(
            0.2854 * (1.0 - 0.28 * 0.001 * specHumidity[i, j])) *
            exp((3.376 / tl - 0.00254) * specHumidity[i, j] * (1 + 0.81 * 0.001 * specHumidity[i, j]))
    }
}

/**
 * Angle distance between points on a sphere.
 *
 * Calculates angle distance between points on a sphere using the haversine formula.
 * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>
 *
 * @param [from] First point (lat, lon) in degrees.
 * @param [to] Second point (lat, lon) in degrees.
 *
 * @author Evgeny Polikutin
 *
 * */
private fun sphereAngleDistance(from: Pair<Double, Double>,
                                to: Pair<Double, Double>): Double {
    val (lat1, lon1) = from
    val (lat2, lon2) = to
    val l1 = Math.toRadians(lat1)
    val l2 = Math.toRadians(lat2)
    val dlat = Math.toRadians(lat2 - lat1)
    val dlon = Math.toRadians(lon2 - lon1)
    return 2 * asin(
        sqrt(sin(dlat / 2).pow(2) + sin(dlon / 2).pow(2) * cos(l1) * cos(l2)))
}

/**
 * Distance between points on the Earth.
 *
 * Calculates distance between points on a sphere using the haversine formula.
 * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>
 *
 * @param [from] First point (lat, lon) in degrees.
 * @param [to] Second point (lat, lon) in degrees.
 *
 * @author Evgeny Polikutin
 *
 * */
fun earthDistance(from: Pair<Double, Double>,
                  to: Pair<Double, Double>): Double {
    return Constants.EARTH_MEAN_RADIUS * sphereAngleDistance(from, to)
}

/**
 * Angle between p1-p2 and p1-p3 on a sphere.
 *
 * Calculates angle between segments on a sphere.
 * @see <a href="http://gis-lab.info/qa/angles-sphere.html">Angle on a sphere</a>
 *
 * @param [p1] First point, angle is calculated here
 * @param [p2] Second point
 * @param [p3] Third point
 *
 * @author Evgeny Polikutin
 *
 * */
fun sphereAngle(p1: Pair<Double, Double>, p2: Pair<Double, Double>,
                p3: Pair<Double, Double>): Double {
    val a = sphereAngleDistance(p2, p3)
    val b = sphereAngleDistance(p1, p3)
    val c = sphereAngleDistance(p1, p2)
    return Math.toDegrees(
        acos((cos(a) - cos(b) * cos(c)) / (sin(b) * sin(c))))
}

/**
 * Coriolis parameter.
 *
 * Calculates Coriolis parameter at given latitude on the Earth.
 *
 * @param [lat] Latitude.
 *
 * @author Evgeny Polikutin
 *
 * */
fun coriolis(lat: Double): Double {
    return 2 * Constants.EARTH_ROTATION_RATE * sin(Math.toRadians(lat))
}

/**
 * Vector of gradient of a scalar field on the Earth surface.
 *
 * Calculates the gradient of a scalar field on the Earth surface. Input data are assumed to have correct dimensions.
 *
 * @param [field] 2D array [lat, lon] containing the scalar field.
 *
 * @author Evgeny Polikutin
 *
 * */
fun gradient(field: FieldInterface): Pair<FieldInterface, FieldInterface> {
    val (xSize, ySize) = field.size
    val resultLat = field.clone()
    val resultLon = field.clone()
    val latitude = field.xCoordinates
    val longitude = field.yCoordinates
    for (i in 1..xSize - 2) {
        for (j in 1..ySize - 2) {
            // Minus is to change the direction
            resultLat[i, j] = -(field[i + 1, j] - field[i - 1, j]) /
                earthDistance(
                    Pair(latitude[i + 1], longitude[j]),
                    Pair(latitude[i - 1], longitude[j]))
            resultLon[i, j] = (field[i, j + 1] - field[i, j - 1]) /
                earthDistance(
                    Pair(latitude[i], longitude[j + 1]),
                    Pair(latitude[i], longitude[j - 1]))
        }
    }
    // Here goes calculation in the edge points via directed diffs. It is not pretty.
    for (i in 0 until xSize) {
        val l = ySize - 1
        if (i != 0 && i != xSize - 1) {
            resultLat[i, 0] = -(field[i + 1, 0] - field[i - 1, 0]) /
                earthDistance(
                    Pair(latitude[i + 1], longitude[0]),
                    Pair(latitude[i - 1], longitude[0]))
            resultLat[i, l] = -(field[i + 1, l] - field[i - 1, l]) /
                earthDistance(
                    Pair(latitude[i + 1], longitude[l]),
                    Pair(latitude[i - 1], longitude[l]))
        }
        resultLon[i, 0] = (field[i, 1] - field[i, 0]) /
            earthDistance(Pair(latitude[i], longitude[1]),
                Pair(latitude[i], longitude[0]))
        resultLon[i, l] = (field[i, l] - field[i, l - 1]) /
            earthDistance(Pair(latitude[i], longitude[l]),
                Pair(latitude[i], longitude[l - 1]))
    }
    for (j in 0 until ySize) {
        val l = xSize - 1
        resultLat[0, j] = -(field[1, j] - field[0, j]) /
            earthDistance(
                Pair(latitude[1], longitude[j]),
                Pair(latitude[0], longitude[j]))
        resultLat[l, j] = -(field[l, j] - field[l - 1, j]) /
            earthDistance(
                Pair(latitude[l], longitude[j]),
                Pair(latitude[l - 1], longitude[j]))
        if (j != 0 && j != ySize - 1) {
            resultLon[l, j] = (field[l, j + 1] - field[l, j - 1]) /
                earthDistance(
                    Pair(latitude[l], longitude[j + 1]),
                    Pair(latitude[l], longitude[j - 1]))
            resultLon[0, j] = (field[0, j + 1] - field[0, j - 1]) /
                earthDistance(
                    Pair(latitude[0], longitude[j + 1]),
                    Pair(latitude[0], longitude[j - 1]))
        }
    }
    return Pair(resultLat, resultLon)
}

fun absValue(vecField: Pair<FieldInterface, FieldInterface>): FieldInterface {
    val (lat, lon) = vecField
    val result = lat.clone()
    val (xSize, ySize) = result.size
    for (i in 0 until xSize) {
        for (j in 0 until ySize) {
            result[i, j] = sqrt(lat[i, j].pow(2) + lon[i, j].pow(2))
        }
    }
    return result
}

/**
 * Absolute value of gradient of a scalar field on the Earth surface.
 *
 * Calculates the gradient of a scalar field on the Earth surface. Input data are assumed to have correct dimensions.
 *
 * @param [field] 2D array [lat, lon] containing the scalar field.
 *
 * @author Evgeny Polikutin
 *
 * */
fun gradientAbs(field: FieldInterface): FieldInterface =
    absValue(gradient(field))

/**
 * Vorticity of a vector field on the Earth surface.
 *
 * Calculates the relative vorticity of a vector field on the Earth surface.
 * Input data are assumed to have correct dimensions.
 *
 * @param [ufield] 2D array [lat, lon] containing the u-component field.
 * @param [vfield] 2D array [lat, lon] containing the v-component field.
 *
 * @author Evgeny Polikutin
 *
 * */
fun vecVorticity(ufield: FieldInterface,
                      vfield: FieldInterface): FieldInterface {
    val result = ufield.clone()
    val (latu, _) = gradient(ufield)
    val (_, lonv) = gradient(vfield)
    val (xSize, ySize) = result.size
    for (i in 0 until xSize) {
        for (j in 0 until ySize) {
            result[i, j] = (lonv[i, j] - latu[i, j] + ufield[i, j] / Constants.EARTH_MEAN_RADIUS * tan(
                Math.toRadians(result.xCoordinates[i]))) / 1000
        }
    }
    return result
}

typealias Front = MutableList<Point2D.Double>

fun Front.toCSV(): String {
    var s = "${this.first().x}, ${this.first().y}"
    for (c in this.asSequence().drop(1)) {
        s += ", ${c.x}, ${c.y}"
    }
    return s
}