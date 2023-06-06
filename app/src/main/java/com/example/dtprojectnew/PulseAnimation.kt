package com.example.dtprojectnew

import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView

class PulseAnimation {

    // This function calculates the duration of the pulse animation based on the pulse value.
    // If pulseValue is greater than 0, the duration is calculated and limited to the range 300-1000 milliseconds.
    // If pulseValue is not greater than 0, the duration is set to 0.
    fun calculateAnimationDuration(pulseValue: Int): Long {
        return if (pulseValue > 0) {
            (1000L * 60 / pulseValue).coerceIn(300L, 1000L)
        } else {
            0L
        }
    }

    // This function creates the pulse animation.
    // The animation scales the view from 100% to 120% of the original size and then reverses back to 100%.
    // This scaling animation is repeated indefinitely.
    fun createPulseAnimation(duration: Long): ScaleAnimation {
        return ScaleAnimation(
            1.0f, 1.2f, 1.0f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            this.duration = duration
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }

    // This function starts the pulse animation on the given view.
    // It sets an animation listener to restart the animation when it ends.
    // The animation is restarted only if the duration of the animation is greater than 0.
    fun startAnimation(view: ImageView, pulseValue: Int) {
        val pulseAnimation = createPulseAnimation(calculateAnimationDuration(pulseValue))
        pulseAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                pulseAnimation.duration = calculateAnimationDuration(pulseValue)
                if (pulseAnimation.duration > 0) {
                    pulseAnimation.reset()
                    view.startAnimation(pulseAnimation)
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        view.startAnimation(pulseAnimation)
    }
}
