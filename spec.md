# susan_android — requirements

Create an Android app for the abstract game Susan.

## Build & Environment
- The project uses Kotlin as the primary language.
- The build system is Gradle (Kotlin DSL).
- The project structure must be openable and compilable by Android Studio.
- Minimum SDK: 26 (Android 8.0). Target SDK: 34 (Android 14).
- The app is a single-activity application with no external dependencies beyond AndroidX and the standard Android SDK.

## Game Board
- The game is played on a hex grid (hexagon tiling) with 5 hexes per side — a hex board of radius 5 (61 hexagons total, in a hexagonal shape).
- Hexagons are rendered in a flat-top orientation.
- Each hexagon has a unique coordinate. Use axial coordinates (q, r) for the hex grid.
- The board is centered on the screen and scales to fit the display.
- The board can be zoomed and panned for small screens.
- Put the label "Version 1.0" in the upper right.  Increment the sub version with changes.
- Put the title "Susan" on top

## Game State & Turns
- The game is a two-player, turn-based abstract game.
- Player 1 plays white stones. Player 2 plays black stones.
- White goes first.
- On each turn, a player must make exactly one of the following moves:
  1. **Place** a new stone of their color on any empty hex.  
  2. **Slide** one of their own existing stones to an empty adjacent hex.
- A stone may not be placed on an occupied hex.
- A slide moves a stone to an immediately neighboring hex (sharing an edge) that is currently empty.
- When a stone is selected with a button-down click, highlight the legal neighboring cells for movement.  Do not highlight illegal positions even on a long mouse drag.
- Stones are never removed from the board (no captures).
- There is no limit on the number of stones a player may place (other than board capacity).

## UI — Stone Placement
- Two buttons are shown above the board: "White" and "Black".
- When the player whose turn it is taps their color button, the app enters "placement mode".
- While in placement mode, tapping any empty hex, anywhere in the hex, places a stone of that player's color there.
- A stone is a circle of the player's color that fills 70% of the width of the hex.
- After placing, the app exits placement mode and the turn passes to the other player.
- Tapping the wrong color button (not the current player's turn) has no effect (or shows a brief message).

## UI — Stone Sliding
- A player slides a stone by touch-dragging (gliding) one of their own stones to an empty adjacent hex.
- When the user starts a drag on one of their own stones, the hex highlights to indicate it is selected.
- As the user drags, if the finger is over an adjacent empty hex, that hex highlights as a valid target.
- Releasing the finger over a valid adjacent empty hex completes the slide; the stone moves there.
- Releasing anywhere else cancels the slide; the stone stays in place.
- Dragging an opponent's stone does nothing.

## UI — General
- The current player's turn is displayed on screen (e.g. "White's turn" or "Black's turn").
- The board updates immediately after each move.
- A "Reset" button clears all stones from the board and resets the turn to White.
- The UI must work in portrait orientation. Landscape is optional.
- Touch events on hexagons must correctly map screen coordinates to hex coordinates using pixel-to-hex conversion.

## Bot
- Add a bot button so that a bot takes the current turn.  It could help a player on their turn, or take every turn and be an opponent.
- Use a minimax strategy to try and make interesting plays.

## Game End
- If one of white's stones is not adjacent to an empty hex, white loses.  This is true on either white's turn or black's.
- Black is similar.
- On White's turn, if a white and black stone are both surrounded on the same play, white loses.
- Recognize this condition and show a dialog box saying so.
- Add a "new game" button.

