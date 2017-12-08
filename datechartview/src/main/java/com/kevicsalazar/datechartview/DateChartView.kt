package com.kevicsalazar.datechartview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.kevicsalazar.datechartview.entities.*
import com.kevicsalazar.datechartview.extensions.*
import java.util.*


/**
 * @author Kevin Salazar
 * @link https://github.com/kevicsalazar
 */
class DateChartView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private enum class Direction {
        NONE, LEFT, RIGHT
    }

    private var mWidthPerDay: Float? = null
    private var valueFactor: Float? = null
    private var daysOffset: Int = 0
    private val circlePath = Path()
    private var mXScrollingSpeed = 1f
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private var mCurrentFlingDirection = Direction.NONE

    private var mMinimumFlingVelocity = 0

    private var mGestureDetector: GestureDetectorCompat? = null
    private var mScroller: OverScroller? = null

    private var dayTextPaint: Paint? = null
    private var dayDisabledTextPaint: Paint? = null
    private var daySelectedTextPaint: Paint? = null
    private var labelTextPaint: Paint? = null
    private var selectedDayBackgroundPaint: Paint? = null
    private var bodyBackgroundPaint: Paint? = null
    private var footerBackgroundPaint: Paint? = null
    private var pointBackgroundPaint: Paint? = null
    private var dataLineStrokePaint: Paint? = null
    private var labelLineStrokePaint: Paint? = null
    private var middleLineStrokePaint: Paint? = null
    private var extraLineStrokePaint: Paint? = null

    private var labelLinePath: Path? = null

    private var minValue: Float = 0f
    private var maxValue: Float = 100f

    private var numOfVisibleDays: Int = 7
    private var numOfHorizontalLabels: Int = 3

    private var labelLineColor: Int = Color.LTGRAY
    private var labelTextColor: Int = Color.LTGRAY
    private var bodyBackgroundColor: Int = Color.LTGRAY
    private var footerBackgroundColor: Int = Color.WHITE
    private var selectedDayBackgroundColor: Int = Color.DKGRAY
    private var dayTextColor: Int = Color.DKGRAY
    private var middleLineColor: Int = Color.CYAN

    private var chartPaddingTop: Int = 50
    private var chartPaddingBottom: Int = 0
    private var labelTextWidth: Int = 50
    private var dayTextSize: Int = 16
    private var labelTextSize: Int = 14
    private var footerHeight: Int = 136
    private var dataPointRadius: Int = 10
    private var dataLineThickness: Int = 8
    private var labelLineThickness: Int = 4
    private var middleLineThickness: Int = 4

    private var middleLineVisible: Boolean = false

    private var mScrollDuration = 250

    var dataLines = mutableListOf<DataLine>()
    var extraLines = listOf<ExtraLine>()
    var onDaySelected: ((date: Date) -> Unit)? = null
    var onMonthChange: ((newYear: Int, newMonth: Int) -> Unit)? = null

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            goToNearestDay()
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {

            when (mCurrentScrollDirection) {
                Direction.NONE  -> {
                    // Allow scrolling only in one direction.
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        mCurrentScrollDirection = if (distanceX > 0) {
                            Direction.LEFT
                        } else {
                            Direction.RIGHT
                        }
                    }
                }
                Direction.LEFT  -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        mCurrentScrollDirection = Direction.RIGHT
                    }
                }
                Direction.RIGHT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        mCurrentScrollDirection = Direction.LEFT
                    }
                }
            }

            mCurrentOrigin.x -= distanceX * mXScrollingSpeed

            if (mCurrentOrigin.x < 0) {
                mCurrentOrigin.x = 0f
                return true
            }

            ViewCompat.postInvalidateOnAnimation(this@DateChartView)

            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

            mScroller!!.forceFinished(true)

            mCurrentFlingDirection = mCurrentScrollDirection

            mScroller!!.fling(mCurrentOrigin.x.toInt(),
                    mCurrentOrigin.y.toInt(),
                    (velocityX * mXScrollingSpeed).toInt(),
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    0,
                    0)

            ViewCompat.postInvalidateOnAnimation(this@DateChartView)

            return true
        }

        override fun onSingleTapUp(e1: MotionEvent): Boolean {
            goToSelectedDay(e1.x, e1.y)
            return true
        }

    }

    init {

        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DateChartView, 0, 0)

        try {
            minValue = a.getFloat(R.styleable.DateChartView_minValue, minValue)
            maxValue = a.getFloat(R.styleable.DateChartView_maxValue, maxValue)
            numOfVisibleDays = a.getInteger(R.styleable.DateChartView_numOfVisibleDays, numOfVisibleDays)
            numOfHorizontalLabels = a.getInteger(R.styleable.DateChartView_numOfHorizontalLabels, numOfHorizontalLabels)
            labelLineColor = a.getColor(R.styleable.DateChartView_labelLineColor, labelLineColor)
            labelTextColor = a.getColor(R.styleable.DateChartView_labelTextColor, labelTextColor)
            bodyBackgroundColor = a.getColor(R.styleable.DateChartView_bodyBackgroundColor, bodyBackgroundColor)
            footerBackgroundColor = a.getColor(R.styleable.DateChartView_footerBackgroundColor, footerBackgroundColor)
            selectedDayBackgroundColor = a.getColor(R.styleable.DateChartView_selectedDayBackgroundColor, selectedDayBackgroundColor)
            dayTextColor = a.getColor(R.styleable.DateChartView_dayTextColor, dayTextColor)
            middleLineColor = a.getColor(R.styleable.DateChartView_middleLineColor, middleLineColor)
            chartPaddingTop = a.getDimensionPixelSize(R.styleable.DateChartView_chartPaddingTop, chartPaddingTop)
            chartPaddingBottom = a.getDimensionPixelSize(R.styleable.DateChartView_chartPaddingBottom, if (minValue == 0f) chartPaddingBottom else 25)
            labelTextWidth = a.getDimensionPixelSize(R.styleable.DateChartView_labelTextWidth, labelTextWidth)
            dayTextSize = a.getDimensionPixelSize(R.styleable.DateChartView_dayTextSize, dayTextSize.sp())
            labelTextSize = a.getDimensionPixelSize(R.styleable.DateChartView_labelTextSize, labelTextSize.sp())
            footerHeight = a.getDimensionPixelSize(R.styleable.DateChartView_footerHeight, footerHeight)
            dataPointRadius = a.getDimensionPixelSize(R.styleable.DateChartView_dataPointRadius, dataPointRadius)
            dataLineThickness = a.getDimensionPixelSize(R.styleable.DateChartView_dataLineThickness, dataLineThickness)
            labelLineThickness = a.getDimensionPixelSize(R.styleable.DateChartView_labelLineThickness, labelLineThickness)
            middleLineThickness = a.getDimensionPixelSize(R.styleable.DateChartView_middleLineThickness, middleLineThickness)
            middleLineVisible = a.getBoolean(R.styleable.DateChartView_middleLineVisible, middleLineVisible)
        } finally {
            a.recycle()
        }

        init()
    }

    private fun init() {

        dayTextPaint = Paint().apply {
            color = dayTextColor
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = dayTextSize.toFloat()
            isAntiAlias = true
        }

        dayDisabledTextPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = dayTextSize.toFloat()
            isAntiAlias = true
        }

        daySelectedTextPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = dayTextSize.toFloat()
            isAntiAlias = true
        }

        labelTextPaint = Paint().apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.RIGHT
            textSize = labelTextSize.toFloat()
            color = labelTextColor
            isAntiAlias = true
        }

        bodyBackgroundPaint = Paint().apply {
            color = bodyBackgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        footerBackgroundPaint = Paint().apply {
            color = footerBackgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        selectedDayBackgroundPaint = Paint().apply {
            color = selectedDayBackgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        pointBackgroundPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        dataLineStrokePaint = Paint().apply {
            strokeWidth = dataLineThickness.toFloat()
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        labelLineStrokePaint = Paint().apply {
            strokeWidth = labelLineThickness.toFloat()
            style = Paint.Style.STROKE
            color = labelLineColor
            isAntiAlias = true
        }

        middleLineStrokePaint = Paint().apply {
            strokeWidth = middleLineThickness.toFloat()
            style = Paint.Style.STROKE
            color = middleLineColor
            isAntiAlias = true
        }

        extraLineStrokePaint = Paint().apply {
            strokeWidth = labelLineThickness.toFloat()
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        labelLinePath = Path()

        daysOffset = Math.floor(numOfVisibleDays / 2.0).toInt()

        mGestureDetector = GestureDetectorCompat(context, mGestureListener)
        mScroller = OverScroller(context, FastOutLinearInInterpolator())
        mMinimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw Background

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat() - footerHeight, bodyBackgroundPaint)
        canvas.drawRect(0f, height - footerHeight.toFloat(), width.toFloat(), height.toFloat(), footerBackgroundPaint)
        canvas.drawCircle(width / 2f, height - footerHeight / 2f, dayTextSize * 1.2f, selectedDayBackgroundPaint)

        // Draw middle line

        if (middleLineVisible) {
            canvas.drawLine(width / 2f, 0f, width / 2f, (height - footerHeight).toFloat(), middleLineStrokePaint)
        }

        // Draw labels and lines

        labelTextPaint?.color = labelTextColor
        labelTextPaint?.textSize = labelTextSize.toFloat()

        val yFactor = (height - footerHeight - chartPaddingTop - chartPaddingBottom) / (numOfHorizontalLabels - 1)
        val vFactor = (maxValue - minValue) / (numOfHorizontalLabels - 1)
        (0 until numOfHorizontalLabels).forEach {

            val labelY = (it * yFactor).toFloat() + chartPaddingTop
            val labelText = "${(maxValue - it * vFactor).toInt()}"

            if (it != numOfHorizontalLabels - 1 || minValue != 0f) {
                canvas.drawText(labelText, width - (labelTextWidth / 4).toFloat(), labelY - labelTextPaint!!.height(), labelTextPaint)
            }

            if (it > 0 && it < numOfHorizontalLabels - 1) {

                val dashFactor = (width - labelTextWidth) / 100f
                val dashWidth = dashFactor * 0.4f

                (0..100).map { it * dashFactor }.forEach {
                    canvas.drawLine(it, labelY, it + dashWidth, labelY, labelLineStrokePaint)
                }

            }

        }

        // Day width

        mWidthPerDay = (width.toFloat() - numOfVisibleDays - 1) / numOfVisibleDays

        // Scroll offset
        if (mCurrentOrigin.x < 0) {
            mCurrentOrigin.x = 0f
        }

        val leftDays = -Math.ceil((mCurrentOrigin.x / mWidthPerDay!!).toDouble()).toInt()
        val startFromPixel = mCurrentOrigin.x + mWidthPerDay!! * leftDays

        // Main logic

        var startPixel = startFromPixel
        dayTextPaint?.color = dayTextColor
        valueFactor = (height - footerHeight - chartPaddingTop - chartPaddingBottom) / (maxValue - minValue)

        //Draw extra lines

        labelTextPaint?.textSize = labelTextSize * 0.7f
        extraLines.forEach {
            val currentY = (maxValue - it.value) * valueFactor!! + chartPaddingTop
            extraLineStrokePaint?.color = it.color
            canvas.drawLine(0f, currentY, (width - labelTextWidth / 4).toFloat(), currentY, extraLineStrokePaint)
            labelTextPaint?.color = it.color
            canvas.drawText(it.label, (width - labelTextWidth / 4).toFloat(), currentY + labelTextPaint!!.height(), labelTextPaint)
        }

        // Draw points

        (leftDays - daysOffset..(leftDays - daysOffset + numOfVisibleDays + 1)).forEach { dayNumber ->

            val currentDate = today().dateByAddingDays(dayNumber)

            if (dataLines.find { it.year == currentDate.year && it.month == currentDate.month } == null) {
                onMonthChange?.let { it(currentDate.year, currentDate.month) }
            } else {
                dataLines.forEach { dataLine ->
                    pointBackgroundPaint?.color = dataLine.color
                    dataLineStrokePaint?.color = dataLine.color

                    val dataLineDate = today().changeDate(dataLine.year, dataLine.month, 1)

                    val currentPoint = dataLine.points.find { dataLineDate.changeDay(it.dayOfMonth).isSameDayAsDate(currentDate) }
                    currentPoint?.let {

                        val currentX = startPixel + mWidthPerDay!! / 2
                        val currentY = (maxValue - it.value) * valueFactor!! + chartPaddingTop
                        canvas.drawCircle(currentX, currentY, dataPointRadius.toFloat(), pointBackgroundPaint)

                        val prevPoint = dataLine.points.find { dataLineDate.changeDay(it.dayOfMonth).isSameDayAsDate(today().dateByAddingDays(-dayNumber - 1)) }
                        prevPoint?.let {
                            val prevX = startPixel + (mWidthPerDay!! / 2) - mWidthPerDay!!
                            val prevY = (maxValue - it.value) * valueFactor!! + chartPaddingTop
                            canvas.drawLine(prevX, prevY, currentX, currentY, dataLineStrokePaint)
                        }

                        val nextPoint = dataLine.points.find { dataLineDate.changeDay(it.dayOfMonth).isSameDayAsDate(today().dateByAddingDays(+dayNumber + 1)) }
                        nextPoint?.let {
                            val prevX = startPixel + (mWidthPerDay!! / 2) + mWidthPerDay!!
                            val prevY = (maxValue - it.value) * valueFactor!! + chartPaddingTop
                            canvas.drawLine(prevX, prevY, currentX, currentY, dataLineStrokePaint)
                        }

                    }
                }
            }

            val paint = if (currentDate.isLaterThanDate(today())) dayDisabledTextPaint else dayTextPaint
            canvas.drawText(currentDate.dayOfMonth.toString(), startPixel + mWidthPerDay!! / 2, height - (footerHeight / 2) - dayTextPaint!!.height(), paint)
            startPixel += mWidthPerDay!!

        }

        // Draw selected day

        val trianguleWidth = Math.round(dayTextSize * 4f)
        val trianguleHeight = Math.round(dayTextSize * 0.25f)

        canvas.drawTriangle(bodyBackgroundPaint!!, width / 2, height - trianguleHeight, trianguleWidth, trianguleHeight)

        circlePath.addCircle(width / 2f, height - footerHeight / 2f, dayTextSize * 1.2f, Path.Direction.CW)
        canvas.clipPath(circlePath)

        startPixel = startFromPixel
        (leftDays - daysOffset..(leftDays - daysOffset + numOfVisibleDays + 1)).forEach { dayNumber ->
            val currentDay = today().dateByAddingDays(dayNumber)
            canvas.drawText(currentDay.dayOfMonth.toString(), startPixel + mWidthPerDay!! / 2, height - (footerHeight / 2) - dayTextPaint!!.height(), daySelectedTextPaint)
            startPixel += mWidthPerDay!!
        }

    }

    fun addMonthDataLines(dataLines: List<DataLine>) {
        with(this.dataLines) {
            //dataLines.
        }
        this.dataLines.addAll(dataLines)
        invalidate()
        Log.e(":)", this.dataLines.size.toString())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val value = mGestureDetector?.onTouchEvent(event)
        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event?.action == MotionEvent.ACTION_UP && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestDay()
            }
            mCurrentScrollDirection = Direction.NONE
        }
        return value!!
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller!!.isFinished) {
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestDay()
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && mScroller!!.currVelocity <= mMinimumFlingVelocity) {
                goToNearestDay()
            } else if (mScroller!!.computeScrollOffset()) {
                mCurrentOrigin.x = mScroller!!.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    private fun goToNearestDay() {

        var leftDays = (mCurrentOrigin.x / mWidthPerDay!!).toDouble()

        leftDays = when {
            mCurrentFlingDirection != Direction.NONE   -> Math.round(leftDays).toDouble()
            mCurrentScrollDirection == Direction.LEFT  -> Math.floor(leftDays)
            mCurrentScrollDirection == Direction.RIGHT -> Math.ceil(leftDays)
            else                                       -> Math.round(leftDays).toDouble()
        }

        if (leftDays < 0) leftDays = 0.0

        val nearestOrigin = (mCurrentOrigin.x - leftDays * mWidthPerDay!!).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller?.forceFinished(true)
            // Snap to date.
            mScroller?.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / mWidthPerDay!! * mScrollDuration).toInt())
            ViewCompat.postInvalidateOnAnimation(this@DateChartView)
        }

        // Reset scrolling and fling direction.
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        onDaySelected?.invoke(today().dateBySubtractingDays(leftDays.toInt()).time)

    }

    private fun goToSelectedDay(x: Float, y: Float) {

        if (y < height - footerHeight) return

        var leftDays = ((mCurrentOrigin.x - x) / mWidthPerDay!!).toDouble() + daysOffset

        leftDays = Math.ceil(leftDays)

        if (leftDays < 0) leftDays = 0.0

        val nearestOrigin = (mCurrentOrigin.x - leftDays * mWidthPerDay!!).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller?.forceFinished(true)
            // Snap to date.
            mScroller?.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / mWidthPerDay!! * mScrollDuration).toInt())
            ViewCompat.postInvalidateOnAnimation(this@DateChartView)
        }

        // Reset scrolling and fling direction.
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        onDaySelected?.invoke(today().dateBySubtractingDays(leftDays.toInt()).time)

    }

    // Extensions

    private fun Int.sp() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, toFloat(), context.resources.displayMetrics).toInt()

}