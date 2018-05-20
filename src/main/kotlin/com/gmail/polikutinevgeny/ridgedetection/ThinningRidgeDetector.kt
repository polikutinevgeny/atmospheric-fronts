package com.gmail.polikutinevgeny.ridgedetection

import com.gmail.polikutinevgeny.fields.FieldInterface
import com.gmail.polikutinevgeny.thinning.ThinningService
import com.gmail.polikutinevgeny.utility.Front
import java.awt.geom.Point2D.Double as Point

class ThinningRidgeDetector(field: FieldInterface,
                            mask: FieldInterface,
                            classification: FieldInterface) :
    RidgeDetector(field, mask, classification) {

    override fun ridgeDetection(): MutableList<Front> {
        detectedFronts.clear()
        val ts = ThinningService()
        val thinned = ts.doBSTThinning(Array(field.xCoordinates.size, { i ->
            IntArray(field.yCoordinates.size,
                { j -> if (mask[i, j] > 0) 1 else 0 })
        }))
        frontWalk(thinned)
        return detectedFronts
    }

    private fun frontWalk(input: Array<IntArray>) {
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

        fun frontWalkRec(x: Int, y: Int, line: Front) {
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
                    val l = Front(mutableListOf(
                        Point(field.xCoordinates[x], field.yCoordinates[y])),
                        classification[x, y])
                    frontWalkRec(x + dx, y + dy, l)
                    detectedFronts.add(l)
                }
            }
        }

        for (i in 0 until temp.size) {
            for (j in 0 until temp[i].size) {
                if (temp[i][j] == 1 && adjCount(i, j) == 1) {
                    val line = Front(mutableListOf(), classification[i, j])
                    frontWalkRec(i, j, line)
                    detectedFronts.add(line)
                }
            }
        }
    }
}