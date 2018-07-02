package com.gmail.polikutinevgeny.utility

import java.awt.geom.Point2D

/**
 * Class representing an atmospheric front.
 *
 * Extends ArrayList<Point2D.Double> and has a method for serialization.
 *
 * @param [p0] Collection of front points.
 * @property [temp] Front temperature (>0 - cold, <0 - warm).
 *
 * @author Evgeny Polikutin
 *
 */
class Front(p0: MutableCollection<out Point2D.Double>?, val temp: Double) :
    ArrayList<Point2D.Double>(p0) {

    /**
     * Converts front to CSV representation.
     *
     * @author Evgeny Polikutin
     *
     */
    fun toCSV(): String {
        var s = "${this.temp}, ${this.first().x}, ${this.first().y}"
        for (c in this.asSequence().drop(1)) {
            s += ", ${c.x}, ${c.y}"
        }
        return s
    }
}