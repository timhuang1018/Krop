package com.timhuang.cropimagetest

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import kotlin.math.sqrt


class CropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {


    private val TAG = "CropImageView"
    private var mode = NONE
    private var imageViewRatio = 0f
    private var scaleFactor = 1f
    private var viewW = 0
    private var viewH = 0
    private var viewX = 0
    private var viewY = 0
    private var imageW = 0
    private var imageH = 0
    private var imageX : Float = 0f
    private var imageY : Float = 0f
    private var touchX = -1f
    private var touchY = -1f
    private var leftLimit = 0
    private var rightLimit = 0
    private var topLimit = 0
    private var bottomLimit = 0
    var originDistance = 0f
    private var midY: Float = 0f
    private var midX: Float = 0f
    val matrixArray = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f)

//    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
//        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            if (scaleFactor >= 1.15f || scaleFactor <= 0.85f) return false
//            scaleFactor *= detector.scaleFactor
//            if (detector.scaleFactor!=1f){
//                scaleFactor = Math.max(0.85f,Math.min(1.15f,scaleFactor))
//            }
//            Log.d(TAG,"scale factor multiply:$scaleFactor, ${detector.scaleFactor}")
//            scale(detector.focusX,detector.focusY)
//            return true
//        }
//    }
//    private val scaleDetector = ScaleGestureDetector(this.context,scaleListener)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged, w:${w}, h:${h} , oldw:$oldw oldh:$oldh")
        init(w,h)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event==null) return false

