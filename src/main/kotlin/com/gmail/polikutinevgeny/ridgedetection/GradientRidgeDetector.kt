package com.gmail.polikutinevgeny.ridgedetection

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.utility.Front
import com.gmail.polikutinevgeny.utility.gradient
import com.gmail.polikutinevgeny.utility.minus
import com.gmail.polikutinevgeny.utility.vecAngle
import java.awt.Point
import java.awt.geom.Point2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GradientRidgeDetector(field: FieldInterface, mask: FieldInterface,
                            classification: FieldInterface,
                            var upperThreshold: Double,
                            var lowerThreshold: Double,
                            var minAngle: Double, var searchRadius: Int,
                            var lookback: Int, val temp: FieldInterface) :
    RidgeDetector(field, mask, classification) {

    init {
        ridgeDetection()
    }

    override fun ridgeDetection(): MutableList<Front> {
        detectedFronts.clear()
        val sups = mutableListOf<Point>()
        val (xSize, ySize) = field.size
        val ridges = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })

        val walkDirs = directions(searchRadius)
        val dirs = directions(1)

        val count = count(dirs, sups, lowerThreshold, upperThreshold)

        val gradientVec = gradient(temp)

        fun ridgeFilterWalk(
            position: Point,
            prev: Point,
            front: MutableList<Point>
        ): Point {
            val candidates = walkDirs.filter {
                position.x + it.x in 0 until field.size.first &&
                    position.y + it.y in 0 until field.size.second
            }.sortedWith(compareBy(
                { searchRadius - max(abs(it.x), abs(it.y)) },
                {
                    val grad = Point2D.Double(
                        gradientVec.first[position.x + it.x, position.y + it.y],
                        gradientVec.second[position.x + it.x, position.y + it.y])
                    val norm1 = Point2D.Double(-grad.y, grad.x)
                    val norm2 = Point2D.Double(grad.y, -grad.x)
                    min(it.vecAngle(norm1), it.vecAngle(norm2))
                },
                { count[position.x + it.x][position.y + it.y] },
                { field[position.x + it.x, position.y + it.y] }
            )).reversed()
            outer@ for (it in candidates) {
                val i = position.x + it.x
                val j = position.y + it.y
                var correct = true

                val grad = Point2D.Double(gradientVec.first[i, j],
                    gradientVec.second[i, j])
                val norm1 = Point2D.Double(-grad.y, grad.x)
                val norm2 = Point2D.Double(grad.y, -grad.x)
                val a1 = it.vecAngle(norm1)
                val a2 = it.vecAngle(norm2)
                val tmp = a1 <= 50.0 || a2 <= 50.0

                if (ridges[i][j] != 1 && count[i][j] >= 4 &&
                    tmp && correct &&
                    classification[i, j] == classification[position.x, position.y]) {
                    val segment = line(position.x, position.y, i, j)
                    for (p in segment.drop(1)) {
                        if (ridges[p.x][p.y] == 1) {
                            continue@outer
                        }
                    }
                    val thickSegment = thickLine(position.x, position.y, i, j)
                    for (p in thickSegment) {
                        ridges[p.x][p.y] = 1
                    }
                    if ((position - prev).vecAngle(it) == 0.0) {
                        front.removeAt(front.lastIndex)
                    }
                    front.add(Point(i, j))
                    ridgeFilterWalk(Point(i, j), position, front)
                    return Point(i, j)
                }
            }
            return Point(-1, -1)
        }

        for (start in sups.asReversed()) {
            if (ridges[start.x][start.y] > 0) {
                continue
            }
            ridges[start.x][start.y] = 1
            val front = mutableListOf(start)
            val last = ridgeFilterWalk(start, Point(-1, -1), front)
            front.reverse()
            ridgeFilterWalk(start, last, front)
            if (front.size >= 2) {
                detectedFronts.add(Front(front.map {
                    Point2D.Double(field.xCoordinates[it.x],
                        field.yCoordinates[it.y])
                }.toMutableList(), front.first().run { classification[x, y] }))
            }
        }
        return detectedFronts
    }

}