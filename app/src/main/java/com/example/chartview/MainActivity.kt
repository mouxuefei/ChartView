package com.example.chartview

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switch_dash.setOnCheckedChangeListener { _, isChecked ->
            chartView.isShowDash = isChecked
        }

        switch_type.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chartView.lineType = BROKENLINE
            } else {
                chartView.lineType = CURVELINE
            }
        }

        switch_cover.setOnCheckedChangeListener { _, isChecked ->
            chartView.fillArea = isChecked
        }

        val random = Random()
        val pointList = ArrayList<Int>()
        for (i in 0..30) {
            val nextInt = random.nextInt(30)
            pointList.add(nextInt)
        }

        chartView.dashColor = Color.GREEN
        chartView.maxY=35
        chartView.setChartPoints(pointList)
    }
}
