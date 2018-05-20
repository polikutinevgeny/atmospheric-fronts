package com.gmail.polikutinevgeny.ridgedetection

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.utility.Front
import kotlin.math.abs
import kotlin.math.sqrt
import java.awt.Point as IntPoint
import java.awt.geom.Point2D.Double as Point

abstract class RidgeDetector(val field: FieldInterface,
                             var mask: FieldInterface,
                             val classification: FieldInterface) {

    protected companion object {
        @JvmStatic
        protected fun directions(radius: Int): MutableList<IntPoint> {
            val dirs = mutableListOf<IntPoint>()
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    if (dx != 0 || dy != 0) {
                        dirs.add(IntPoint(dx, dy))
                    }
                }
            }
            return dirs
        }

        @JvmStatic
        protected fun line(x0: Int, y0: Int,
                           x1: Int, y1: Int): MutableList<IntPoint> {
            var x0 = x0
            var y0 = y0
            val dx = abs(x1 - x0)
            val sx = if (x0 < x1) 1 else -1
            val dy = abs(y1 - y0)
            val sy = if (y0 < y1) 1 else -1
            var err = (if (dx > dy) dx else -dy) / 2
            var e2: Int
            val result = mutableListOf<IntPoint>()
            while (true) {
                result.add(IntPoint(x0, y0))
                if (x0 == x1 && y0 == y1) break
                e2 = err
                if (e2 > -dx) {
                    err -= dy
                    x0 += sx
                }
                if (e2 < dy) {
                    err += dx
                    y0 += sy
                }
            }
            return result
        }

        @JvmStatic
        protected fun thickLine(x0: Int, y0: Int,
                                x1: Int, y1: Int): MutableList<IntPoint> {
            var x0 = x0
            var y0 = y0
            val dx = abs(x1 - x0)
            val sx = if (x0 < x1) 1 else -1
            val dy = abs(y1 - y0)
            val sy = if (y0 < y1) 1 else -1
            var err = dx - dy
            var e2: Int
            var x2: Int
            val ed = if (dx + dy == 0) 1.0 else sqrt(
                dx * dx.toDouble() + dy * dy.toDouble())
            val result = mutableListOf<IntPoint>()
            while (true) {
                result.add(IntPoint(x0, y0))
                e2 = err
                x2 = x0
                if (2 * e2 >= -dx) {
                    if (x0 == x1) break
                    if (e2 + dy < ed)
                        result.add(IntPoint(x0, y0 + sy))
                    err -= dy
                    x0 += sx
                }
                if (2 * e2 <= dy) {
                    if (y0 == y1) break
                    if (dx - e2 < ed)
                        result.add(IntPoint(x2 + sx, y0))
                    err += dx
                    y0 += sy
                }
            }
            return result
        }
    }

    val detectedFronts: MutableList<Front> = mutableListOf()

    abstract fun ridgeDetection(): MutableList<Front>

    fun isSuperior(position: IntPoint, shift: IntPoint): Boolean {
        return field[position.x, position.y] > field[position.x + shift.x, position.y + shift.y]
    }

    protected fun indexToCoords(input: IntPoint) =
        Point(field.xCoordinates[input.x], field.yCoordinates[input.y])

    protected fun indexToCoords(i: Int, j: Int) = indexToCoords(IntPoint(i, j))

    protected fun count(dirs: MutableList<IntPoint>,
                        sups: MutableList<IntPoint>,
                        lowerThreshold: Double,
                        upperThreshold: Double): Array<IntArray> {
        val (xSize, ySize) = field.size
        val count = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })
        for (i in 1..xSize - 2) {
            for (j in 1..ySize - 2) {
                if (mask[i, j] < 1.0) {
                    continue
                }
                val position = java.awt.Point(i, j)
                count[i][j] = dirs.count { isSuperior(position, it) }
                if (count[i][j] == 5) {
                    /**
                     * A H G
                     * B X F
                     * C D E
                     * */
                    for (shift in 0 until 8) {
                        val h = dirs[(7 + shift) % 8]
                        val a = dirs[(0 + shift) % 8]
                        val b = dirs[(1 + shift) % 8]
                        val c = dirs[(2 + shift) % 8]
                        if (!(!isSuperior(position, a) &&
                                !isSuperior(position, b) &&
                                isSuperior(position, h) &&
                                isSuperior(position, c))) {
                            count[i][j] = 0
                        }
                    }
                }
                if (count[i][j] == 4) {
                    /**
                     * A H G
                     * B X F
                     * C D E
                     * */
                    for (shift in 0 until 8) {
                        val a = dirs[(0 + shift) % 8]
                        val b = dirs[(1 + shift) % 8]
                        val d = dirs[(3 + shift) % 8]
                        val e = dirs[(4 + shift) % 8]
                        val f = dirs[(5 + shift) % 8]
                        val g = dirs[(6 + shift) % 8]
                        if (!isSuperior(position, a) &&
                            !isSuperior(position, b) && ((
                                !isSuperior(position, d) &&
                                    !isSuperior(position, e)) || (
                                !isSuperior(position, e) &&
                                    !isSuperior(position, f)) || (
                                !isSuperior(position, f) &&
                                    !isSuperior(position, g)
                                ))) continue
                        count[i][j] = 0
                    }
                }
                if (field[i, j] < lowerThreshold || count[i][j] < 4) {
                    count[i][j] = 0
                }
                if (count[i][j] == 8 && field[i, j] >= upperThreshold) {
                    sups += position
                }
            }
        }
        return count
    }
}
