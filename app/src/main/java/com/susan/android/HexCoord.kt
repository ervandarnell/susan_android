package com.susan.android

/**
 * Axial coordinates (q, r) for a hex grid with flat-top orientation.
 * The third cube coordinate is s = -q - r.
 */
data class HexCoord(val q: Int, val r: Int) {

    val s: Int get() = -q - r

    companion object {
        /** The six axial direction vectors for flat-top hexagons. */
        val DIRECTIONS: List<HexCoord> = listOf(
            HexCoord(1, 0),  // East
            HexCoord(1, -1), // Northeast
            HexCoord(0, -1), // Northwest
            HexCoord(-1, 0), // West
            HexCoord(-1, 1), // Southwest
            HexCoord(0, 1)   // Southeast
        )
    }
}
