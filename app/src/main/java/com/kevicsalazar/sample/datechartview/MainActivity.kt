package com.kevicsalazar.sample.datechartview

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.kevicsalazar.datechartview.entities.DataLine
import com.kevicsalazar.datechartview.entities.DataPoint
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDaySelected.text = Date().string("EEEE, d ' de ' MMMM")

        dateChart.onDaySelected = { date ->
            tvDaySelected.text = date.string("EEEE, d ' de ' MMMM")
            tvValueSelected.text = "81 %"
        }

        dateChart.onMonthChange = { newYear, newMonth ->

            val dataPoints = listOf(
                    DataPoint(16, 80f),
                    DataPoint(17, 82f),
                    DataPoint(18, 84f),
                    DataPoint(19, 81f)
            )

            val dataLine = DataLine(newYear, newMonth, dataPoints, colorRes(R.color.amber_500))
            val dataLines = mutableListOf(dataLine)
            dateChart.addMonthDataLines(dataLines)

        }

    }

    // Extensions

    private fun Date.string(format: String, locale: String? = null): String? {
        val df = SimpleDateFormat(format, locale?.let { Locale(it) } ?: Locale.getDefault())
        return df.format(this)
    }

    private fun Context.colorRes(colorResId: Int) = ContextCompat.getColor(this, colorResId)

}
