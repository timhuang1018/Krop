package com.timhuang.cropimagetest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ClippingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ClippedView(this))
    }
}
