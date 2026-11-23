package com.susan.android

import android.graphics.PointF

/**
 * Hex math utilities for flat-top hexagons.
 * Based on the standard hex grid guide: https://www.redblobgames.com/grids/hexagons/
 */
object HexUtils {

    /** Size of a hexagon: distance from center to a vertex (flat-top). */
    const val HEX_SIZE = 48f

    /** Width of a flat-top hexagon. */
    val HEX_WIDTH: Float get() = 2 * HEX_SIZE

    /** Height of a flat-top hexagon. */
    val HEX_HEIGHT: Float get() = Math.sqrt(3.0).toFloat() * HEX_SIZE

    /** Horizontal distance between centers of adjacent hexagons. */
    val HEX_HORIZONTAL_SPACING: Float get() = 1.5f * HEX_SIZE

    /** Vertical distance between centers of adjacent hexagons. */
    val HEX_VERTICAL_SPACING: Float get() = Math.sqrt(3.0).toFloat() * HEX_SIZE

    /**
     * Convert axial hex coordinates to pixel center position (flat-top).
     */
    fun hexToPixel(hex: HexCoord): PointF {
        val x = HEX_SIZE * (1.5f * hex.q)
        val y = HEX_SIZE * (Math.sqrt(3.0).toFloat() / 2f * hex.q + Math.sqrt(3.0).toFloat() * hex.r)
        return PointF(x, y)
    }

    /**
     * Convert pixel coordinates to axial hex coordinates (flat-top).
     * Returns the hex that the pixel falls into.
     */
    fun pixelToHex(x: Float, y: Float): HexCoord {
        val q = (2f / 3f * x) / HEX_SIZE
        val r = (-1f / 3f * x + Math.sqrt(3.0).toFloat() / 3f * y) / HEX_SIZE
        return axialRound(q, r)
    }

    /**
     * Round fractional axial coordinates to the nearest hex.
     */
    private fun axialRound(q: Float, r: Float): HexCoord {
        val s = -q - r
        var rq = q.roundToInt()
        var rr = r.roundToInt()
        var rs = s.roundToInt()

        val qDiff = Math.abs(rq - q)
        val rDiff = Math.abs(rr - r)
        val sDiff = Math.abs(rs - s)

        if (qDiff > rDiff && qDiff > sDiff) {
            rq = -rr - rs
        } else if (rDiff > sDiff) {
            rr = -rq - rs
        }
        // else sDiff is largest: adjust s implicitly

        return HexCoord(rq, rr)
    }

    /**
     * Get all neighbors of a hex.
     */
    fun neighbors(hex: HexCoord): List<HexCoord> {
        return HexCoord.DIRECTIONS.map { dir -> HexCoord(hex.q + dir.q, hex.r + dir.r) }
    }

    /**
     * Get all hex coordinates within a hexagonal shape of given radius.
     * Radius is the number of hexes from center to edge.
     */
    fun hexesInRadius(radius: Int): Set<HexCoord> {
        val result = mutableSetOf<HexCoord>()
        for (q in -radius..radius) {
            for (r in maxOf(-radius, -q - radius)..minOf(radius, -q + radius)) {
                result.add(HexCoord(q, r))
            }
        }
        return result
    }

    /**
     * Get the vertices of a flat-top hexagon centered at the given pixel point.
     * Returns 6 corner points in clockwise order starting from the rightmost point.
     *
     * @param center  the center of the hexagon
     * @param size    distance from center to vertex
     * @param out     optional reusable list to fill and return (avoids allocation)
     */
    fun hexCorners(
        center: PointF,
        size: Float = HEX_SIZE,
        out: MutableList<PointF> = mutableListOf()
    ): List<PointF> {
        out.clear()
        for (i in 0 until 6) {
            val angleDeg = 60f * i  // flat-top starts at 0 degrees (right)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            if (out.size <= i) {
                out.add(PointF())
            }
            out[i].x = center.x + size * Math.cos(angleRad).toFloat()
            out[i].y = center.y + size * Math.sin(angleRad).toFloat()
        }
        return out
    }

    /**
     * Check if two hexes are adjacent (share an edge).
     */
    fun areAdjacent(a: HexCoord, b: HexCoord): Boolean {
        val dq = Math.abs(a.q - b.q)
        val dr = Math.abs(a.r - b.r)
        val ds = Math.abs(a.s - b.s)
        return maxOf(dq, dr, ds) == 1
    }

    /**
     * Compute hex distance between two hexes.
     */
    fun distance(a: HexCoord, b: HexCoord): Int {
        return maxOf(
            Math.abs(a.q - b.q),
            Math.abs(a.r - b.r),
            Math.abs(a.s - b.s)
        )
    }

    /**
     * Check if a hex is within the board (radius 5 hexagon shape).
     */
    fun isOnBoard(hex: HexCoord, radius: Int = 5): Boolean {
        return maxOf(Math.abs(hex.q), Math.abs(hex.r), Math.abs(hex.s)) <= radius
    }

    private fun Float.roundToInt(): Int {
        return Math.round(this)
    }
}
