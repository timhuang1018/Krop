package com.timhuang.cropper

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
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
    private val shadowColor = ResourcesCompat.getColor(resources, R.color.black_opacity_50,null)
    private lateinit var extraCanvas :Canvas
    private lateinit var extraBitmap : Bitmap

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

    //display and api setting
    private var shape = 0

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CropImageView,
            0,0
        ).apply {
            try {
                shape = getInteger(R.styleable.CropImageView_cropShape, RECTANGLE)
            }finally {
                recycle()
            }
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged, w:${w}, h:${h} , oldw:$oldw oldh:$oldh")
        init(w,h)
        drawableSetting()
    }


    override fun onDraw(canvas: Canvas?) {
        try {
            super.onDraw(canvas)
            if (::extraBitmap.isInitialized){
                canvas?.drawBitmap(extraBitmap,0f,0f,null)
            }
        }catch (e:Exception){
            Log.e(TAG,e.toString())
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event==null) return false

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

                    if (viewX <=locationX && locationX <= (viewX+viewW) && viewY<= locationY && locationY<=(viewY+viewH)){
                        mode = SCALE
                        originDistance = spacing(event)

                        scaleRateX = (locationX - imageX) / imageW
                        scaleRateY = (locationY - imageY) / imageH
                    }else{
                        mode = NONE
                    }
                }
            }
        }
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
//        Log.d(TAG,"dispatchTouchEvent called")

        parent.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(event)
    }


    override fun setImageBitmap(bm: Bitmap?) {
        Log.d(TAG,"setImageBitmap called")
        super.setImageBitmap(bm)
        drawableSetting()
    }

    override fun setImageResource(resId: Int) {
        Log.d(TAG,"setImageResource called")
        super.setImageResource(resId)
        drawableSetting()
    }

    override fun setImageURI(uri: Uri?) {
        Log.d(TAG,"setImageURI called")
        super.setImageURI(uri)
        drawableSetting()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        Log.d(TAG,"setImageDrawable called")
        super.setImageDrawable(drawable)
        drawableSetting()
    }

    private fun motion(event: MotionEvent) {
        when(mode){
            DRAG->{
                if (touchX>0 && touchY>0){

                    val moveX = event.rawX - touchX
                    val moveY = event.rawY - touchY
                    //move pic to right

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
                if (event.pointerCount==2){
                    val afterDistance = spacing(event)
                    var ratio = afterDistance / originDistance
                    val centerX = imageX + imageW * scaleRateX
                    val centerY = imageY + imageH * scaleRateY
                    if (ratio>1 && imageW >=imageMaxW) return
                    if (ratio<1 && imageW <=imageMinW) return
                    scale(ratio,centerX ,centerY)

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

    private fun drawableSetting(){
        if(drawable==null) return
        Log.d(TAG,"image W :${drawable.intrinsicWidth}, H :${drawable.intrinsicHeight}")
        Log.d(TAG,"view matrix :$matrix, imageMatrix :$imageMatrix")

        val wRatio = viewW / drawable.intrinsicWidth.toFloat()
        val hRatio = viewH / drawable.intrinsicHeight.toFloat()
        if (wRatio>hRatio){
            imageMatrix = Matrix().apply {
                postScale(wRatio,wRatio)
            }
            imageViewRatio = wRatio
        }else{
            imageMatrix = Matrix().apply {
                postScale(hRatio,hRatio)
            }
            imageViewRatio = hRatio
        }
        imageX = viewX.toFloat()
        imageY = viewY.toFloat()
        imageW = (drawable.intrinsicWidth * imageViewRatio).toInt()
        imageH = (drawable.intrinsicHeight * imageViewRatio).toInt()
        imageMinW = imageW
        imageMinH = imageH
        imageMaxW = (imageW * 1.3).toInt()
        imageMaxH = (imageH * 1.3).toInt()
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
        leftLimit = point.x
        topLimit = point.y
        rightLimit = point.x + viewW
        bottomLimit = point.y + viewH
        extraBitmap = Bitmap.createBitmap(viewW,viewH,Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        drawCropArea()
    }

    private fun drawCropArea() = run {
        if (!::extraCanvas.isInitialized) return@run
        Log.d(TAG,"drawCropArea called")
        if (shape== OVAL){
            val rect = Rect(0,0,viewW,viewH)

            val paint = Paint().apply {
                isAntiAlias = true
                color = shadowColor
                alpha = 90
                style = Paint.Style.FILL
            }
            extraCanvas.drawPaint(paint)

            val paint2 = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
                color = Color.TRANSPARENT

            }
            val centerX = viewW/2
            val centerY = viewH /2
            val radius = centerX

            extraCanvas.drawCircle(centerX.toFloat(),centerY.toFloat(),radius.toFloat(),paint2)
        }

    }

    private fun scale(ratio :Float, focusX: Float, focusY: Float) {
        Log.d(TAG,"ratio:$ratio, focusX :$focusX, focusY:$focusY")
        imageMatrix.getValues(matrixArray)
        val oldX = matrixArray[2]
        val oldY = matrixArray[5]

        imageMatrix = Matrix(imageMatrix).apply {
            postScale(ratio,ratio,focusX,focusY)
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
        imageX += moveX
        imageY += moveY
    }


    private fun cropImageRectangle(): Bitmap {
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
//        cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream("/sdcard/Download/tmp.jpg"))
//        bitmap.recycle()
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

    private fun cropImageOval(): Bitmap {
        val srcBitmap = cropImageRectangle()
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

        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(srcBitmap, null, rectF, paint)
        srcBitmap.recycle()

        return dstBitmap
    }

    fun setShapeOval(boolean: Boolean){
        if (boolean){
            shape = OVAL
            drawCropArea()
        }
    }

    fun cropImage():Bitmap{
        return when(shape){
            RECTANGLE-> cropImageRectangle()
            OVAL-> cropImageOval()
            else -> cropImageRectangle()
        }
    }

    companion object{
        const val NONE = 0
        const val DRAG = 1
        const val SCALE = 2
        const val RECTANGLE = 0
        const val OVAL = 1
    }
}