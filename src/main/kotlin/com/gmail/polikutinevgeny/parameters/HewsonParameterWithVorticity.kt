package com.gmail.polikutinevgeny.parameters

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.utility.*
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

    override val value: FieldInterface = temperature.filledClone { i, j ->
        -(temperatureGradientVec.first[i, j] * temperatureGradientGradientVec.first[i, j] +
            temperatureGradientVec.second[i, j] * temperatureGradientGradientVec.second[i, j]) / temperatureGradient[i, j]
    }
    override val mask: FieldInterface
        get() = temperature.filledClone { i, j ->
            if (value[i, j] >= tfpThreshold &&
                temperatureGradient[i, j] + 100 * temperatureGradientGradient[i, j] / sqrt(2.0) >= gradientThreshold &&
                vorticity[i, j] / coriolis(vorticity.xCoordinates[i]) > vorticityThreshold) 1.0 else 0.0
            }
}