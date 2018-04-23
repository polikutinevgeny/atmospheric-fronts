package com.gmail.polikutinevgeny.ridgedetection

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.frontutils.FrontSimplifier
import com.gmail.polikutinevgeny.utility.*
import kotlin.math.*
import java.awt.Point as IntPoint
import java.awt.geom.Point2D.Double as Point

private fun IntPoint.vecAngle(other: IntPoint): Double {
    val n = (this.x * other.x + this.y * other.y).toDouble()
    val d = sqrt((this.x.toDouble().pow(2) + this.y.toDouble().pow(
        2)) * (other.x.toDouble().pow(2) + other.y.toDouble().pow(2)))
    return Math.toDegrees(acos(n / d))
}

private fun IntPoint.vecAngle(other: Point): Double {
    val n = (this.x * other.x + this.y * other.y)
    val d = sqrt((this.x.toDouble().pow(2) + this.y.toDouble().pow(
        2)) * (other.x.pow(2) + other.y.pow(2)))
    return Math.toDegrees(acos(n / d))
}

private operator fun IntPoint.minus(other: IntPoint): IntPoint {
    return IntPoint(this.x - other.x, this.y - other.y)
}

class RidgeDetector(var upperThreshold: Double, var lowerThreshold: Double,
                    val field: FieldInterface, var mask: FieldInterface,
                    var searchRadius: Int, var minAngle: Double,
                    var lookback: Int, var classification: FieldInterface) {
    companion object {
        /**
         * Moving directions (counterclockwise!)
         *
         *
         * */
        private val DIRECTIONS = arrayOf(
            IntPoint(-1, -1), IntPoint(0, -1), IntPoint(1, -1), IntPoint(1, 0),
            IntPoint(1, 1),
            IntPoint(0, 1), IntPoint(-1, 1), IntPoint(-1, 0)
        )
    }

    val detectedFronts: MutableList<MutableList<Point>> = mutableListOf()
    val detectedFronts2: MutableList<Front> = mutableListOf()
    val detectedFrontsThinning: MutableList<MutableList<Point>> = mutableListOf()
    val detectedFrontsGradient: MutableList<Front> = mutableListOf()

    init {
        //        ridgeDetection()
        ridgeDetection2()
        //        val ts = ThinningService()
        //        val thinned = ts.doBSTThinning(Array(field.xCoordinates.size, { i ->
        //            IntArray(field.yCoordinates.size,
        //                { j -> if (mask[i, j] > 0) 1 else 0 })
        //        }))
        //        frontWalk(thinned)
    }

    @Deprecated("Needed for thinning. Use normal algorithms.")
    fun frontWalk(input: Array<IntArray>) {
        val dirs = arrayOf(
            Pair(0, -1), Pair(1, 0), Pair(0, 1), Pair(-1, 0), Pair(-1, 1),
            Pair(-1, -1), Pair(1, -1), Pair(1, 1)
        )
        val temp = Array(input.size) { input[it].clone() }

        fun adjCount(x: Int, y: Int): Int {
            return dirs.count count@{
                if (!(x + it.first in 0..temp.lastIndex && y + it.second in 0..temp[0].lastIndex)) {
                    return@count false
                }
                return@count temp[x + it.first][y + it.second] == 1
            }
        }

        fun frontWalkRec(x: Int, y: Int, line: MutableList<Point>) {
            temp[x][y] = 0
            line.add(Point(field.xCoordinates[x], field.yCoordinates[y]))
            val n = adjCount(x, y)
            for ((dx, dy) in dirs) {
                if (!(x + dx in 0..temp.lastIndex && y + dy in 0..temp[0].lastIndex) || temp[x + dx][y + dy] == 0) {
                    continue
                }
                if (n == 1) {
                    frontWalkRec(x + dx, y + dy, line)
                    return
                }
                if (n > 1) {
                    val l = mutableListOf(
                        Point(field.xCoordinates[x], field.yCoordinates[y]))
                    frontWalkRec(x + dx, y + dy, l)
                    detectedFrontsThinning.add(l)
                }
            }
        }

        for (i in 0 until temp.size) {
            for (j in 0 until temp[i].size) {
                if (temp[i][j] == 1 && adjCount(i, j) == 1) {
                    val line = mutableListOf<Point>()
                    frontWalkRec(i, j, line)
                    detectedFrontsThinning.add(line)
                }
            }
        }
    }

    fun ridgeDetection2() {
        detectedFronts2.clear()
        val sups = mutableListOf<IntPoint>()
        val (xSize, ySize) = field.size
        val count = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })
        val ridges = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })
        val endPoints = Array(xSize,
            { _ -> Array<MutableList<IntPoint>?>(ySize, { _ -> null }) })

        val dirs = mutableListOf<IntPoint>()
        for (dx in -searchRadius..searchRadius) {
            for (dy in -searchRadius..searchRadius) {
                if (dx != 0 || dy != 0) {
                    dirs.add(IntPoint(dx, dy))
                }
            }
        }

        fun line(x0: Int, y0: Int, x1: Int, y1: Int): MutableList<IntPoint> {
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

        fun thickLine(x0: Int, y0: Int, x1: Int,
                      y1: Int): MutableList<IntPoint> {
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

        /**
         * Checks value superiority
         *
         * Checks if the value at position is greater than value at position + shift
         * @see <a href="http://dx.doi.org/10.1017/S1350482702003092">
         *     Use of a simple pattern recognition approach for the detection of ridge lines and stripes<\a>
         *
         * @param [field] Field where superiority is checked
         * @param [position] Position of point that is (or not) superior
         * @param [shift] Relative shift of position
         *
         * @author Polikutin Evgeny
         *
         * */
        fun isSuperior(position: IntPoint, shift: IntPoint): Boolean {
            return field[position.x, position.y] > field[position.x + shift.x, position.y + shift.y]
        }

        fun indexToCoords(input: IntPoint) =
            Pair(field.xCoordinates[input.x], field.yCoordinates[input.y])

        fun indexToCoords(i: Int, j: Int) = indexToCoords(IntPoint(i, j))

        /**
         * A supplementary function for [ridgeFilter].
         *
         * Performs a recursive walk over field
         * @see <a href="http://dx.doi.org/10.1017/S1350482702003092">
         *     Use of a simple pattern recognition approach for the detection of ridge lines and stripes<\a>
         *
         * @param [position] Current position
         * @param [prev] Previous position
         *
         * @author Polikutin Evgeny
         * */
        fun ridgeFilterWalk(
            position: IntPoint,
            prev: IntPoint,
            front: MutableList<IntPoint>
        ): IntPoint {
            val candidates = dirs.filter {
                position.x + it.x in 0 until field.size.first &&
                    position.y + it.y in 0 until field.size.second
            }.sortedWith(compareBy(
                { searchRadius - max(abs(it.x), abs(it.y)) },
                {
                    if (prev == IntPoint(-1, -1))
                        0
                    else
                        -it.vecAngle(prev - position)
                },
                { count[position.x + it.x][position.y + it.y] },
                { field[position.x + it.x, position.y + it.y] }
            )).reversed()
            outer@ for (it in candidates) {
                val i = position.x + it.x
                val j = position.y + it.y
                var correct = true
                for (off in 1..lookback) {
                    if (front.lastIndex < off || !correct) {
                        break
                    }
                    correct = correct && it.vecAngle(
                        front[front.lastIndex - off] - front[front.lastIndex - off + 1]) >= minAngle
                }
                if (ridges[i][j] != 1 && count[i][j] >= 4 &&
                    (prev - position).vecAngle(it) >= minAngle && correct &&
                    classification[i, j] == classification[position.x, position.y]) {
                    val segment = thickLine(position.x, position.y, i, j)
                    for (p in segment.drop(1)) {
                        if (ridges[p.x][p.y] == 1) {
                            continue@outer
                        }
                    }
                    val thickSegment = line(position.x, position.y, i, j)
                    for (p in thickSegment) {
                        ridges[p.x][p.y] = 1
                    }
                    if (prev != IntPoint(-1, -1) && (position - prev).vecAngle(
                            it) == 10.0) {
                        front.removeAt(front.lastIndex)
                    }
                    front.add(IntPoint(i, j))
                    ridgeFilterWalk(IntPoint(i, j), position, front)
                    return IntPoint(i, j)
                }
            }
            if (prev != IntPoint(-1, -1)) {
                endPoints[prev.x][prev.y] = front
            }
            return IntPoint(-1, -1)
        }

        for (i in 1..xSize - 2) {
            for (j in 1..ySize - 2) {
                if (mask[i, j] < 1.0) {
                    continue
                }
                val position = IntPoint(i, j)
                count[i][j] = DIRECTIONS.count { isSuperior(position, it) }
                if (count[i][j] == 5) {
                    /**
                     * A H G
                     * B X F
                     * C D E
                     * */
                    for (shift in 0 until 8) {
                        val h = DIRECTIONS[(7 + shift) % 8]
                        val a = DIRECTIONS[(0 + shift) % 8]
                        val b = DIRECTIONS[(1 + shift) % 8]
                        val c = DIRECTIONS[(2 + shift) % 8]
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
                        val a = DIRECTIONS[(0 + shift) % 8]
                        val b = DIRECTIONS[(1 + shift) % 8]
                        val d = DIRECTIONS[(3 + shift) % 8]
                        val e = DIRECTIONS[(4 + shift) % 8]
                        val f = DIRECTIONS[(5 + shift) % 8]
                        val g = DIRECTIONS[(6 + shift) % 8]
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
        val fs = FrontSimplifier()
        val eps = 0.0
        for (start in sups.asReversed()) {
            if (ridges[start.x][start.y] > 0) {
                continue
            }
            ridges[start.x][start.y] = 1
            val front = mutableListOf(start)
            val last = ridgeFilterWalk(start, IntPoint(-1, -1), front)
            front.reverse()
            ridgeFilterWalk(start, last, front)
            if (front.size >= 2) {
                detectedFronts2.add(Front(front.map {
                    Point(field.xCoordinates[it.x], field.yCoordinates[it.y])
                }.toMutableList(), front.first().run { classification[x, y] }))
            }
        }
    }

    @Deprecated("Old ridge detection algo, generates discontinuities.")
    fun ridgeDetection() {
        detectedFronts.clear()
        val sups = mutableListOf<IntPoint>()
        val (xSize, ySize) = field.size
        val count = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })
        val ridges = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })

        /**
         * Checks value superiority
         *
         * Checks if the value at position is greater than value at position + shift
         * @see <a href="http://dx.doi.org/10.1017/S1350482702003092">
         *     Use of a simple pattern recognition approach for the detection of ridge lines and stripes<\a>
         *
         * @param [field] Field where superiority is checked
         * @param [position] Position of point that is (or not) superior
         * @param [shift] Relative shift of position
         *
         * @author Polikutin Evgeny
         *
         * */
        fun isSuperior(position: IntPoint, shift: IntPoint): Boolean {
            return field[position.x, position.y] > field[position.x + shift.x, position.y + shift.y]
        }

        fun indexToCoords(input: IntPoint) =
            Point(field.xCoordinates[input.x], field.yCoordinates[input.y])

        fun indexToCoords(i: Int, j: Int) = indexToCoords(IntPoint(i, j))

        /**
         * A supplementary function for [ridgeFilter].
         *
         * Performs a recursive walk over field
         * @see <a href="http://dx.doi.org/10.1017/S1350482702003092">
         *     Use of a simple pattern recognition approach for the detection of ridge lines and stripes<\a>
         *
         * @param [field] Field where ridges are selected
         * @param [count] Derived from field. Shows the number of neighbours
         * supporting the ridge line hypothesis at a point
         * @param [ridges] Array the ridges are put into
         * @param [position] Current position
         * @param [prev] Previous position
         *
         * @author Polikutin Evgeny
         * */
        fun ridgeFilterWalk(
            position: IntPoint,
            prev: IntPoint,
            front: MutableList<Point>
        ): IntPoint {
            val candidates = DIRECTIONS.sortedWith(
                compareBy(
                    { count[position.x + it.y][position.x + it.y] },
                    { field[position.x + it.y, position.x + it.y] }
                )
            ).reversed()
            for ((dx, dy) in candidates) {
                val i = position.x + dx
                val j = position.y + dy
                // Drop sharp direction changes (< 45 degrees). Also excludes previous position
                //                if (abs(i - prev.first) + abs(j - prev.second) <= 1 || count[i][j] < 4 || ridges[i][j] > 0) {
                //                    continue
                //                }
                // TODO region Change to planar condition
                if (prev != IntPoint(-1, -1) &&
                    (
                        i !in 1 until xSize - 1 ||
                            j !in 1 until ySize - 1 ||
                            sphereAngle(indexToCoords(position),
                                indexToCoords(prev),
                                indexToCoords(i, j)) < minAngle
                        ) || count[i][j] < 4 || ridges[i][j] > 0
                ) {
                    continue
                }
                ridges[i][j] = 1
                front.add(Point(field.xCoordinates[i], field.yCoordinates[j]))
                ridgeFilterWalk(IntPoint(i, j), position, front)
                return IntPoint(i, j)
            }
            return IntPoint(-1, -1)
        }

        for (i in 1..xSize - 2) {
            for (j in 1..ySize - 2) {
                if (mask[i, j] < 1.0) {
                    continue
                }
                val position = IntPoint(i, j)
                count[i][j] = DIRECTIONS.count { isSuperior(position, it) }
                if (count[i][j] == 5) {
                    /**
                     * A H G
                     * B X F
                     * C D E
                     * */
                    for (shift in 0 until 8) {
                        val h = DIRECTIONS[(7 + shift) % 8]
                        val a = DIRECTIONS[(0 + shift) % 8]
                        val b = DIRECTIONS[(1 + shift) % 8]
                        val c = DIRECTIONS[(2 + shift) % 8]
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
                        val a = DIRECTIONS[(0 + shift) % 8]
                        val b = DIRECTIONS[(1 + shift) % 8]
                        val d = DIRECTIONS[(3 + shift) % 8]
                        val e = DIRECTIONS[(4 + shift) % 8]
                        val f = DIRECTIONS[(5 + shift) % 8]
                        val g = DIRECTIONS[(6 + shift) % 8]
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
        val fs = FrontSimplifier()
        val eps = 0.0
        for (start in sups.asReversed()) {
            if (ridges[start.x][start.y] > 0) {
                continue
            }
            ridges[start.x][start.y] = 1
            val startCoord = Point(field.xCoordinates[start.x],
                field.yCoordinates[start.y])
            var front = mutableListOf(startCoord)
            val last = ridgeFilterWalk(start, IntPoint(-1, -1), front)
            front = fs.ramerDouglasPeucker(front, eps)
            front.reverse()
            ridgeFilterWalk(start, last, front)
            if (front.size >= 2) {
                detectedFronts.add(fs.ramerDouglasPeucker(front, eps))
            }
        }
    }

    fun ridgeDetectionGradient(temp: FieldInterface) {
        detectedFronts2.clear()
        val gradientVec = gradient(temp)
        val sups = mutableListOf<IntPoint>()
        val (xSize, ySize) = field.size
        val count = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })
        val ridges = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })
        val endPoints = Array(xSize,
            { _ -> Array<MutableList<IntPoint>?>(ySize, { _ -> null }) })

        val dirs = mutableListOf<IntPoint>()
        for (dx in -searchRadius..searchRadius) {
            for (dy in -searchRadius..searchRadius) {
                if (dx != 0 || dy != 0) {
                    dirs.add(IntPoint(dx, dy))
                }
            }
        }

        fun line(x0: Int, y0: Int, x1: Int, y1: Int): MutableList<IntPoint> {
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

        fun thickLine(x0: Int, y0: Int, x1: Int,
                      y1: Int): MutableList<IntPoint> {
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

        /**
         * Checks value superiority
         *
         * Checks if the value at position is greater than value at position + shift
         * @see <a href="http://dx.doi.org/10.1017/S1350482702003092">
         *     Use of a simple pattern recognition approach for the detection of ridge lines and stripes<\a>
         *
         * @param [field] Field where superiority is checked
         * @param [position] Position of point that is (or not) superior
         * @param [shift] Relative shift of position
         *
         * @author Polikutin Evgeny
         *
         * */
        fun isSuperior(position: IntPoint, shift: IntPoint): Boolean {
            return field[position.x, position.y] > field[position.x + shift.x, position.y + shift.y]
        }

        fun indexToCoords(input: IntPoint) =
            Pair(field.xCoordinates[input.x], field.yCoordinates[input.y])

        fun indexToCoords(i: Int, j: Int) = indexToCoords(IntPoint(i, j))

        /**
         * A supplementary function for [ridgeFilter].
         *
         * Performs a recursive walk over field
         * @see <a href="http://dx.doi.org/10.1017/S1350482702003092">
         *     Use of a simple pattern recognition approach for the detection of ridge lines and stripes<\a>
         *
         * @param [position] Current position
         * @param [prev] Previous position
         *
         * @author Polikutin Evgeny
         * */
        fun ridgeFilterWalk(
            position: IntPoint,
            prev: IntPoint,
            front: MutableList<IntPoint>
        ): IntPoint {
            val candidates = dirs.filter {
                position.x + it.x in 0 until field.size.first &&
                    position.y + it.y in 0 until field.size.second
            }.sortedWith(compareBy(
                { searchRadius - max(abs(it.x), abs(it.y)) },
                {
                    val grad = Point(
                        gradientVec.first[position.x + it.x, position.y + it.y],
                        gradientVec.second[position.x + it.x, position.y + it.y])
                    val norm1 = Point(-grad.y, grad.x)
                    val norm2 = Point(grad.y, -grad.x)
                    min(it.vecAngle(norm1), it.vecAngle(norm2))
                },
                { count[position.x + it.x][position.y + it.y] },
                { field[position.x + it.x, position.y + it.y] }
            )).reversed()
            outer@ for (it in candidates) {
                val i = position.x + it.x
                val j = position.y + it.y
                var correct = true
                //                for (off in 1..lookback) {
                //                    if (front.lastIndex < off || !correct) {
                //                        break
                //                    }
                //                    correct = correct && it.vecAngle(
                //                        front[front.lastIndex - off] - front[front.lastIndex - off + 1]) >= minAngle
                //                }

                val grad = Point(gradientVec.first[i, j],
                    gradientVec.second[i, j])
                val norm1 = Point(-grad.y, grad.x)
                val norm2 = Point(grad.y, -grad.x)
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
                    front.add(IntPoint(i, j))
                    ridgeFilterWalk(IntPoint(i, j), position, front)
                    return IntPoint(i, j)
                }
            }
            if (prev != IntPoint(-1, -1)) {
                endPoints[prev.x][prev.y] = front
            }
            return IntPoint(-1, -1)
        }

        for (i in 1..xSize - 2) {
            for (j in 1..ySize - 2) {
                if (mask[i, j] < 1.0) {
                    continue
                }
                val position = IntPoint(i, j)
                count[i][j] = DIRECTIONS.count { isSuperior(position, it) }
                if (count[i][j] == 5) {
                    /**
                     * A H G
                     * B X F
                     * C D E
                     * */
                    for (shift in 0 until 8) {
                        val h = DIRECTIONS[(7 + shift) % 8]
                        val a = DIRECTIONS[(0 + shift) % 8]
                        val b = DIRECTIONS[(1 + shift) % 8]
                        val c = DIRECTIONS[(2 + shift) % 8]
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
                        val a = DIRECTIONS[(0 + shift) % 8]
                        val b = DIRECTIONS[(1 + shift) % 8]
                        val d = DIRECTIONS[(3 + shift) % 8]
                        val e = DIRECTIONS[(4 + shift) % 8]
                        val f = DIRECTIONS[(5 + shift) % 8]
                        val g = DIRECTIONS[(6 + shift) % 8]
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
        for (start in sups.asReversed()) {
            if (ridges[start.x][start.y] > 0) {
                continue
            }
            ridges[start.x][start.y] = 1
            val front = mutableListOf(start)
            val last = ridgeFilterWalk(start, IntPoint(-1, -1), front)
            front.reverse()
            ridgeFilterWalk(start, last, front)
            if (front.size >= 2) {
                detectedFrontsGradient.add(Front(front.map {
                    Point(field.xCoordinates[it.x], field.yCoordinates[it.y])
                }.toMutableList(), front.first().run { classification[x, y] }))
            }
        }
    }
}