//        if () return true
//        scaleDetector.onTouchEvent(event)

        when(event.action and event.actionMasked){
            MotionEvent.ACTION_DOWN->{
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE->{
                motion(event)
            }

            MotionEvent.ACTION_UP->{
                mode = NONE
                touchX = -1f
                touchY = -1f
                ifOutOfBounds()
            }

            MotionEvent.ACTION_POINTER_DOWN->{
//                Log.d(TAG,"ACTION_POINTER_DOWN down.... ${event.pointerCount}")
                if (event.pointerCount==2){
                    val dx= event.getX(1)-event.getX(0)
                    val dy = event.getY(1)-event.getY(0)
                    val offsetY = event.rawY - event.getY(0)
                    val locationX = event.rawX + dx
                    val locationY = event.rawY + dy
//                    Log.e(TAG,"dx :$dx, dy:$dy, offsetY = $offsetY, locationX:$locationX, locationY:$locationY,event rawY:${event.rawY}")

                    if (viewX <=locationX && locationX <= (viewX+viewW) && viewY<= locationY && locationY<=(viewY+viewH)){
                        mode = SCALE
                        originDistance = spacing(event)

                        midX = (event.getX(0)+event.getX(1))/2 + viewX
                        midY = (event.getY(0)+event.getY(1))/2 + viewY
                    }else{
                        mode = NONE
                    }
                }
            }
        }
        return true
    }

    private fun motion(event: MotionEvent) {
        when(mode){
            DRAG->{
                if (touchX>0 && touchY>0){

                    val moveX = event.rawX - touchX
                    val moveY = event.rawY - touchY
                    //move pic to right
                    Log.d(TAG,"event rawY:${event.rawY}, eventY ${event.getY(0)} imageX:$imageX,w $imageW, right limit :$rightLimit")

                    //prevent moving
                    val isValidX = (moveX<0 && imageX+imageW>rightLimit) || (moveX>0 && imageX < leftLimit)
                    val isValidY = (moveY<0 && imageY+imageH>bottomLimit) || (moveY>0 && imageY < topLimit)
                    when{
                        isValidX && isValidY-> moveImage(moveX,moveY)
                        isValidX -> moveImage(moveX = moveX)
                        isValidY -> moveImage(moveY =moveY)
                        else -> return
                    }

                }
                touchX = event.rawX
                touchY = event.rawY
            }

            SCALE->{

//                Log.d(TAG,"pointerCount :${event.pointerCount}")
                if (event.pointerCount==2){
                    val afterDistance = spacing(event)
                    var ratio = afterDistance / originDistance
                    val l = (midX - (midX - imageX) * ratio - imageX).toInt()
                    val t = (midY - (midY - imageY) * ratio - imageY).toInt()
                    val w: Double
                    val h: Double

                    scale(ratio, midX,midY)
//                    when {
//
//                        (originW * ratio < imgMinWidth) or (originH * ratio < imgMinHeight) -> {
//                            w = imgMinWidth.toDouble()
//                            h = imgMinHeight.toDouble()
//                        }
//                        (originW * ratio > imgMaxWidth) or (originH * ratio > imgMaxHeight) -> {
//                            w = imgMaxWidth.toDouble()
//                            h = imgMaxHeight.toDouble()
//                        }
//                        else -> {
//                            w = originW * ratio
//                            h = originH * ratio
//                        }
//                    }
//                    ima
//                    view.layout(l, t, (l + w).toInt(), (t + h).toInt())
                }

            }

            NONE->{
                //maybe some reset
            }

        }
    }

    private fun spacing(event: MotionEvent):Float{
        val x = event.getX(0)-event.getX(1)
        val y = event.getY(0)-event.getY(1)
        return sqrt(x*x+y*y).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent){
        val x = event.getX(0)+event.getX(1)
        val y = event.getY(0)+event.getY(1)
        point.set(x/2,y/2)
    }


    private fun init(w: Int, h: Int) {
        viewW = w
        viewH = h

        val intArray = intArrayOf(0,0)
        getLocationOnScreen(intArray)
        val point = Point(intArray[0],intArray[1])
        Log.d(TAG, "view location x:${point.x}, y:${point.y}")
        viewX = point.x
        viewY = point.y
        imageX = point.x.toFloat()
        imageY = point.y.toFloat()
        leftLimit = point.x
        topLimit = point.y
        rightLimit = point.x + viewW
        bottomLimit = point.y + viewH
//

        Log.d(TAG,"image W :${drawable.intrinsicWidth}, H :${drawable.intrinsicHeight}")

        Log.d(TAG, "scaleType:${scaleType}, scaleX:${scaleX}, scaleY:${scaleY}")
        Log.d(TAG,"view matrix :$matrix, imageMatrix :$imageMatrix")

        if (drawable.intrinsicWidth < viewW || drawable.intrinsicHeight < viewH){

            val wRatio = viewW / drawable.intrinsicWidth.toFloat()
            val hRatio = viewH / drawable.intrinsicHeight.toFloat()
            Log.d(TAG,"wRatio :$wRatio, hRatio :$hRatio")
            if (wRatio>hRatio){
                imageMatrix = Matrix().apply {
                    postScale(wRatio,wRatio)
                }
                imageViewRatio = wRatio
//                imageMatrix.postScale(wRatio,wRatio)
            }else{
                imageMatrix = Matrix().apply {
                    postScale(hRatio,hRatio)
                }
                imageViewRatio = hRatio
            }
        }
        Log.d(TAG,"view matrix :$matrix, imageMatrix :$imageMatrix")
//        imageMatrix.getValues(matrixArray)
        imageW = (drawable.intrinsicWidth * imageViewRatio).toInt()
        imageH = (drawable.intrinsicHeight * imageViewRatio).toInt()
    }

    fun scale(ratio :Float,focusX: Float, focusY: Float) {
        Log.d(TAG,"ratio:$ratio, focusX :$focusX, focusY:$focusY")
        imageMatrix = Matrix(imageMatrix).apply {
            postScale(ratio,ratio,focusX,focusY)

//            postTranslate(focusX,focusY)
        }
        Log.d(TAG," imageMatrix :$imageMatrix")

    }


    private fun moveImage(moveX :Float =0f,moveY:Float=0f){
        imageMatrix = Matrix(imageMatrix).apply {
            postTranslate(moveX,moveY)
        }
//        imageMatrix = Matrix(imageMatrix).apply {
//            postTranslate(0f,moveY)
//        }
        imageX += moveX
        imageY += moveY
    }


    private fun ifOutOfBounds() {
        val outRight = rightLimit - (imageX+imageW)
        val outLeft = imageX - leftLimit
        val outTop = imageY - topLimit
        val outBottom = bottomLimit - (imageY + imageH)
        if (outRight>0) moveImage(moveX = outRight)
        if (outLeft>0) moveImage(moveX = -outLeft)
        if (outTop>0) moveImage(moveY = -outTop)
        if (outBottom>0) moveImage(moveY = outBottom)

    }



    override fun onDrawForeground(canvas: Canvas?) {
        Log.d(TAG,"onDrawForeground called")
        super.onDrawForeground(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(TAG, "onMeasure, widthMeasureSpec:${widthMeasureSpec}, heightMeasureSpec:${heightMeasureSpec} ")

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }


    companion object{
        const val NONE = 0
        const val DRAG = 1
        const val SCALE = 2
    }

}