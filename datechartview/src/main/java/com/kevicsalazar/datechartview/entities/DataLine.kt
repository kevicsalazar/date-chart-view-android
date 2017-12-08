package com.kevicsalazar.datechartview.entities

import android.graphics.Color
import android.support.annotation.ColorInt

/**
 * @author Kevin Salazar
 * @link https://github.com/kevicsalazar
 */
data class DataLine(val year: Int, val month: Int, val points: List<DataPoint>, @ColorInt val color: Int = Color.CYAN)