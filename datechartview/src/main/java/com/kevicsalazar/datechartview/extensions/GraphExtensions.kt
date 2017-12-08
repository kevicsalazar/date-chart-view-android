package com.kevicsalazar.datechartview.extensions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * @author Kevin Salazar
 * @link https://github.com/kevicsalazar
 */

internal fun Paint.height() = (ascent() + descent()) / 2

internal fun Canvas.drawTriangle(paint: Paint, x: Int, y: Int, width: Int, height: Int) {
    val halfWidth = width / 2
    val halfHeight = height / 2
    val path = Path()
    path.moveTo(x.toFloat(), y.toFloat() - halfHeight) // Top
    path.lineTo(x.toFloat() - halfWidth, y.toFloat() + halfWidth) // Bottom left
    path.lineTo(x.toFloat() + halfWidth, y.toFloat() + halfWidth) // Bottom right
    path.lineTo(x.toFloat(), y.toFloat() - halfHeight) // Back to Top
    path.close()
    drawPath(path, paint)
}