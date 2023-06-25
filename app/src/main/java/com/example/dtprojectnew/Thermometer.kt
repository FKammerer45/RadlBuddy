package com.example.dtprojectnew

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.lang.IndexOutOfBoundsException

class ThermometerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    // Thermometer-Werte und Farben
    private val temperatureValues = listOf(-20,-10,0, 15, 25, 30, 35)
        private val tempClrs = listOf(Color.rgb(255,255,255), Color.rgb(165, 0, 255),Color.rgb(0, 0, 255), Color.rgb(0, 255, 0), Color.rgb(255, 255, 0), Color.rgb(255,165,0),Color.rgb(255, 0, 0)

    )

    private var currentTemperature: Int = 0

    // Farben für das Thermometer
    private val outlineColor = Color.BLACK

    // Pinsel für die Grafik
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = outlineColor
        strokeWidth = 5f
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width / 4f

        // Thermometer zeichnen
        canvas.drawRect(centerX - 20f, centerY - radius, centerX + 20f, centerY + radius, outlinePaint)

        // Temperaturwert abrufen und Farbe entsprechend setzen
        val temperatureIndex = temperatureValues.indexOfFirst { it >= currentTemperature }
        var tempClr: Int
        tempClr = try {
            tempClrs[temperatureIndex]
        } catch (e: IndexOutOfBoundsException) {
            if (currentTemperature > 0)
                Color.rgb(255, 0, 0)
            else
                Color.rgb(255,255,255)
        }

        // Temperaturbereich zeichnen
        val rangeTop = centerY + radius - radius * ((currentTemperature + 100) / 100f)
        canvas.drawRect(centerX - 15f, rangeTop, centerX + 15f, centerY + radius, Paint().apply {
            style = Paint.Style.FILL
            color = tempClr
        })
    }

    fun setTemperature(temperature: Float) {
        currentTemperature = temperature.toInt()
        invalidate() // Neuzeichnen der View
    }
}