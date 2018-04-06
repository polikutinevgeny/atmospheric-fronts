package com.gmail.polikutinevgeny.ridgedetection

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.utility.Front
import com.gmail.polikutinevgeny.utility.sphereAngle
import java.awt.Point as IntPoint
import java.awt.geom.Point2D.Double as Point

private operator fun IntPoint.component1(): Int {
    return this.x
}


private operator fun IntPoint.component2(): Int {
    return this.y
}

class RidgeDetector(var upperThreshold: Double, var lowerThreshold: Double,
                    val field: FieldInterface, var mask: FieldInterface) {
    companion object {
        /**
         * Moving directions (counterclockwise!)
         *
         *
         * */
        private val DIRECTIONS = arrayOf(
            IntPoint(-1, -1), IntPoint(0, -1), IntPoint(1, -1), IntPoint(1, 0), IntPoint(1, 1),
            IntPoint(0, 1), IntPoint(-1, 1), IntPoint(-1, 0)
        )
    }

    val detectedFronts: MutableList<Front> = mutableListOf()

    init {
        ridgeDetection()
    }

    private fun ridgeDetection() {
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
            Pair(field.xCoordinates[input.x], field.yCoordinates[input.y])

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
            front: Front
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
                        sphereAngle(indexToCoords(position), indexToCoords(prev), indexToCoords(i, j)) < 60
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
        for (start in sups) {
            if (ridges[start.x][start.y] > 0) {
                continue
            }
            ridges[start.x][start.y] = 1
            val startCoord = Point(field.xCoordinates[start.x], field.yCoordinates[start.y])
            val front = mutableListOf(startCoord)
            val last = ridgeFilterWalk(start, IntPoint(-1, -1), front)
            front.reverse()
            ridgeFilterWalk(start, last, front)
            if (front.size > 2) {
                detectedFronts.add(front)
            }
        }
    }
}