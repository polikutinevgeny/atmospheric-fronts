package com.gmail.polikutinevgeny.frontutils

import java.awt.geom.Point2D.Double as Point

class FrontSimplifier {
    private fun perpendicularDistance(pt: Point, lineStart: Point, lineEnd: Point): Double {
        var dx = lineEnd.x - lineStart.x
        var dy = lineEnd.y - lineStart.y

        // Normalize
        val mag = Math.hypot(dx, dy)
        if (mag > 0.0) {
            dx /= mag; dy /= mag
        }
        val pvx = pt.x - lineStart.x
        val pvy = pt.y - lineStart.y

        // Get dot product (project pv onto normalized direction)
        val pvdot = dx * pvx + dy * pvy

        // Scale line direction vector and substract it from pv
        val ax = pvx - pvdot * dx
        val ay = pvy - pvdot * dy

        return Math.hypot(ax, ay)
    }

    fun ramerDouglasPeucker(pointList: List<Point>,
                            epsilon: Double): MutableList<Point> {
        if (pointList.size < 2) throw IllegalArgumentException("Not enough points to simplify")
        val out = mutableListOf<Point>()
        // Find the point with the maximum distance from line between start and end
        var dmax = 0.0
        var index = 0
        val end = pointList.size - 1
        for (i in 1 until end) {
            val d = perpendicularDistance(pointList[i], pointList[0], pointList[end])
            if (d > dmax) {
                index = i; dmax = d
            }
        }

        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            val firstLine = pointList.take(index + 1)
            val lastLine = pointList.drop(index)
            val recResults1 = ramerDouglasPeucker(firstLine, epsilon)
            val recResults2 = ramerDouglasPeucker(lastLine, epsilon)

            // build the result list
            out.addAll(recResults1.take(recResults1.size - 1))
            out.addAll(recResults2)
            if (out.size < 2) throw RuntimeException("Problem assembling output")
        } else {
            // Just return start and end points
            out.clear()
            out.add(pointList.first())
            out.add(pointList.last())
        }

        return out
    }
}