package com.gmail.polikutinevgeny.frontutils

import java.awt.geom.Point2D.Double as Point

class FrontSimplifier {
    private fun perpendicularDistance(pt: Point, lineStart: Point, lineEnd: Point): Double {
        var dx = lineEnd.x - lineStart.x
        var dy = lineEnd.y - lineStart.y
        val mag = Math.hypot(dx, dy)
        if (mag > 0.0) {
            dx /= mag; dy /= mag
        }
        val pvx = pt.x - lineStart.x
        val pvy = pt.y - lineStart.y
        val pvdot = dx * pvx + dy * pvy
        val ax = pvx - pvdot * dx
        val ay = pvy - pvdot * dy
        return Math.hypot(ax, ay)
    }

    fun ramerDouglasPeucker(pointList: List<Point>,
                            epsilon: Double): MutableList<Point> {
        if (pointList.size < 2) return pointList.toMutableList()
        val out = mutableListOf<Point>()
        var dmax = 0.0
        var index = 0
        val end = pointList.size - 1
        for (i in 1 until end) {
            val d = perpendicularDistance(pointList[i], pointList[0], pointList[end])
            if (d > dmax) {
                index = i; dmax = d
            }
        }
        if (dmax > epsilon) {
            val firstLine = pointList.take(index + 1)
            val lastLine = pointList.drop(index)
            val recResults1 = ramerDouglasPeucker(firstLine, epsilon)
            val recResults2 = ramerDouglasPeucker(lastLine, epsilon)
            out.addAll(recResults1.take(recResults1.size - 1))
            out.addAll(recResults2)
            if (out.size < 2) throw RuntimeException("Problem assembling output")
        } else {
            out.clear()
            out.add(pointList.first())
            out.add(pointList.last())
        }
        return out
    }
}