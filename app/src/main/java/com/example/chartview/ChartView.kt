package com.example.chartview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.support.annotation.IntDef
import android.util.AttributeSet
import android.view.View
import java.util.*
import java.util.Collections.max


class ChartView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    //当前View的高
    private var mViewHeight: Int = 0
    //当前View的宽
    private var mViewWidth: Int = 0
    // 绘制坐标系的画笔
    private var mPaintCdt: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //绘制坐标系上刻度点的画笔
    private var mPaintSysPoint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //绘制折线上的点的画笔
    private var mPaintLinePoint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //绘制文字的画笔
    private var mPaintText: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //绘制折线的画笔
    private var mPaintLine: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //绘制虚线的画笔
    private var mPaintDash: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //x,y轴的画笔
    private var mPaintSys: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //绘制覆盖区域
    private var mPaintFillArea: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var mXBound: Rect
    private lateinit var mYBound: Rect
    private lateinit var pointList: ArrayList<Point>
    //x，y坐标轴每多少一个刻度
    private var xScale = 2
    private var yScale = 10
    private var everyXwidth: Float = 0.0f
    private var everyYheight: Float = 0.0f
    //传入点的X的最大坐标
    private var maxX: Int = 0
    //传入点的Y的最大坐标
    var maxY: Int = -1
        set(value) {
            field = value
            invalidate()
        }
    //折线的颜色
    var brokenLineColor = Color.WHITE
        set(value) {
            field = value
            mPaintCdt.color = brokenLineColor
            postInvalidate()
        }
    //折线的宽度
    var brokenLineSize = 2f
        set(value) {
            field = value
            mPaintCdt.strokeWidth = brokenLineSize
            postInvalidate()
        }
    //折线小圆点的颜色
    var brokenLinePointColor = Color.parseColor("#00EE00")
        set(value) {
            field = value
            mPaintLinePoint.color = brokenLinePointColor
            postInvalidate()
        }
    //点的半径
    var brokenLinePointRadius = 6f
        set(value) {
            field = value
            postInvalidate()
        }

    var SystemPointRadius = 6f
        set(value) {
            field = value
            postInvalidate()
        }
    var dashSize = 2f
        set(value) {
            field = value
            mPaintDash.strokeWidth = dashSize
            postInvalidate()
        }
    //折线图距离四周的像素大小
    var margin = 80
        set(value) {
            field = value
            postInvalidate()
        }
    //是否显示虚线
    var isShowDash = false
        set(value) {
            field = value
            postInvalidate()
        }

    //虚线的颜色
    var dashColor: Int = Color.WHITE
        set(value) {
            field = value
            mPaintDash.color = dashColor
            postInvalidate()
        }
    //坐标轴的颜色
    var coordinateSystemColor = Color.WHITE
        set(value) {
            field = value
            mPaintSys.color = value
            postInvalidate()
        }
    //坐标轴的颜色
    var coordinateSystemPointColor = Color.WHITE
        set(value) {
            field = value
            mPaintSys.color = value
            postInvalidate()
        }
    //坐标轴的粗细
    var coordinateSystemSize = 3f
        set(value) {
            field = value
            mPaintSys.strokeWidth = coordinateSystemSize
            postInvalidate()
        }
    //线的类型，默认是折线
    @LineType
    var lineType: Int = CURVELINE
        set(value) {
            field = value
            initAnimator(maxX)
            invalidate()
        }
    //是否填充
    var fillArea = true
        set(value) {
            field = value
            invalidate()
        }

    init {
        initPaint()
    }


    //初始化画笔
    private fun initPaint() {
        //虚线需要关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        mPaintCdt.style = Paint.Style.STROKE
        mPaintCdt.strokeWidth = brokenLineSize
        mPaintCdt.color = brokenLineColor

        mPaintLinePoint.style = Paint.Style.FILL
        mPaintLinePoint.color = brokenLinePointColor

        mPaintSysPoint.color = coordinateSystemPointColor

        mPaintSys.style = Paint.Style.STROKE
        mPaintSys.strokeWidth = coordinateSystemSize
        mPaintSys.color = coordinateSystemColor

        mPaintText.textAlign = Paint.Align.CENTER
        mPaintText.color = Color.WHITE
        mPaintText.textSize = 30f

        mPaintFillArea.color = Color.YELLOW
        mPaintFillArea.style = Paint.Style.FILL

        mPaintLine.style = Paint.Style.STROKE
        mPaintLine.strokeWidth = brokenLineSize
        mPaintLine.color = brokenLineColor

        mPaintDash = Paint()
        mPaintDash.style = Paint.Style.STROKE
        mPaintDash.strokeWidth = dashSize
        mPaintDash.color = dashColor
        //设置绘制虚线，floatArrayOf(10f, 10f)表示10个像素的实线，10个像素的虚线
        mPaintDash.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)

        mXBound = Rect()
        mYBound = Rect()


    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measureSpec(widthMeasureSpec), measureSpec(heightMeasureSpec))
    }

    private fun measureSpec(heightMeasureSpec: Int): Int {
        var result: Int
        val specSize = View.MeasureSpec.getSize(heightMeasureSpec) //获取高的高度 单位 为px
        val specMode = View.MeasureSpec.getMode(heightMeasureSpec)//获取测量的模式
        //如果是精确测量，就将获取View的大小设置给将要返回的测量值
        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = 400
            //如果设置成wrap_content时，给高度指定一个值
            if (specMode == View.MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize)
            }
        }
        return result
    }

    private lateinit var mShader: LinearGradient
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //获取当前View的宽高
        mViewWidth = w
        mViewHeight = h
        //渐变
        mShader = LinearGradient(mViewWidth.toFloat(), mViewHeight.toFloat(), mViewWidth.toFloat(), 0f, intArrayOf(Color.YELLOW, Color.TRANSPARENT), null, Shader.TileMode.REPEAT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        getWidthAndHeight()
        drawCoordinate(canvas)
        drawCalibration(canvas)
        when (lineType) {
            BROKENLINE -> {
                drawBrokenLine(canvas)
            }
            CURVELINE -> {
                drawCurve(canvas)
            }
        }
    }

    private val getWidthAndHeight = {
        //x轴上需要绘制的刻度的个数
        val numX = maxX / xScale
        //每格的宽度
        everyXwidth = (mViewWidth - margin * 2) / (numX * 1f)
        //y轴上需要绘制的刻度的个数
        val numY = maxY / yScale
        //每格的高度
        everyYheight = (mViewHeight - margin * 2) / (numY * 1f)
    }

    /**
     * 绘制曲线
     */
    private val drawCurve = { canvas: Canvas ->
        val fillPath = Path()
        val path = Path()
        val zeroPoint = Point()
        zeroPoint.x = (margin + pointList[0].x / (xScale * 1f) * everyXwidth).toInt()
        zeroPoint.y = (mViewHeight - margin - pointList[0].y / (yScale * 1f) * everyYheight).toInt()
        if (fillArea) {
            //移动到原点
            fillPath.moveTo(margin.toFloat(), (mViewHeight - margin).toFloat())
            fillPath.lineTo(zeroPoint.x.toFloat(), zeroPoint.y.toFloat())
        }
        path.moveTo(zeroPoint.x.toFloat(), zeroPoint.y.toFloat())
        for (i in 0 until mAnimatorValue - 1) {
            val startX = pointList[i].x / (xScale * 1f) * everyXwidth
            val startY = pointList[i].y / (yScale * 1f) * everyYheight
            val startPx = margin + startX
            val startPy = mViewHeight.toFloat() - margin.toFloat() - startY
            val endX = pointList[i + 1].x / (xScale * 1f) * everyXwidth
            val endY = pointList[i + 1].y / (yScale * 1f) * everyYheight
            val endPx = margin + endX
            val endPy = mViewHeight.toFloat() - margin.toFloat() - endY
            val wt = (startPx + endPx) / 2
            val p3 = Point()
            val p4 = Point()
            p3.y = startPy.toInt()
            p3.x = wt.toInt()
            p4.y = endPy.toInt()
            p4.x = wt.toInt()
            path.cubicTo(p3.x.toFloat(), p3.y.toFloat(), p4.x.toFloat(), p4.y.toFloat(), endPx, endPy)
            if (fillArea) {
                fillPath.cubicTo(p3.x.toFloat(), p3.y.toFloat(), p4.x.toFloat(), p4.y.toFloat(), endPx, endPy)
                if (i == mAnimatorValue - 2) {
                    fillPath.lineTo(endPx, mViewHeight.toFloat() - margin.toFloat())
                    fillPath.close()
                    mPaintFillArea.shader = mShader
                    canvas.drawPath(fillPath, mPaintFillArea)
                }
            }
            canvas.drawPath(path, mPaintLine)
            //这里绘制两次是为了把线盖住，效果更好
            if (i != 1) {
                canvas.drawCircle(startPx, startPy, brokenLinePointRadius, mPaintLinePoint)
            }
            canvas.drawCircle(endPx, endPy, brokenLinePointRadius, mPaintLinePoint)
            if (isShowDash) {
                //绘制横向虚线
                canvas.drawLine(margin.toFloat(), mViewHeight.toFloat() - margin.toFloat() - startY - brokenLinePointRadius / 2, margin + startX - brokenLinePointRadius / 2, mViewHeight.toFloat() - margin.toFloat() - startY - brokenLinePointRadius / 2, mPaintDash)
                //绘制竖向虚线
                canvas.drawLine(endPx, endPy, endPx, mViewHeight.toFloat() - margin.toFloat() - brokenLinePointRadius, mPaintDash)
            }
        }
    }

    /**
     * 绘制折线
     * *计算绘制点的坐标位置
     * 绘制点的坐标 =  (传入点的的最大的x，y坐标/坐标轴上的间隔） * 坐标间隔对应的屏幕上的间隔
     */
    private val drawBrokenLine = { canvas: Canvas ->

        val zeroPoint = Point()
        zeroPoint.x = (margin + pointList[0].x / (xScale * 1f) * everyXwidth).toInt()
        zeroPoint.y = (mViewHeight - margin - pointList[0].y / (yScale * 1f) * everyYheight).toInt()
        val fillPath = Path()
        if (fillArea) {
            //移动到原点
            fillPath.moveTo(margin.toFloat(), (mViewHeight - margin).toFloat())
            fillPath.lineTo(zeroPoint.x.toFloat(), zeroPoint.y.toFloat())
        }
        for (i in 1 until mAnimatorValue) {
            //计算出脱离坐标系的点所处的位置
            val pointX = pointList[i].x / (xScale * 1f) * everyXwidth
            val pointY = pointList[i].y / (yScale * 1f) * everyYheight

            //坐标系内的点的位置
            val startX = zeroPoint.x.toFloat()
            val startY = zeroPoint.y.toFloat()
            val endX = margin + pointX
            val endY = mViewHeight.toFloat() - margin.toFloat() - pointY
            canvas.drawLine(startX, startY, endX, endY, mPaintLine)
            //这里绘制两次是为了把线盖住，效果更好
            canvas.drawCircle(startX, startY, brokenLinePointRadius, mPaintLinePoint)
            canvas.drawCircle(endX, endY, brokenLinePointRadius, mPaintLinePoint)
            //记录上一个坐标点的位置
            zeroPoint.x = endX.toInt()
            zeroPoint.y = endY.toInt()

            if (isShowDash) {
                //绘制横向虚线
                canvas.drawLine(margin.toFloat(), mViewHeight.toFloat() - margin.toFloat() - pointY - brokenLinePointRadius / 2, margin + pointX - brokenLinePointRadius / 2, mViewHeight.toFloat() - margin.toFloat() - pointY - brokenLinePointRadius / 2, mPaintDash)
                //绘制竖向虚线
                canvas.drawLine(zeroPoint.x.toFloat(), zeroPoint.y.toFloat(), zeroPoint.x.toFloat(), mViewHeight.toFloat() - margin.toFloat() - brokenLinePointRadius, mPaintDash)
            }

            if (fillArea) {
                fillPath.lineTo(startX, startY)
                if (i == mAnimatorValue - 1) {
                    fillPath.lineTo(endX, endY)
                    fillPath.lineTo(endX, mViewHeight.toFloat() - margin.toFloat())
                    fillPath.close()
                    mPaintFillArea.shader = mShader
                    canvas.drawPath(fillPath, mPaintFillArea)
                }
            }
        }
    }

    /**
     *  绘制 X,y轴刻度标记
     *  desc:将控件的宽度减去margin值，然后根据坐标的个数进行绘制
     */
    private val drawCalibration = { canvas: Canvas ->
        //x轴上的点
        for (i in pointList.indices) {
            canvas.drawCircle(margin + i * everyXwidth, (mViewHeight - margin).toFloat(), SystemPointRadius, mPaintSysPoint)
            val indexX = (i * xScale).toString()
            if (indexX.toInt() <= maxX) {
                //将TextView 的文本放入一个矩形中， 测量TextView的高度和宽度
                mPaintText.getTextBounds(indexX, 0, indexX.length, mXBound)
                canvas.drawText(indexX, margin + i * everyXwidth, (mViewHeight - margin + 2 * mXBound.height()).toFloat(), mPaintText)
            }
        }

        //绘制y轴文字
        for (i in 0..maxY) {
            canvas.drawCircle(margin.toFloat(), mViewHeight.toFloat() - margin.toFloat() - i * everyYheight, SystemPointRadius, mPaintSysPoint)
            val indexY = (i * yScale).toString()
            //这里直接测量"0",不然会因为0和1或者其他的长度不同导致位置有点问题
            mPaintText.getTextBounds("0", 0, "0".length, mYBound)
            if (i != 0) {
                canvas.drawText(indexY, margin.toFloat() - 2 * mYBound.width(), mViewHeight.toFloat() - margin.toFloat() - i * everyYheight + mYBound.height() / 2, mPaintText)
            }
        }
    }

    //绘制X轴Y轴 以及原点
    private val drawCoordinate = { canvas: Canvas ->
        canvas.drawLine(margin.toFloat(), (mViewHeight - margin).toFloat(), margin.toFloat(), 5f, mPaintSys)
        canvas.drawLine(margin.toFloat(), (mViewHeight - margin).toFloat(), (mViewWidth - 5).toFloat(), (mViewHeight - margin).toFloat(), mPaintSys)
        canvas.drawCircle(margin.toFloat(), (mViewHeight - margin).toFloat(), SystemPointRadius, mPaintSysPoint)
    }

    fun setChartPoints(points: ArrayList<Int>) {
        pointList = ArrayList()
        points.forEachIndexed { index, value ->
            val point = Point()
            point.x = index
            point.y = value
            pointList.add(point)
        }
        val yPointArray = arrayListOf<Int>()

        for (i in pointList.indices) {
            yPointArray.add( pointList[i].y)
        }
        if (maxY == -1) {
            maxY = max(yPointArray)
        }
        maxX = points.size
        //默认x有10格，y5格，这里你可以修改
        xScale = maxX / 10
        yScale = maxY / 5
        initAnimator(maxX)
        invalidate()
    }

    private lateinit var valueAnimator: ValueAnimator
    private var mAnimatorValue: Int = 0
    private lateinit var mUpdateListener: ValueAnimator.AnimatorUpdateListener
    private val defaultDuration = 2000

    private fun initAnimator(maxX: Int) {
        valueAnimator = ValueAnimator.ofInt(0, maxX).setDuration(defaultDuration.toLong())
        mUpdateListener = ValueAnimator.AnimatorUpdateListener { animation ->
            mAnimatorValue = animation.animatedValue as Int
            invalidate()
        }
        valueAnimator.addUpdateListener(mUpdateListener)
        valueAnimator.start()
    }
}

@IntDef(value = [CURVELINE, BROKENLINE], flag = false)
@Retention(AnnotationRetention.SOURCE)
annotation class LineType

//曲线
const val CURVELINE = 1
//折线
const val BROKENLINE = 2