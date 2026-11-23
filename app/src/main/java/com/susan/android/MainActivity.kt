package com.susan.android

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), HexBoardListener {

    companion object {
        private var versionCounter = 1
    }

    private lateinit var gameState: GameState
    private lateinit var hexBoard: HexBoardView
    private lateinit var turnIndicator: TextView
    private lateinit var versionLabel: TextView
    private lateinit var btnWhite: Button
    private lateinit var btnBlack: Button
    private lateinit var btnReset: Button
    private lateinit var btnNewGame: Button
    private lateinit var btnBot: Button
    private lateinit var btnRules: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameState = GameState()

        hexBoard = findViewById(R.id.hexBoard)
        hexBoard.gameState = gameState
        hexBoard.listener = this

        turnIndicator = findViewById(R.id.turnIndicator)
        versionLabel = findViewById(R.id.versionLabel)
        versionLabel.text = "Version 1.$versionCounter"
        versionCounter++

        btnWhite = findViewById(R.id.btnWhite)
        btnBlack = findViewById(R.id.btnBlack)
        btnReset = findViewById(R.id.btnReset)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnBot = findViewById(R.id.btnBot)
        btnRules = findViewById(R.id.btnRules)

        btnRules.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Susan — Rules")
                .setMessage(
                    """
                    Susan is a two-player abstract game played on a hex grid.
                    
                    Players: White goes first, then Black.
                    
                    On each turn you must make exactly one move:
                    
                    • PLACE: Tap your color button, then tap any empty hex to place a stone.
                    
                    • SLIDE: Touch-drag one of your own stones to an empty adjacent hex.
                    
                    A player loses if any of their stones is not adjacent to an empty hex.
                    If both players are surrounded on the same play, the player whose turn it is loses.
                    
                    No captures. Stones are never removed.
                    """.trimIndent()
                )
                .setPositiveButton("OK", null)
                .show()
        }

        btnWhite.setOnClickListener {
            if (gameState.gameOver) return@setOnClickListener
            if (gameState.currentTurn == StoneColor.WHITE) {
                hexBoard.placementColor = StoneColor.WHITE
                updateTurnDisplay()
            }
        }

        btnBlack.setOnClickListener {
            if (gameState.gameOver) return@setOnClickListener
            if (gameState.currentTurn == StoneColor.BLACK) {
                hexBoard.placementColor = StoneColor.BLACK
                updateTurnDisplay()
            }
        }

        btnReset.setOnClickListener {
            gameState.reset()
            hexBoard.placementColor = null
            hexBoard.postInvalidate()
            updateTurnDisplay()
        }

        btnNewGame.setOnClickListener {
            gameState.reset()
            hexBoard.placementColor = null
            btnNewGame.visibility = Button.GONE
            hexBoard.postInvalidate()
            updateTurnDisplay()
        }

        btnBot.setOnClickListener {
            if (gameState.gameOver) return@setOnClickListener
            val color = gameState.currentTurn
            val move = SusanBot.chooseMove(gameState, color) ?: return@setOnClickListener
            when (move) {
                is BotMove.Place -> gameState.placeStone(move.hex, color)
                is BotMove.Slide -> gameState.slideStone(move.from, move.to)
            }
            hexBoard.placementColor = null
            hexBoard.postInvalidate()
            updateTurnDisplay()
            checkGameOverDialog()
        }

        updateTurnDisplay()
    }

    override fun onHexTapped(hex: HexCoord) {
        val color = hexBoard.placementColor ?: return
        val result = gameState.placeStone(hex, color)
        if (result is MoveResult.Success) {
            hexBoard.placementColor = null
            hexBoard.postInvalidate()
            updateTurnDisplay()
            checkGameOverDialog()
        }
    }

    override fun onStoneSlid(from: HexCoord, to: HexCoord) {
        val result = gameState.slideStone(from, to)
        if (result is MoveResult.Success) {
            hexBoard.postInvalidate()
            updateTurnDisplay()
            checkGameOverDialog()
        }
    }

    private fun updateTurnDisplay() {
        if (gameState.gameOver) {
            turnIndicator.text = "Game over"
        } else {
            turnIndicator.text = when (gameState.currentTurn) {
                StoneColor.WHITE -> getString(R.string.turn_white)
                StoneColor.BLACK -> getString(R.string.turn_black)
            }
            // Show placement mode indicator
            when (hexBoard.placementColor) {
                StoneColor.WHITE -> turnIndicator.text = "White: tap a hex to place"
                StoneColor.BLACK -> turnIndicator.text = "Black: tap a hex to place"
                null -> {} // already set above
            }
        }
    }

    private fun checkGameOverDialog() {
        if (!gameState.gameOver) return

        val winner = gameState.winner ?: return
        val message = when (winner) {
            StoneColor.WHITE -> R.string.black_no_moves
            StoneColor.BLACK -> R.string.white_no_moves
        }

        turnIndicator.text = "Game over — ${winner.name} wins!"

        AlertDialog.Builder(this)
            .setTitle(getString(if (winner == StoneColor.WHITE) R.string.white_wins else R.string.black_wins))
            .setMessage(getString(message))
            .setPositiveButton("OK") { _, _ -> btnNewGame.visibility = Button.VISIBLE }
            .setCancelable(false)
            .show()
    }
}
