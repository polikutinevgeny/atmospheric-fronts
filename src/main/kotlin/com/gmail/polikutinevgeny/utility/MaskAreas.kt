package com.gmail.polikutinevgeny.utility

import com.gmail.polikutinevgeny.fields.FieldInterface
import java.awt.Point
import java.awt.geom.Point2D
import java.util.*

private val DIRECTIONS = arrayOf(
    Point(-1, -1), Point(0, -1), Point(1, -1), Point(1, 0), Point(1, 1),
    Point(0, 1), Point(-1, 1), Point(-1, 0)
)

fun maskAreas(mask: FieldInterface,
              classification: FieldInterface): MutableList<Front> {
    val visited = mask.clone()
    val result: MutableList<Front> = mutableListOf()
    val (xSize, ySize) = mask.size
    for (i in 0 until xSize) {
        for (j in 0 until ySize) {
            if (mask[i, j] == 1.0 && visited[i, j] == 0.0) {
                val queue: Queue<Point> = ArrayDeque<Point>()
                val list = Front(mutableListOf(), classification[i, j])
                queue.add(Point(i, j))
                list.add(
                    Point2D.Double(mask.xCoordinates[i], mask.yCoordinates[j]))
                visited[i, j] = 1.0
                while (queue.isNotEmpty()) {
                    val (x, y) = queue.remove()!!
                    for ((dx, dy) in DIRECTIONS) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until xSize && ny in 0 until ySize &&
                            visited[nx, ny] == 0.0 && mask[nx, ny] == 1.0 && classification[nx, ny] == classification[x, y]) {
                            visited[nx, ny] = 1.0
                            queue.add(Point(nx, ny))
                            list.add(Point2D.Double(mask.xCoordinates[nx],
                                mask.yCoordinates[ny]))
                        }
                    }
                }
                if (list.size > 4) {
                    result.add(list)
                }
            }
        }
    }
    return result
}