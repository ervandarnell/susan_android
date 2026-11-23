package com.susan.android

/**
 * Represents a move the bot can choose.
 */
sealed class BotMove {
    data class Place(val hex: HexCoord) : BotMove()
    data class Slide(val from: HexCoord, val to: HexCoord) : BotMove()
}

/**
 * Minimax-based bot for the game Susan.
 *
 * Evaluates each legal move by simulating the opponent's best reply,
 * then picks the move that maximises the bot's advantage (minimax with
 * alpha-beta pruning, 2-ply deep).
 */
object SusanBot {

    private const val PLY = 2

    /**
     * Choose the best move for [color] in the given [state].
     */
    fun chooseMove(state: GameState, color: StoneColor): BotMove? {
        val moves = allLegalMoves(state, color)
        if (moves.isEmpty()) return null

        var bestMove: BotMove? = null
        var bestScore = Float.NEGATIVE_INFINITY

        for (move in moves) {
            val sim = state.copy()
            applyMove(sim, move, color)
            val score = minimax(sim, PLY - 1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, false, color)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    /**
     * Minimax with alpha-beta pruning.
     *
     * @param state      the current board state
     * @param depth      remaining depth to search
     * @param alpha      best score the maximiser can guarantee
     * @param beta       best score the minimiser can guarantee
     * @param maximizing true if it's the bot's turn to move
     * @param botColor   the bot's color (constant throughout)
     * @return the evaluated score
     */
    private fun minimax(
        state: GameState,
        depth: Int,
        alpha: Float,
        beta: Float,
        maximizing: Boolean,
        botColor: StoneColor
    ): Float {
        // Terminal states
        if (state.gameOver) {
            return when (state.winner) {
                botColor -> 1000f
                botColor.opponent() -> -1000f
                else -> 0f
            }
        }
        if (depth == 0) {
            return evaluate(state, botColor)
        }

        val color = if (maximizing) botColor else botColor.opponent()
        val moves = allLegalMoves(state, color)
        if (moves.isEmpty()) {
            // No legal moves — current player loses under Susan's rules
            return if (maximizing) -1000f else 1000f
        }

        var a = alpha
        var b = beta

        if (maximizing) {
            var value = Float.NEGATIVE_INFINITY
            for (move in moves) {
                val sim = state.copy()
                applyMove(sim, move, color)
                value = maxOf(value, minimax(sim, depth - 1, a, b, false, botColor))
                a = maxOf(a, value)
                if (value >= b) break  // beta cut-off
            }
            return value
        } else {
            var value = Float.POSITIVE_INFINITY
            for (move in moves) {
                val sim = state.copy()
                applyMove(sim, move, color)
                value = minOf(value, minimax(sim, depth - 1, a, b, true, botColor))
                b = minOf(b, value)
                if (value <= a) break  // alpha cut-off
            }
            return value
        }
    }

    /**
     * Return all legal moves for [color] in [state].
     */
    private fun allLegalMoves(state: GameState, color: StoneColor): List<BotMove> {
        if (color != state.currentTurn || state.gameOver) return emptyList()
        val moves = mutableListOf<BotMove>()

        // Place moves: any empty hex
        for (hex in state.emptyHexes()) {
            moves.add(BotMove.Place(hex))
        }

        // Slide moves: for each stone of this color, each empty neighbour
        for ((hex, c) in state.stones) {
            if (c == color) {
                for (to in state.emptyNeighbors(hex)) {
                    moves.add(BotMove.Slide(hex, to))
                }
            }
        }
        return moves
    }

    /**
     * Apply [move] to [state] for the given [color] (no validation needed).
     */
    private fun applyMove(state: GameState, move: BotMove, color: StoneColor) {
        when (move) {
            is BotMove.Place -> state.placeStone(move.hex, color)
            is BotMove.Slide -> state.slideStone(move.from, move.to)
        }
    }

    /**
     * Heuristic evaluation of [state] from [botColor]'s perspective.
     * Positive = good for bot, negative = good for opponent.
     */
    private fun evaluate(state: GameState, botColor: StoneColor): Float {
        val oppColor = botColor.opponent()
        var score = 0f

        // For each of the bot's stones: reward having many empty neighbours
        for ((hex, color) in state.stones) {
            val emptyNeighbours = state.emptyNeighbors(hex).size
            if (color == botColor) {
                score += emptyNeighbours * 3f
            } else {
                score -= emptyNeighbours * 3f
            }
        }

        // Adjacency to opponent: reward proximity to opponent's stones
        for ((hex, color) in state.stones) {
            if (color != botColor) continue
            for (n in HexUtils.neighbors(hex)) {
                val s = state.stones[n]
                if (s == oppColor) {
                    score += 2f  // bot is next to opponent
                }
            }
        }

        // Central control: slight bonus for stones near the centre (0,0)
        for ((hex, color) in state.stones) {
            val dist = HexUtils.distance(hex, HexCoord(0, 0))
            val bonus = (6 - dist) * 0.5f
            if (color == botColor) {
                score += bonus
            } else {
                score -= bonus
            }
        }

        return score
    }
}