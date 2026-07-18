package com.tally.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Matches each game's dedicated icon shape — Catan (hexagon), Poker (spade), Smash (star), Uno (square). */
enum class GameShape { HEXAGON, SPADE, STAR, SQUARE }

/**
 * A single-color game icon, drawn on a [Canvas] rather than bundled as vector assets, so its
 * tint always follows that game's brand color exactly (see `GameCatan`, `GamePoker`, etc. in
 * the theme). Deliberately has no background container of its own — screens that need the
 * colored-tile treatment (the "Pick a Game" grid, the screen-map banner) wrap this in their
 * own `Surface`/`Box`, keeping this composable a single-responsibility, reusable primitive.
 */
@Composable
fun GameGlyph(
    shapeType: GameShape,
    tintColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        when (shapeType) {
            GameShape.HEXAGON -> drawHexagon(tintColor)
            GameShape.SPADE -> drawSpade(tintColor)
            GameShape.STAR -> drawStar(tintColor)
            GameShape.SQUARE -> drawSquareGlyph(tintColor)
        }
    }
}

/** Flat-top hexagon outline — echoes a Catan board tile. */
private fun DrawScope.drawHexagon(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.minDimension / 2f
    val path = Path().apply {
        for (i in 0 until 6) {
            val angle = Math.toRadians((60 * i).toDouble())
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color = color, style = Stroke(width = size.minDimension * 0.09f))
}

/** Poker spade suit silhouette: rounded twin lobes tapering to a point, plus a small stem. */
private fun DrawScope.drawSpade(color: Color) {
    val w = size.width
    val h = size.height
    val lobe = Path().apply {
        moveTo(w * 0.5f, h * 0.85f)
        cubicTo(w, h * 0.55f, w * 0.75f, 0f, w * 0.5f, 0f)
        cubicTo(w * 0.25f, 0f, 0f, h * 0.55f, w * 0.5f, h * 0.85f)
        close()
    }
    drawPath(lobe, color = color)
    val stem = Path().apply {
        moveTo(w * 0.5f, h * 0.62f)
        lineTo(w * 0.66f, h)
        lineTo(w * 0.34f, h)
        close()
    }
    drawPath(stem, color = color)
}

/** Standard 5-point star, alternating outer/inner radii. */
private fun DrawScope.drawStar(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outerRadius = size.minDimension / 2f
    val innerRadius = outerRadius * 0.4f
    val path = Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = Math.toRadians((36 * i - 90).toDouble())
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color = color)
}

/** Rounded-square outline with a centered dot, echoing the mockup's Uno target-style glyph. */
private fun DrawScope.drawSquareGlyph(color: Color) {
    val strokeWidth = size.minDimension * 0.16f
    val inset = strokeWidth / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(size.minDimension * 0.22f),
        style = Stroke(width = strokeWidth),
    )
    drawCircle(
        color = color,
        radius = size.minDimension * 0.13f,
        center = Offset(size.width / 2f, size.height / 2f),
    )
}
