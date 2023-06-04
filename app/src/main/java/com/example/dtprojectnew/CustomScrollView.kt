package com.example.dtprojectnew
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class CustomScrollView(context: Context, attrs: AttributeSet) : ScrollView(context, attrs) {

    // Variable that decides if we should consume the touch event or not.
    private var enableScrolling = true

    fun setScrollingEnabled(isEnable: Boolean) {
        this.enableScrolling = isEnable
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return enableScrolling && super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return enableScrolling && super.onTouchEvent(ev)
    }
}
