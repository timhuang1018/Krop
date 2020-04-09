package com.timhuang.cropimagetest

import android.graphics.Point
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

private val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        crop_image_view.setOnClickListener {
            getData()

        }
    }

    private fun getData() {

//        val intArray = intArrayOf(0,0)
//        crop_image_view.getLocationOnScreen(intArray)
//        val point = Point(intArray[0],intArray[1])
//        Log.d(TAG, "location x:${point.x}, y:${point.y}")
//
//        val rect = Rect()
//
//        crop_image_view.getClipBounds(rect)
//
//
//        Log.d(TAG, "rect left:${rect.left}, right:${rect.right}, bottom :${rect.bottom}, top:${rect.top}")


    }
}
