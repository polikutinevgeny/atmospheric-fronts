package com.gmail.polikutinevgeny.ridgedetection

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.utility.Front
import com.gmail.polikutinevgeny.utility.component1
import com.gmail.polikutinevgeny.utility.component2
import com.gmail.polikutinevgeny.utility.sphereAngle
import java.awt.Point as IntPoint
import java.awt.geom.Point2D.Double as Point

class JannRidgeDetector(field: FieldInterface, mask: FieldInterface,
                        classification: FieldInterface,
                        var upperThreshold: Double, var lowerThreshold: Double,
                        var minAngle: Double) :
    RidgeDetector(field, mask, classification) {

    init {
        ridgeDetection()
    }

    override fun ridgeDetection(): MutableList<Front> {
        detectedFronts.clear()
        val sups = mutableListOf<IntPoint>()
        val (xSize, ySize) = field.size
        val ridges = Array(xSize, { _ -> IntArray(ySize, { _ -> 0 }) })

        val walkDirs = directions(1)
        val dirs = directions(1)

        val count = count(dirs, sups, lowerThreshold, upperThreshold)

        fun ridgeFilterWalk(
            position: IntPoint,
            prev: IntPoint,
            front: MutableList<IntPoint>
        ): IntPoint {
            val candidates = walkDirs.sortedWith(
                compareBy(
                    { count[position.x + it.y][position.x + it.y] },
                    { field[position.x + it.y, position.x + it.y] }
                )
            ).reversed()
            for ((dx, dy) in candidates) {
                val i = position.x + dx
                val j = position.y + dy
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
                front.add(IntPoint(i, j))
                ridgeFilterWalk(IntPoint(i, j), position, front)
                return IntPoint(i, j)
            }
            return IntPoint(-1, -1)
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
                detectedFronts.add(Front(front.map {
                    Point(field.xCoordinates[it.x], field.yCoordinates[it.y])
                }.toMutableList(), front.first().run { classification[x, y] }))
            }
        }
        return detectedFronts
    }
}