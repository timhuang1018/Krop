package com.timhuang.cropimagetest

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.FileOutputStream
import kotlin.math.sqrt


class CropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {


    private val TAG = "CropImageView"
    private var mode = NONE
    private var imageViewRatio = 1f
    //view setting
    private var viewW = 0
    private var viewH = 0
    private var viewX = 0
    private var viewY = 0
    private var leftLimit = 0
    private var rightLimit = 0
    private var topLimit = 0
    private var bottomLimit = 0


    //indicating crop area setting
    private val shallowColor = ResourcesCompat.getColor(resources,R.color.black,null)
    private lateinit var extraCanvas :Canvas
    private lateinit var extraBitmap :Bitmap



    //image setting
    private var imageW = 0
    private var imageH = 0
    private var imageMaxW = 0
    private var imageMaxH = 0
    private var imageMinW = 0
    private var imageMinH = 0
    private var imageX = 0f
    private var imageY = 0f
    private var scaleRateX = 0f
    private var scaleRateY = 0f


    //for calculating
    var originDistance = 0f
    private var touchX = -1f
    private var touchY = -1f
    val matrixArray = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f)


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged, w:${w}, h:${h} , oldw:$oldw oldh:$oldh")
        init(w,h)
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(extraBitmap,0f,0f,null)
//        drawCropArea(canvas)
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
//                    val locationX = (event.getX(0)+event.getX(1))/2 + viewX
//                    val locationY = (event.getY(0)+event.getY(1))/2 + viewY
//                    Log.e(TAG,"dx :$dx, dy:$dy, offsetY = $offsetY, locationX:$locationX, locationY:$locationY,event rawY:${event.rawY}")

                    if (viewX <=locationX && locationX <= (viewX+viewW) && viewY<= locationY && locationY<=(viewY+viewH)){
                        mode = SCALE
                        originDistance = spacing(event)

                        scaleRateX = (locationX - imageX) / imageW
                        scaleRateY = (locationY - imageY) / imageH
//                        midX = (event.getX(0)+event.getX(1))/2 + viewX
//                        midY = (event.getY(0)+event.getY(1))/2 + viewY
//                        Log.d(TAG,"midX:$midX,midY:$midY")
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
//                    Log.d(TAG,"event rawY:${event.rawY}, eventY ${event.getY(0)} imageX:$imageX,w $imageW, right limit :$rightLimit")

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
//                    val l = (midX - (midX - imageX) * ratio - imageX).toInt()
//                    val t = (midY - (midY - imageY) * ratio - imageY).toInt()
//                    val w: Double
//                    val h: Double
                    val centerX = imageX + imageW * scaleRateX
                    val centerY = imageY + imageH * scaleRateY
                    if (ratio>1 && imageW >=imageMaxW) return
                    if (ratio<1 && imageW <=imageMinW) return
                    scale(ratio,centerX ,centerY)
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

//        Log.d(TAG,"image W :${drawable.intrinsicWidth}, H :${drawable.intrinsicHeight}")
//
//        Log.d(TAG, "scaleType:${scaleType}, scaleX:${scaleX}, scaleY:${scaleY}")
//        Log.d(TAG,"view matrix :$matrix, imageMatrix :$imageMatrix")

//        if (drawable.intrinsicWidth < viewW || drawable.intrinsicHeight < viewH){

            val wRatio = viewW / drawable.intrinsicWidth.toFloat()
            val hRatio = viewH / drawable.intrinsicHeight.toFloat()
//            Log.d(TAG,"wRatio :$wRatio, hRatio :$hRatio")
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
//        }
//        Log.d(TAG,"view matrix :$matrix, imageMatrix :$imageMatrix")
//        imageMatrix.getValues(matrixArray)
        imageW = (drawable.intrinsicWidth * imageViewRatio).toInt()
        imageH = (drawable.intrinsicHeight * imageViewRatio).toInt()
        imageMinW = imageW
        imageMinH = imageH
        imageMaxW = (imageW * 1.2).toInt()
        imageMaxH = (imageH * 1.2).toInt()
//        if (imageViewRatio<=1f){

//        }else{

//        }

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(viewW,viewH,Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        drawCropArea(extraCanvas)
    }

    private fun drawCropArea(canvas: Canvas?) = canvas?.also { canvas ->
        val rect = Rect(0,0,viewW,viewH)

        val paint = Paint().apply {
            isAntiAlias = true
            color = shallowColor
            alpha = 90
            style = Paint.Style.FILL
        }
        canvas.drawPaint(paint)

        val paint2 = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
            color = Color.TRANSPARENT

        }
        val centerX = viewW/2
        val centerY = viewH /2
        val radius = centerX

        canvas.drawCircle(centerX.toFloat(),centerY.toFloat(),radius.toFloat(),paint2)
    }

    private fun scale(ratio :Float, focusX: Float, focusY: Float) {
        Log.d(TAG,"ratio:$ratio, focusX :$focusX, focusY:$focusY")
        imageMatrix.getValues(matrixArray)
        val oldX = matrixArray[2]
        val oldY = matrixArray[5]

        imageMatrix = Matrix(imageMatrix).apply {
            postScale(ratio,ratio,focusX,focusY)

//            postTranslate(focusX,focusY)
        }
        imageMatrix.getValues(matrixArray)
        imageW = (imageW * ratio).toInt()
        imageH = (imageH * ratio).toInt()
        imageX = imageX + matrixArray[2] - oldX
        imageY = imageY + matrixArray[5] - oldY
        Log.d(TAG,"imageX:$imageX,imageY:$imageY,imageW:$imageW,imageH:$imageH imageMatrix :$imageMatrix")
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


    fun cropImageRectangle(): Bitmap? {
        val bitmap = drawable.toBitmap(imageW,imageH)
        Log.d(TAG,"bitmap :${bitmap.byteCount},${bitmap.width},${bitmap.height}")

        var x = (viewX - imageX).toInt()
        var y = (viewY - imageY).toInt()
        Log.d(TAG,"x :${x},y :$y, $viewW ,$viewH")
        if (x<0) x=0
        if (y<0) y=0
        //viewW,viewH should not bigger than bitmap w,h
        var cropW = viewW
        var cropH = viewH
        if (x+cropW>imageW){
            cropW = imageW - x
        }
        if (y+cropH>imageH){
            cropH = imageH - y
        }
        val cropBitmap = Bitmap.createBitmap(bitmap,x,y,cropW,cropH)
        Log.d(TAG,"cropBitmap :${cropBitmap.byteCount},${cropBitmap.width},${cropBitmap.height}")
        cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream("/sdcard/Download/tmp.jpg"))
        bitmap.recycle()
        return cropBitmap
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



//    override fun onDrawForeground(canvas: Canvas?) {
//        Log.d(TAG,"onDrawForeground called")
//        super.onDrawForeground(canvas)
//    }
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        Log.d(TAG, "onMeasure, widthMeasureSpec:${widthMeasureSpec}, heightMeasureSpec:${heightMeasureSpec} ")
//
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//    }

//    open fun cropImageOval():Bitmap?{
//        //get original bitmap
//        val bitmap = drawable.toBitmap(imageW,imageH)
//        Log.d(TAG,"bitmap :${bitmap.byteCount},${bitmap.width},${bitmap.height}")
//
//        val canvas = Canvas(bitmap)
//        val paint = Paint()
//        val rect = Rect(0,0,imageW,imageH)
//        paint.isAntiAlias = true
//        canvas.drawARGB(0,0,0,0)
//        canvas.drawCircle((imageW/2).toFloat(), (imageH/2).toFloat(),(imageW/2).toFloat(),paint)
//        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//        canvas.drawBitmap(bitmap,rect,rect,paint)
//        Log.d(TAG,"bitmap :${bitmap.byteCount},${bitmap.width},${bitmap.height}")
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream("/sdcard/Download/tmp.jpg"))
//        return bitmap
//
//    }

    fun cropImageOval(): Bitmap? {
        val srcBitmap = cropImageRectangle() ?: return null
        // Calculate the circular bitmap width with border
        val squareBitmapWidth = Math.min(srcBitmap.width, srcBitmap.height)
        // Initialize a new instance of Bitmap
        val dstBitmap = Bitmap.createBitmap(
            squareBitmapWidth,  // Width
            squareBitmapWidth,  // Height
            Bitmap.Config.ARGB_8888 // Config
        )
        Log.d(TAG,"dstBitmap :${dstBitmap.byteCount},${dstBitmap.width},${dstBitmap.height}")
        val canvas = Canvas(dstBitmap)
        // Initialize a new Paint instance
        val paint = Paint()
        paint.isAntiAlias = true
        val rect = Rect(0, 0, squareBitmapWidth, squareBitmapWidth)
        val rectF = RectF(rect)
//        val centerX = squareBitmapWidth/2
//        val centerY = squareBitmapWidth /2
//        val radius = centerX

//        canvas.drawCircle(centerX.toFloat(),centerY.toFloat(),radius.toFloat(),paint)
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // Calculate the left and top of copied bitmap
        val left = (squareBitmapWidth - srcBitmap.width) / 2.toFloat()
        val top = (squareBitmapWidth - srcBitmap.height) / 2.toFloat()
        canvas.drawBitmap(srcBitmap, null, rectF, paint)
//        canvas.drawBitmap(srcBitmap, left, top, paint)
        // Free the native object associated with this bitmap.
        srcBitmap.recycle()
        // Return the circular bitmap
        dstBitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream("/sdcard/Download/tmp.jpg"))
        return dstBitmap
    }

    companion object{
        const val NONE = 0
        const val DRAG = 1
        const val SCALE = 2
    }

}