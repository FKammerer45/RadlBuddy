package com.example.dtprojectnew

import android.widget.ImageView

class TiltController(private val imageView: ImageView) {

    fun setTilt(degrees: Float) {
        imageView.rotation = degrees
    }
}
