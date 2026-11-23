package com.susan.android

/**
 * Represents a stone color.
 */
enum class StoneColor {
    WHITE, BLACK;

    fun opponent(): StoneColor = when (this) {
        WHITE -> BLACK
        BLACK -> WHITE
    }
}

/**
 * Represents the result of a move attempt.
 */
sealed class MoveResult {
    data object Success : MoveResult()
    data class Failure(val reason: String) : MoveResult()
}

/**
 * Represents the game-over condition.
 */
sealed class GameOverResult {
    data object NotOver : GameOverResult()
    data class Winner(val color: StoneColor) : GameOverResult()
}

/**
 * Manages the state of a Susan game.
 *
 * @param boardRadius the radius of the hexagonal board (default 5).
 */
class GameState(val boardRadius: Int = 5) {

    /** The board hex coordinates within the hexagon shape of [boardRadius]. */
    val boardHexes: Set<HexCoord> = HexUtils.hexesInRadius(boardRadius)

    /** Map from hex coordinate to the stone color occupying it. */
    private val _stones: MutableMap<HexCoord, StoneColor> = mutableMapOf()
    val stones: Map<HexCoord, StoneColor> get() = _stones.toMap()

    /** Whose turn it is. */
    var currentTurn: StoneColor = StoneColor.WHITE
        private set

    /** Whether the game is over. */
    var gameOver: Boolean = false
        private set

    /** The winner, if the game is over. */
    var winner: StoneColor? = null
        private set

    // --- Queries ---

    /**
     * Returns all hexes that are empty (no stone placed).
     */
    fun emptyHexes(): Set<HexCoord> {
        return boardHexes.filter { it !in _stones }.toSet()
    }

    /**
     * Returns all hexes occupied by the given color.
     */
    fun stonesOfColor(color: StoneColor): Set<HexCoord> {
        return _stones.filter { it.value == color }.keys
    }

    /**
     * Returns the empty hexes adjacent to the given hex.
     */
    fun emptyNeighbors(hex: HexCoord): Set<HexCoord> {
        return HexUtils.neighbors(hex).filter { it in boardHexes && it !in _stones }.toSet()
    }

    /**
     * Returns all hexes where the given color can place a new stone.
     */
    fun placeableHexes(color: StoneColor): Set<HexCoord> {
        if (color != currentTurn || gameOver) return emptySet()
        return emptyHexes()
    }

    /**
     * Returns all possible slide destinations for a stone at the given hex.
     */
    fun slideDestinations(hex: HexCoord): Set<HexCoord> {
        val stone = _stones[hex] ?: return emptySet()
        if (stone != currentTurn || gameOver) return emptySet()
        return emptyNeighbors(hex)
    }

    /**
     * Return a copy of the current state.  Shared boardHexes is fine (read-only),
     * but _stones and turn/winner/gameOver must be independent copies.
     */
    fun copy(): GameState {
        val gs = GameState(boardRadius)
        gs._stones.putAll(_stones)
        gs.currentTurn = currentTurn
        gs.gameOver = gameOver
        gs.winner = winner
        return gs
    }

    // --- Mutations ---

    /**
     * Attempt to place a stone of [color] at [hex].
     * Returns Success if valid, Failure with reason otherwise.
     */
    fun placeStone(hex: HexCoord, color: StoneColor): MoveResult {
        if (gameOver) return MoveResult.Failure("Game is over")
        if (color != currentTurn) return MoveResult.Failure("Not ${color.name}'s turn")
        if (hex !in boardHexes) return MoveResult.Failure("Hex is off the board")
        if (hex in _stones) return MoveResult.Failure("Hex is already occupied")

        _stones[hex] = color
        currentTurn = color.opponent()
        checkGameOver()
        return MoveResult.Success
    }

    /**
     * Attempt to slide a stone from [from] to [to].
     * Returns Success if valid, Failure with reason otherwise.
     */
    fun slideStone(from: HexCoord, to: HexCoord): MoveResult {
        if (gameOver) return MoveResult.Failure("Game is over")
        if (from !in _stones) return MoveResult.Failure("No stone at source hex")
        if (_stones[from] != currentTurn) return MoveResult.Failure("Not your stone")
        if (to in _stones) return MoveResult.Failure("Target hex is occupied")
        if (to !in boardHexes) return MoveResult.Failure("Target hex is off the board")
        if (!HexUtils.areAdjacent(from, to)) return MoveResult.Failure("Hexes are not adjacent")

        _stones.remove(from)
        _stones[to] = currentTurn
        currentTurn = currentTurn.opponent()
        checkGameOver()
        return MoveResult.Success
    }

    /**
     * Check whether the game is over and set the winner accordingly.
     * A player loses if any of their stones is not adjacent to an empty hex.
     * This is checked on either player's turn, after every move.
     *
     * If both players have surrounded stones on the same play, the player
     * whose turn it is loses ("on White's turn, if a white and black stone
     * are both surrounded on the same play, white loses").
     */
    private fun checkGameOver() {
        if (gameOver) return

        val whiteTrapped = _stones.any { (hex, color) ->
            color == StoneColor.WHITE && emptyNeighbors(hex).isEmpty()
        }
        val blackTrapped = _stones.any { (hex, color) ->
            color == StoneColor.BLACK && emptyNeighbors(hex).isEmpty()
        }

        when {
            // Both trapped: the player whose turn it is loses
            whiteTrapped && blackTrapped -> {
                gameOver = true
                winner = currentTurn.opponent()
            }
            whiteTrapped -> {
                gameOver = true
                winner = StoneColor.BLACK
            }
            blackTrapped -> {
                gameOver = true
                winner = StoneColor.WHITE
            }
        }
    }

    /**
     * Check if the given player has any legal move at all.
     */
    fun hasLegalMoves(color: StoneColor): Boolean {
        if (color != currentTurn) return false

        // Can place if any empty hex exists
        if (emptyHexes().isNotEmpty()) return true

        // Can slide if any stone of this color has an empty neighbor
        for ((hex, stoneColor) in _stones) {
            if (stoneColor == color && emptyNeighbors(hex).isNotEmpty()) return true
        }

        return false
    }

    /**
     * Reset the board (keep same board size), clearing all stones.
     */
    fun reset() {
        _stones.clear()
        currentTurn = StoneColor.WHITE
        gameOver = false
        winner = null
    }
}
