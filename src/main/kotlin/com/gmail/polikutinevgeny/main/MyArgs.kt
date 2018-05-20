package com.gmail.polikutinevgeny.main

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

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
        help = "TFP threshold (K/(100km)^2)"
    ) { toDouble() / 10000.0 }.default(0.75 / 10000.0)

    val gradientThreshold by parser.storing(
        "--gradt",
        help = "gradient threshold (K/100km)"
    ) { toDouble() / 100.0 }.default(1.0 / 100.0)

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
    ) { toInt() }.default(2)

    val lookback by parser.storing(
        "--lookback",
        help = "lookback for fronts curve"
    ) { toInt() }.default(3)

    val minAngle by parser.storing(
        "--minangle",
        help = "minimal possible angle of front line"
    ) { toDouble() }.default(90.0)

    val minLength by parser.storing(
        "--minlength",
        help = "minimal possible front length in kilometers"
    ) { toDouble() }.default(500.0)

    val maskOutput by parser.storing(
        "--maskoutput",
        help = "mask output path"
    ).default("")

    val maskAreasOutput by parser.storing(
        "--maskareasoutput",
        help = "mask areas output path"
    ).default("")
}