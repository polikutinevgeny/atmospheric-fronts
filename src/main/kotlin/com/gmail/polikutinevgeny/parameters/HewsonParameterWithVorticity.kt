package com.gmail.polikutinevgeny.parameters

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.utility.absValue
import com.gmail.polikutinevgeny.utility.coriolis
import com.gmail.polikutinevgeny.utility.gradient
import com.gmail.polikutinevgeny.utility.vecVorticity
import kotlin.math.sqrt

class HewsonParameterWithVorticity(
    private val temperature: FieldInterface,
    private val uWind: FieldInterface,
    private val vWind: FieldInterface,
    var tfpThreshold: Double,
    var gradientThreshold: Double,
    var vorticityThreshold: Double) : ParameterInterface {

    private val vorticity: FieldInterface = vecVorticity(uWind, vWind)
    private val temperatureGradientVec: Pair<FieldInterface, FieldInterface> = gradient(
        temperature)
    private val temperatureGradient: FieldInterface = absValue(
        temperatureGradientVec)
    private val temperatureGradientGradientVec: Pair<FieldInterface, FieldInterface> = gradient(
        temperatureGradient)
    private val temperatureGradientGradient: FieldInterface = absValue(
        temperatureGradientGradientVec)

    override val value: FieldInterface = temperature.clone { i, j ->
        -(temperatureGradientVec.first[i, j] * temperatureGradientGradientVec.first[i, j] +
            temperatureGradientVec.second[i, j] * temperatureGradientGradientVec.second[i, j]) / temperatureGradient[i, j] * vorticity[i, j] / coriolis(
            vorticity.xCoordinates[i])
    }
    override val mask: FieldInterface
        get() = temperature.clone { i, j ->
            if (value[i, j] >= tfpThreshold &&
                temperatureGradient[i, j] + 100 * temperatureGradientGradient[i, j] / sqrt(2.0) >= gradientThreshold &&
                vorticity[i, j] / coriolis(vorticity.xCoordinates[i]) > vorticityThreshold) 1.0 else 0.0
            }

    val classification: FieldInterface
        get() = temperature.clone { i, j ->
            //WARNING: are u/v directions right?
            val a = -(vWind[i, j] * temperatureGradientVec.first[i, j] + uWind[i, j] * temperatureGradientVec.second[i, j])
            if (a > 0) 1.0 else -1.0
        }
}