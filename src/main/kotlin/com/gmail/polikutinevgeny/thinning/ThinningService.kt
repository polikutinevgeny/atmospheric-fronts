package com.gmail.polikutinevgeny.thinning

class ThinningService {

    // p9 p2 p3
    // p8 p1 p4
    // p7 p6 p5

    companion object {
        private val DIRECTIONS = arrayOf(
            Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1),
            Pair(1, 0),
            Pair(1, -1), Pair(0, -1), Pair(-1, -1)
        )
    }

    fun doBSTThinning(input: Array<IntArray>): Array<IntArray> {
        val temp = Array(input.size) { input[it].clone() }
        do {
            var changed = false
            for (i in 0..temp.lastIndex) {
                for (j in 0..temp[i].lastIndex) {
                    if (
                        temp[i][j] == 1 &&
                        (i + j) % 2 == 0 &&
                        c(temp, i, j) == 1 &&
                        b(temp, i, j) in 2..7 &&
                        position(temp, i, j, 2) * position(temp, i, j, 4) *
                        position(temp, i, j, 6) == 0 &&
                        position(temp, i, j, 4) * position(temp, i, j, 6) *
                        position(temp, i, j, 8) == 0
                    ) {
                        temp[i][j] = 0
                        changed = true
                    }
                }
            }
            for (i in 0..temp.lastIndex) {
                for (j in 0..temp[i].lastIndex) {
                    if (
                        temp[i][j] == 1 &&
                        (i + j) % 2 != 0 &&
                        c(temp, i, j) == 1 &&
                        b(temp, i, j) in 1..7 &&
                        position(temp, i, j, 2) * position(temp, i, j, 4) *
                        position(temp, i, j, 8) == 0 &&
                        position(temp, i, j, 2) * position(temp, i, j, 6) *
                        position(temp, i, j, 8) == 0
                    ) {
                        if (b(temp, i, j) == 1) {
                            for (k in arrayOf(3, 5, 7, 9)) {
                                if (position(temp, i, j, k) == 1) {
                                    val x = i + DIRECTIONS[k - 1].first
                                    val y = j + DIRECTIONS[k - 1].second
                                    if ((x + y) % 2 != 0) {
                                        if (b(input, x, y) > 2) {
                                            temp[i][j] = 0
                                            changed = true
                                        }
                                    } else {
                                        temp[i][j] = 0
                                        changed = true
                                    }
                                    break
                                }
                            }
                        } else {
                            temp[i][j] = 0
                            changed = true
                        }
                    }
                }
            }
        } while (changed)
        return temp
    }

    private fun b(input: Array<IntArray>, x: Int, y: Int): Int =
        DIRECTIONS.drop(1).count {
            position(input, x + it.first, y + it.second, 1) == 1
        }

    private fun position(input: Array<IntArray>, x: Int, y: Int,
                         index: Int): Int {
        if (!(x + DIRECTIONS[index - 1].first in 0..input.lastIndex && y + DIRECTIONS[index - 1].second in 0..input[0].lastIndex)) {
            return 0
        }
        return input[x + DIRECTIONS[index - 1].first][y + DIRECTIONS[index - 1].second]
    }

    private fun c(input: Array<IntArray>, x: Int, y: Int): Int {
        fun p(index: Int): Boolean = position(input, x, y, index) == 1
        var res = 0
        if (!p(2) && (p(3) || p(4))) ++res
        if (!p(4) && (p(5) || p(6))) ++res
        if (!p(6) && (p(7) || p(8))) ++res
        if (!p(8) && (p(9) || p(2))) ++res
        return res
    }
}