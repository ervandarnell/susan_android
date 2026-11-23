package com.susan.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.withScale

/**
 * Callback interface for hex board interactions.
 */
interface HexBoardListener {
    fun onHexTapped(hex: HexCoord)
    fun onStoneSlid(from: HexCoord, to: HexCoord)
}

/**
 * Custom View that renders the Susan hex board and handles touch input.
 *
 * Supports:
 * - Rendering the hex grid, stones, and highlights
 * - Pinch-to-zoom and two-finger pan
 * - Tap to select/place (delegated to [listener])
 * - Drag to slide stones (delegated to [listener])
 */
class HexBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Listeners ---
    var listener: HexBoardListener? = null

    // --- Model reference ---
    var gameState: GameState? = null
        set(value) {
            field = value
            invalidate()
        }

    // --- Mode ---
    /** The color to place, or null if not in placement mode. */
    var placementColor: StoneColor? = null
        set(value) {
            field = value
            invalidate()
        }

    // --- Highlight state ---
    /** The hex currently being dragged from (for slide preview). */
    private var dragFromHex: HexCoord? = null
    /** The hex the finger is currently over during a drag. */
    private var dragOverHex: HexCoord? = null
    /** Legal slide destinations for the currently selected stone. */
    private var dragDestinations: Set<HexCoord> = emptySet()

    // --- Transform ---
    private var scale = 1f
    private var panX = 0f
    private var panY = 0f
    private var boardCenterX = 0f
    private var boardCenterY = 0f

    // --- Gesture detectors ---
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    private var isScaling = false

    // --- Paints ---
    private val hexFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E8D5B7")
    }
    private val hexStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#8B7355")
        strokeWidth = 2f
    }
    private val hexHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFD700")
    }
    private val hexValidTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#90EE90")
    }
    private val stoneWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val stoneWhiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#999999")
        strokeWidth = 3f
    }
    private val stoneBlackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#222222")
    }
    private val stoneBlackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 3f
    }

    init {
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boardCenterX = w / 2f
        boardCenterY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val gs = gameState ?: return

        canvas.withScale(scale, scale, boardCenterX + panX, boardCenterY + panY) {
            // Draw each hex on the board
            for (hex in gs.boardHexes) {
                val center = HexUtils.hexToPixel(hex)
                val screenCenter = PointF(
                    center.x + boardCenterX,
                    center.y + boardCenterY
                )

                // Determine fill color
                val fillPaint = when {
                    hex == dragFromHex -> hexHighlightPaint
                    hex in dragDestinations -> hexValidTargetPaint
                    else -> hexFillPaint
                }

                // Draw hexagon
                val path = Path()
                val corners = HexUtils.hexCorners(screenCenter)
                if (corners.isNotEmpty()) {
                    path.moveTo(corners[0].x, corners[0].y)
                    for (i in 1 until corners.size) {
                        path.lineTo(corners[i].x, corners[i].y)
                    }
                    path.close()
                    drawPath(path, fillPaint)
                    drawPath(path, hexStrokePaint)
                }
            }

            // Draw stones
            for ((hex, color) in gs.stones) {
                val center = HexUtils.hexToPixel(hex)
                val screenCenter = PointF(
                    center.x + boardCenterX,
                    center.y + boardCenterY
                )
                val stoneRadius = HexUtils.HEX_SIZE * 0.7f  // fill 70% of hex width

                val fillPaint = if (color == StoneColor.WHITE) stoneWhitePaint else stoneBlackPaint
                val borderPaint = if (color == StoneColor.WHITE) stoneWhiteBorderPaint else stoneBlackBorderPaint

                drawCircle(screenCenter.x, screenCenter.y, stoneRadius, fillPaint)
                drawCircle(screenCenter.x, screenCenter.y, stoneRadius, borderPaint)
            }
        }
    }

    /**
     * Convert touch (screen) coordinates to hex coordinates, accounting for zoom/pan.
     *
     * The board is drawn inside `withScale(scale, scale, pivotX, pivotY)` where
     * pivot = (boardCenterX + panX, boardCenterY + panY).  A canvas point
     * (cx, cy) appears on screen at  (cx - pivotX) * scale + pivotX.
     * Hex (0,0) is drawn at canvas (boardCenterX, boardCenterY).
     *
     * Reverse:  canvasX = (touchX - pivotX) / scale + pivotX
     *           hexX   = canvasX - boardCenterX
     *                 = (touchX - boardCenterX - panX) / scale + panX
     */
    private fun touchToHex(touchX: Float, touchY: Float): HexCoord? {
        val gs = gameState ?: return null
        val hexX = (touchX - boardCenterX - panX) / scale + panX
        val hexY = (touchY - boardCenterY - panY) / scale + panY
        val hex = HexUtils.pixelToHex(hexX, hexY)
        // Check that the tap is actually within the hexagon, not in the gaps between hexes.
        // For a flat-top hex of size HEX_SIZE, the farthest point from center is at
        // distance HEX_SIZE (a vertex corner). Use a proper hex-containment test:
        // a point is inside the hex iff all three cube-coordinate inequalities hold.
        if (hex !in gs.boardHexes) return null
        val pixel = HexUtils.hexToPixel(hex)
        val dx = hexX - pixel.x
        val dy = hexY - pixel.y
        // Convert to cube coordinates relative to hex center
        val rq = dx / HexUtils.HEX_SIZE
        val rr = (-dx * 0.5f + dy * 0.8660254f) / HexUtils.HEX_SIZE
        val rs = -rq - rr
        // Point is inside the hex if all three absolute values <= 0.5
        return if (maxOf(Math.abs(rq), Math.abs(rr), Math.abs(rs)) <= 0.5001f) hex else null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // End drag mode
                val from = dragFromHex
                val over = dragOverHex
                if (from != null && over != null && over != from) {
                    listener?.onStoneSlid(from, over)
                }
                dragFromHex = null
                dragOverHex = null
                dragDestinations = emptySet()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && dragFromHex != null) {
                    val hex = touchToHex(event.x, event.y)
                    if (hex != dragOverHex) {
                        dragOverHex = hex
                        invalidate()
                    }
                }
            }
        }
        return true
    }

    // --- Scale Gesture Listener ---
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            scale = scale.coerceIn(0.3f, 3.0f)
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    }

    // --- Gesture Detector Listener ---
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            val hex = touchToHex(e.x, e.y) ?: return false
            val gs = gameState ?: return false

            // If placement mode is active, return true so onSingleTapUp fires
            if (placementColor != null) return true

            // Check if we're touching a stone we can drag
            val stone = gs.stones[hex]
            if (stone != null && stone == gs.currentTurn) {
                dragFromHex = hex
                dragOverHex = hex
                dragDestinations = gs.slideDestinations(hex)
                invalidate()
                return true
            }

            // Always return true for any board hex so GestureDetector delivers onSingleTapUp
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (isScaling) return false
            val hex = touchToHex(e.x, e.y) ?: return false
            listener?.onHexTapped(hex)
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!isScaling && dragFromHex == null) {
                // Two-finger pan
                panX -= distanceX
                panY -= distanceY
                invalidate()
                return true
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            // If not already dragging, try to start a slide
            val hex = touchToHex(e.x, e.y) ?: return
            val gs = gameState ?: return
            val stone = gs.stones[hex]
            if (stone != null && stone == gs.currentTurn) {
                dragFromHex = hex
                dragOverHex = hex
                invalidate()
            }
        }
    }
}
